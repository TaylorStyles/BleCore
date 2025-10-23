/*
 *
 *  * Copyright (c) 2025 StylesDevelopments. BikeConnect.
 *  *
 *  * Created by Taylor Styles on 08/10/2025, 18:11.
 *
 */

package com.stylesdevelopments.blecore.internal.decoding

object BatteryLevelDecoder {

    /**
     * Decode Shimano (or standard GATT) battery level characteristic.
     * @param value Raw byte array from the BLE characteristic.
     * @return Battery level percentage (0–100) or null if invalid.
     */
    fun decodeBatteryLevel(value: ByteArray?): Int? {
        if (value == null || value.isEmpty()) return null

        // Unsigned byte → Int (0–255), clamp to 0–100 range
        val level = value[0].toInt() and 0xFF
        return level.coerceIn(0, 100)
    }
}
