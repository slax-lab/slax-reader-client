package com.slax.bridge.api

typealias SlaxBridgeCallback = (Result<Map<String, Any?>>) -> Unit
typealias SlaxBridgeHandler = (method: String, payload: Map<String, Any?>, callback: SlaxBridgeCallback) -> Unit

object NativeBridgeRegistry {
    var handler: SlaxBridgeHandler? = null
}
