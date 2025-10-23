/*
 *
 *  * Copyright (c) 2025 StylesDevelopments. BikeConnect.
 *  *
 *  * Created by Taylor Styles on 23/10/2025, 13:03.
 *
 */

package com.stylesdevelopments.blecore.api.model

import android.bluetooth.BluetoothDevice

/**
 * Represents a single BLE message event — either received from or sent to a device.
 */
data class MessageEvent(
    val device: BluetoothDevice,
    val data: ByteArray,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MessageEvent

        if (device != other.device) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = device.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}