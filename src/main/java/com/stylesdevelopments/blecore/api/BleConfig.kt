/*
 *
 *  * Copyright (c) 2025 StylesDevelopments. BikeConnect.
 *  *
 *  * Created by Taylor Styles on 23/10/2025, 13:37.
 *
 */

package com.stylesdevelopments.blecore.api

import java.util.UUID

data class BleConfig(
    val scanFilters: List<UUID> = emptyList(),
    val autoReconnect: Boolean = false,
    val loggingEnabled: Boolean = true,
    val rssiPollMs: Long = 1_500L
)