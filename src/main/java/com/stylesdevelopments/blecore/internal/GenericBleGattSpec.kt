/*
 *
 *  * Copyright (c) 2025 StylesDevelopments. BikeConnect.
 *  *
 *  * Created by Taylor Styles on 23/10/2025, 16:04.
 *
 */

package com.stylesdevelopments.blecore.internal

import java.util.UUID

object GenericBleGattSpec {

    private const val STANDARD_BASE_UUID_FORMAT = "0000%s-0000-1000-8000-00805f9b34fb"

    private fun buildStandardUUID(shortCode: String): UUID =
        UUID.fromString(STANDARD_BASE_UUID_FORMAT.format(shortCode))

    object Standard {
        val DEVICE_INFORMATION_SERVICE_UUID: UUID = buildStandardUUID("180A")
        val FIRMWARE_REVISION_UUID: UUID = buildStandardUUID("2A26")
        val MANUFACTURER_NAME_UUID: UUID = buildStandardUUID("2A29")
        val BATTERY_SERVICE_UUID: UUID = buildStandardUUID("180F")
        val BATTERY_LEVEL_UUID: UUID = buildStandardUUID("2A19")
    }
}