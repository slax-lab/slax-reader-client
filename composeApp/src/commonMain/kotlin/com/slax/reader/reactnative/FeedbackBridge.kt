package com.slax.reader.reactnative

import com.slax.reader.data.network.ApiService
import com.slax.reader.data.network.dto.FeedbackParams
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object FeedbackBridge : KoinComponent, ReactNativeMethodHandler {

    override val supportedMethods: Set<String> = setOf("feedback.submit")

    private val apiService: ApiService by inject()

    override suspend fun handle(method: String, payload: Map<String, Any?>): Map<String, Any?> {
        apiService.sendFeedback(
            FeedbackParams(
                bookmark_uuid = payload["bookmark_uuid"]?.toString(),
                entry_point = payload["entry_point"]?.toString().orEmpty(),
                type = payload["type"]?.toString() ?: "parse_error",
                content = payload["content"]?.toString().orEmpty(),
                platform = "app",
                environment = payload["environment"]?.toString().orEmpty(),
                version = payload["version"]?.toString().orEmpty(),
                allow_follow_up = payload["allow_follow_up"].asBoolean(),
                target_url = payload["target_url"]?.toString(),
            )
        )
        return emptyMap()
    }
}

private fun Any?.asBoolean(): Boolean {
    return when (this) {
        is Boolean -> this
        is Number -> this.toInt() != 0
        is String -> this.equals("true", ignoreCase = true) || this == "1"
        else -> false
    }
}
