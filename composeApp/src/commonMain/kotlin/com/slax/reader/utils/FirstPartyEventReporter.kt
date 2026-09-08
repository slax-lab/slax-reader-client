package com.slax.reader.utils

import app.slax.reader.SlaxConfig
import com.slax.reader.data.preferences.AppPreferences
import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Serializable
private data class ClientEvent(
    val event_name: String,
    val occurred_at: String,
    val properties: JsonObject,
)

@Serializable
private data class ClientEventBatch(val events: List<ClientEvent>)

/**
 * Best-effort first-party event collector client. Firebase remains unchanged; this reporter
 * writes the documented client contract to the backend /events endpoint and never blocks UI.
 */
class FirstPartyEventReporter(
    private val httpClient: HttpClient,
    private val appPreferences: AppPreferences,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @OptIn(ExperimentalTime::class)
    fun track(eventName: String, properties: Map<String, String> = emptyMap()) {
        if (eventName.isBlank()) return
        scope.launch {
            runCatching {
                val deviceId = appPreferences.getOrCreateDeviceId()
                val eventProperties = buildJsonObject {
                    put("platform", platformType)
                    put("locale", LocaleString.currentLocale)
                    put("client_version", "${SlaxConfig.APP_VERSION_NAME} (${SlaxConfig.APP_VERSION_CODE})")
                    properties.forEach { (key, value) -> put(key, value) }
                }
                httpClient.post("${AppEnv.apiBaseUrl}/events") {
                    contentType(ContentType.Application.Json)
                    headers.append("X-Device-ID", deviceId)
                    headers.append("X-CLIENT-TYPE", platformType)
                    headers.append("X-CLIENT-VERSION", "${SlaxConfig.APP_VERSION_NAME} (${SlaxConfig.APP_VERSION_CODE})")
                    setBody(
                        ClientEventBatch(
                            events = listOf(
                                ClientEvent(
                                    event_name = eventName,
                                    occurred_at = Clock.System.now().toString(),
                                    properties = eventProperties,
                                )
                            )
                        )
                    )
                }
            }
        }
    }

}
