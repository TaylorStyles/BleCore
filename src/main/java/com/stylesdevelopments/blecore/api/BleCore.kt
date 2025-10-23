/*
 *
 *  * Copyright (c) 2025 StylesDevelopments. BikeConnect.
 *  *
 *  * Created by Taylor Styles on 23/10/2025, 14:28.
 *
 */

package com.stylesdevelopments.blecore.api

import android.app.Application
import android.content.Context
import com.stylesdevelopments.blecore.internal.BleClientImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

object BleCore {

    private var _client: BleClient? = null

    val client: BleClient
        get() = _client ?: throw IllegalStateException("BleCore not initialized — call initialize() first")

    fun initialize(
        context: Context,
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        config: BleConfig = BleConfig()
    ): BleCore {
        if (_client != null) return this // Already initialized

        val appContext = (context.applicationContext as? Application)
            ?: throw IllegalArgumentException("Must pass an Application context")

        _client = BleClientImpl(appContext, config, scope)

        return this
    }
}
