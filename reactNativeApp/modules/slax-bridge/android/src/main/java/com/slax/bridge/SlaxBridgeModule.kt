package com.slax.bridge

import com.slax.bridge.api.NativeBridgeRegistry
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.Promise

class SlaxBridgeModule : Module() {
    override fun definition() = ModuleDefinition {
        Name("SlaxBridge")

        AsyncFunction("invoke") { method: String, payload: Map<String, Any?>, promise: Promise ->
            val handler = NativeBridgeRegistry.handler
                ?: run {
                    promise.reject("ERR_NOT_INITIALIZED", "NativeBridge not initialized", null)
                    return@AsyncFunction
                }
            handler.invoke(method, payload) { result ->
                result.fold(
                    onSuccess = { promise.resolve(it) },
                    onFailure = { promise.reject("ERR_BRIDGE", it.message, it) }
                )
            }
        }
    }
}
