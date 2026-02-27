package com.slax.reader.reactnative

import com.slax.bridge.api.NativeBridgeHandler
import com.slax.bridge.api.NativeBridgeRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object BrownfieldMessagingBridge {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun ensureRegistered() {
        if (NativeBridgeRegistry.handler != null) return
        NativeBridgeRegistry.handler = NativeBridgeHandler { method, payload, callback ->
            scope.launch {
                try {
                    val result = ReactNativeMessageDispatcher.invoke(method, payload)
                    callback(Result.success(result))
                } catch (e: Exception) {
                    callback(Result.failure(e))
                }
            }
        }
    }
}
