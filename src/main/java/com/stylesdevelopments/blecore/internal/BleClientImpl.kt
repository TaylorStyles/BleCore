/*
 * Copyright (c) 2025 StylesDevelopments. BleCore.
 *
 * Created by Taylor Styles on 23/10/2025, 18:00.
 */

package com.stylesdevelopments.blecore.internal

import android.Manifest
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import com.stylesdevelopments.blecore.api.BleClient
import com.stylesdevelopments.blecore.api.BleConfig
import com.stylesdevelopments.blecore.api.model.ConnectionStatus
import com.stylesdevelopments.blecore.internal.decoding.BatteryLevelDecoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import no.nordicsemi.android.kotlin.ble.client.main.callback.ClientBleGatt
import no.nordicsemi.android.kotlin.ble.core.data.GattConnectionState
import no.nordicsemi.android.kotlin.ble.core.scanner.BleScanFilter
import no.nordicsemi.android.kotlin.ble.core.scanner.BleScanResult
import no.nordicsemi.android.kotlin.ble.core.scanner.FilteredServiceUuid
import no.nordicsemi.android.kotlin.ble.scanner.BleScanner
import java.util.UUID

/**
 * Default implementation of [BleClient] providing scanning, connection handling,
 * message routing and RSSI polling.
 *
 * This version is hardware-agnostic (no manufacturer specific logic).
 */
