package com.slax.reader.reactnative

interface ReactNativeMethodHandler {
    val supportedMethods: Set<String>
    suspend fun handle(method: String, payload: Map<String, Any?>): Map<String, Any?>
}

object ReactNativeMessageDispatcher {
    private val handlers: List<ReactNativeMethodHandler> by lazy {
        listOf(FeedbackBridge)
    }

    suspend fun invoke(method: String, payload: Map<String, Any?>): Map<String, Any?> {
        val handler = handlers.firstOrNull { method in it.supportedMethods }
            ?: throw IllegalArgumentException("No handler for method: $method")
        return handler.handle(method, payload)
    }
}
