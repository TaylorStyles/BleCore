/*
 *
 *  * Copyright (c) 2025 StylesDevelopments. BikeConnect.
 *  *
 *  * Created by Taylor Styles on 23/10/2025, 13:37.
 *
 */

package com.stylesdevelopments.blecore.api

import android.bluetooth.BluetoothDevice
import com.stylesdevelopments.blecore.api.model.ConnectionStatus
import com.stylesdevelopments.blecore.api.model.MessageEvent
import kotlinx.coroutines.flow.Flow
import no.nordicsemi.android.kotlin.ble.core.scanner.BleScanResult
import java.util.UUID

interface BleClient {
    fun scan(): Flow<List<BleScanResult>>
    fun stopScan()

    suspend fun connect(deviceAddress: String, timeoutMs: Long = 10_000)
    fun disconnect()

    val connectionStatus: Flow<ConnectionStatus>

    fun observeCharacteristic(serviceUuid: UUID, characteristicUuid: UUID): Flow<ByteArray>


    /**
     * Observe the current RSSI value of the connected device.
     * May emit periodically depending on configuration (see BleConfig.rssiPollMs).
     */
    val rssiFlow: Flow<Int>

    /**
     * Observe standard Bluetooth battery level updates (0–100%).
     * This uses the Battery Service (0x180F / 0x2A19).
     */
    val batteryFlow: Flow<Int>

    val firmwareRevisionFlow: Flow<String>

    val deviceManufacturerFlow: Flow<String>
}