internal class BleClientImpl(
    private val appContext: Context,
    private val config: BleConfig,
    private val scope: CoroutineScope,
) : BleClient {

    private val scanner = BleScanner(appContext)
    private var scanJob: Job? = null

    private var gatt: ClientBleGatt? = null
    private var genericServiceJobs = mutableListOf<Job>()
    private var connectionStateJob: Job? = null

    // region ---- Flows ----

    private val _connection = MutableSharedFlow<ConnectionStatus>(replay = 1)
    override val connectionStatus: Flow<ConnectionStatus> = _connection.asSharedFlow()

    private val _rssiFlow = MutableSharedFlow<Int>(replay = 1)
    override val rssiFlow = _rssiFlow.asSharedFlow()

    private val _batteryFlow = MutableSharedFlow<Int>(replay = 1)
    override val batteryFlow = _batteryFlow.asSharedFlow()

    private val _firmwareRevisionFlow = MutableSharedFlow<String>(replay = 1)
    override val firmwareRevisionFlow = _firmwareRevisionFlow.asSharedFlow()

    private val _deviceManufacturerFlow = MutableSharedFlow<String>(replay = 1)
    override val deviceManufacturerFlow = _deviceManufacturerFlow.asSharedFlow()

    // endregion

    // region ---- Scanning ----

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    override fun scan(): Flow<List<BleScanResult>> = channelFlow {
        val filters = config.scanFilters.map { uuid ->
            BleScanFilter(serviceUuid = FilteredServiceUuid(ParcelUuid(uuid), null))
        }

        val seenDevices = LinkedHashMap<String, BleScanResult>()

        val job = scanner.scan(filters)
            .filter { it.data?.isConnectable == true }
            .onEach { result ->
                val appResult = BleScanResult(
                    device = result.device,
                    data = result.data,
                )

                val isNewDevice = !seenDevices.containsKey(appResult.device.address)
                seenDevices[appResult.device.address] = appResult

                if (config.loggingEnabled) {
                    if (isNewDevice) {
                        Log.d(
                            BLE_CORE_LOGGING_TAG,
                            "Discovered device: ${appResult.device.name ?: "Unknown"} " +
                                    "(${appResult.device.address}) RSSI: ${appResult.data?.rssi ?: "N/A"}"
                        )
                    }
                }

                trySend(seenDevices.values.toList())
            }.launchIn(scope)

        scanJob = job
        awaitClose { job.cancel() }

    }
        .distinctUntilChanged()
        .onStart { cancelScanJob() }

    override fun stopScan() {
        cancelScanJob()
    }

    // endregion

    // region ---- Connection ----

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun connect(deviceAddress: String, timeoutMs: Long) {
        cleanup()
        _connection.emit(ConnectionStatus.Connecting)

        val client = withTimeoutOrNull(timeoutMs) {
            ClientBleGatt.connect(appContext, deviceAddress, scope)
        } ?: run {
            _connection.emit(ConnectionStatus.Disconnected("Connection timeout", true))
            return
        }

        gatt = client

        client.discoverServices()
        _connection.emit(ConnectionStatus.Connected(deviceAddress))

        observeConnectionState(client, deviceAddress)
        observeGenericServices()
        startRssiPolling(client)
    }

    private fun observeConnectionState(client: ClientBleGatt, deviceAddress: String) {
        connectionStateJob?.cancel()
        connectionStateJob = client.connectionState
            .onEach { state ->
                val status = when (state) {
                    GattConnectionState.STATE_CONNECTED -> ConnectionStatus.Connected(deviceAddress)
                    GattConnectionState.STATE_DISCONNECTED -> ConnectionStatus.Disconnected(
                        "Disconnected",
                        true
                    )

                    GattConnectionState.STATE_CONNECTING -> ConnectionStatus.Connecting
                    GattConnectionState.STATE_DISCONNECTING -> ConnectionStatus.Disconnecting
                }
                status.let { _connection.emit(it) }
            }
            .launchIn(scope)
    }

    override fun observeCharacteristic(
        serviceUuid: UUID,
        characteristicUuid: UUID,
    ): Flow<ByteArray> =
        channelFlow {
            val client = gatt ?: run {
                Log.w(BLE_CORE_LOGGING_TAG, "No GATT, not connected")
                close()
                return@channelFlow
            }

            val service = withTimeoutOrNull(5000) {
                client.services.mapNotNull { it?.findService(serviceUuid) }.first()
            }

            if (service == null) {
                if (config.loggingEnabled) Log.w(
                    BLE_CORE_LOGGING_TAG,
                    "Service $serviceUuid not found after timeout"
                )
                return@channelFlow
            }

            val characteristic = service.findCharacteristic(characteristicUuid)
                ?: run {
                    Log.w(BLE_CORE_LOGGING_TAG, "Characteristic $characteristicUuid not found")
                    close()
                    return@channelFlow
                }

            val job = characteristic.getNotifications(0)
                .onEach { packet -> trySend(packet.value) }
                .launchIn(scope)

            awaitClose { job.cancel() }
        }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun observeGenericServices() {
        genericServiceJobs += observeCharacteristic(
            GenericBleGattSpec.Standard.BATTERY_SERVICE_UUID,
            GenericBleGattSpec.Standard.BATTERY_LEVEL_UUID
        ).onEach { raw ->
            val level = BatteryLevelDecoder.decodeBatteryLevel(raw)
            if (level != null) _batteryFlow.emit(level)
        }.launchIn(scope)

        scope.launch {
            readDeviceInformation()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private suspend fun readDeviceInformation() {
        val client = gatt ?: return

        runCatching {
            // Wait for services to be available with a timeout
            val service = withTimeoutOrNull(5000) {
                client.services.mapNotNull {
                    it?.findService(GenericBleGattSpec.Standard.DEVICE_INFORMATION_SERVICE_UUID)
                }.first()
            }

            if (service == null) {
                if (config.loggingEnabled) {
                    Log.w(BLE_CORE_LOGGING_TAG, "Device Information Service not found")
                }
                return
            }

            service.findCharacteristic(GenericBleGattSpec.Standard.FIRMWARE_REVISION_UUID)
                ?.read()?.value?.let { raw ->
                    _firmwareRevisionFlow.emit(raw.decodeToString())
                }

            service.findCharacteristic(GenericBleGattSpec.Standard.MANUFACTURER_NAME_UUID)
                ?.read()?.value?.let { raw ->
                    _deviceManufacturerFlow.emit(raw.decodeToString())
                }
        }.onFailure { error ->
            if (config.loggingEnabled) {
                Log.w(BLE_CORE_LOGGING_TAG, "Failed to read device info: ${error.message}")
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun startRssiPolling(client: ClientBleGatt) {
        if (config.rssiPollMs <= 0L) return

        genericServiceJobs += scope.launch {
            while (isActive) {
                runCatching { client.readRssi() }.onSuccess {
                    if (config.loggingEnabled) Log.d(BLE_CORE_LOGGING_TAG, "RSSI: $it")

                    _rssiFlow.emit(it)
                }

                delay(config.rssiPollMs)
            }
        }
    }

    override fun disconnect() {
        scope.launch {
            _connection.emit(ConnectionStatus.Disconnecting)
            cleanup()
            _connection.emit(ConnectionStatus.Disconnected("Manual disconnect", true))
        }
    }

    private fun cleanup() {
        cancelScanJob()
        cancelGenericServiceJobs()
        cancelConnectionStateJob()

        teardownGatt()
    }

    private fun cancelConnectionStateJob() {
        connectionStateJob?.cancel()
        connectionStateJob = null
    }

    private fun cancelGenericServiceJobs() {
        genericServiceJobs.forEach { it.cancel() }
        genericServiceJobs.clear()
    }

    private fun cancelScanJob() {
        scanJob?.cancel()
        scanJob = null
    }

    private fun teardownGatt() {
        runCatching { gatt?.disconnect() }
        gatt = null
    }

    // endregion

    private companion object Companion {
        const val BLE_CORE_LOGGING_TAG = "BleCore"
    }
}
