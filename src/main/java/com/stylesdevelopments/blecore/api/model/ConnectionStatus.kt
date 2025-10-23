/*
 *
 *  * Copyright (c) 2025 StylesDevelopments. BikeConnect.
 *  *
 *  * Created by Taylor Styles on 23/10/2025, 13:03.
 *
 */

package com.stylesdevelopments.blecore.api.model

sealed class ConnectionStatus {
    data object Connecting : ConnectionStatus()
    data class Connected(val address: String) : ConnectionStatus()
    data object Disconnecting : ConnectionStatus()
    data class Disconnected(val reason: String? = null, val wasError: Boolean = false) : ConnectionStatus()
}