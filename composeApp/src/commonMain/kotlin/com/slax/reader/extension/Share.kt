package com.slax.reader.extension

import app.slax.reader.SlaxConfig
import com.slax.reader.const.AppError
import com.slax.reader.data.network.ApiService
import com.slax.reader.data.preferences.getPreferences
import com.slax.reader.utils.AppEnv
import com.slax.reader.utils.AppLog
import com.slax.reader.utils.LogProcess
import com.slax.reader.utils.bookmarkEvent
import com.slax.reader.utils.platformName
import com.slax.reader.utils.platformEngine
import com.slax.reader.utils.networkEnvironmentSummary
import com.slax.reader.utils.resolveSystemDns
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.request.*
import io.ktor.client.utils.HttpRequestIsReadyForSending
import io.ktor.client.utils.HttpResponseReceived
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.userAgent
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

private const val SHARE_TIMEOUT_MILLIS = 5_000L
private const val SHARE_MAX_ATTEMPTS = 3
private val shareDiagnosticScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

fun getShareLabelText(key: String): String {
    when (key) {
        "collecting" -> return "Collecting"
        "success" -> return "Success!"
        "failed" -> return "Failed!"
    }
    return "Collecting"
}

// 本文件是 Slax Reader iOS / Android Share Extension 专用的逻辑复用
// 请勿在此处使用任何 PowerSync / UI / DI 的逻辑
@OptIn(ExperimentalTime::class)
suspend fun collectionShare(content: String, title: String?, body: String?): String {
    AppLog.initialize(LogProcess.SHARE_EXTENSION)
    val event = bookmarkEvent.action("add_start").channel("app").method("share_extension")
    var traceId = "not-started"
    var requestAttempt = 0
    var requestAttemptStartedAt = 0L
    try {
        val duration = measureTime {
            val regex = "^((https|http)?:\\/\\/)[^\\s]+".toRegex()
            val match = regex.find(content) ?: return shareFailure("Not have URL content to collect.")
            val url = match.value

            val preferences = getPreferences()
            if (AppEnv.current == null){
                AppEnv.init(preferences.getSelectedEnv())
            }
            val token = preferences.getAuthInfoSuspend()
            if (token.isNullOrEmpty()) return shareFailure("Not have auth info, please login first.")

            val apiUrl = Url(AppEnv.apiBaseUrl)
            val targetHost = runCatching { Url(url).host }.getOrDefault("unknown")
            traceId = buildShareTraceId()
            AppLog.i(
                "share_start trace_id=$traceId platform=$platformName " +
                    "app_version=${SlaxConfig.APP_VERSION_NAME} app_build=${SlaxConfig.APP_VERSION_CODE} " +
                    "endpoint=${apiUrl.protocol.name}://${apiUrl.host}:${apiUrl.port}/v1/bookmark/add_url " +
                    "target_host=$targetHost has_title=${!title.isNullOrBlank()} has_body=${!body.isNullOrBlank()}"
            )
            AppLog.i("share_network trace_id=$traceId ${networkEnvironmentSummary()}")
            logDnsDiagnostics(traceId, apiUrl.host)

            val httpClient = HttpClient(platformEngine()) {
                install(HttpRequestRetry) {
                    maxRetries = SHARE_MAX_ATTEMPTS - 1
                    retryOnServerErrors()
                    retryOnException(maxRetries = SHARE_MAX_ATTEMPTS - 1, retryOnTimeout = true)
                    delayMillis(respectRetryAfterHeader = false) { 0 }
                }
                install(ContentNegotiation) {
                    json(Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    })
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = SHARE_TIMEOUT_MILLIS
                    connectTimeoutMillis = SHARE_TIMEOUT_MILLIS
                    socketTimeoutMillis = SHARE_TIMEOUT_MILLIS
                }
                defaultRequest {
                    userAgent("SlaxReader/${platformName} ${SlaxConfig.APP_VERSION_NAME} (${SlaxConfig.APP_VERSION_CODE})")
                    bearerAuth(token)
                    header("X-Slax-Client-Request-Id", traceId)
                }
            }
            httpClient.monitor.subscribe(HttpRequestIsReadyForSending) { request ->
                requestAttempt += 1
                requestAttemptStartedAt = Clock.System.now().toEpochMilliseconds()
                AppLog.i(
                    "share_request_start trace_id=$traceId attempt=$requestAttempt/$SHARE_MAX_ATTEMPTS " +
                        "method=${request.method.value} host=${request.url.host} path=${request.url.encodedPath} " +
                        "request_timeout_ms=5000 connect_timeout_ms=5000 socket_timeout_ms=5000"
                )
            }
            httpClient.monitor.subscribe(HttpRequestRetryEvent) { retry ->
                val elapsed = Clock.System.now().toEpochMilliseconds() - requestAttemptStartedAt
                AppLog.w(
                    "share_request_retry trace_id=$traceId attempt=$requestAttempt/$SHARE_MAX_ATTEMPTS elapsed_ms=$elapsed " +
                        "status=${retry.response?.status?.value ?: "none"} " +
                        "error_type=${retry.cause?.let { it::class.simpleName } ?: "none"} " +
                        "error=${retry.cause?.message ?: "none"}"
                )
            }
            httpClient.monitor.subscribe(HttpResponseReceived) { response ->
                val rayId = response.headers["X-Slax-Ray-Id"] ?: response.headers["CF-Ray"] ?: "none"
                val elapsed = Clock.System.now().toEpochMilliseconds() - requestAttemptStartedAt
                AppLog.i(
                    "share_response trace_id=$traceId attempt=$requestAttempt/$SHARE_MAX_ATTEMPTS elapsed_ms=$elapsed status=${response.status.value} " +
                        "ray_id=$rayId content_type=${response.headers[HttpHeaders.ContentType] ?: "none"}"
                )
            }
            try {
                AppLog.i("share_api_call trace_id=$traceId api=add_url")
                ApiService(httpClient).addBookmarkUrl(url, title)
            } finally {
                httpClient.close()
            }
        }
        AppLog.i("share_success trace_id=$traceId elapsed_ms=${duration.inWholeMilliseconds}")
        runCatching { event.param("status", "success").param("duration", duration).send() }
            .onFailure { AppLog.w("share_metric_failed trace_id=$traceId error=${it.message}") }
        return "ok"
    } catch (e: Exception) {
        val attemptElapsed = if (requestAttemptStartedAt == 0L) {
            0L
        } else {
            Clock.System.now().toEpochMilliseconds() - requestAttemptStartedAt
        }
        AppLog.e(
            "share_failed trace_id=$traceId attempt=$requestAttempt/$SHARE_MAX_ATTEMPTS " +
                "attempt_elapsed_ms=$attemptElapsed error_type=${e::class.simpleName} error=${e.message}",
            e,
        )
        runCatching { event.param("status", "failed").send() }
            .onFailure { AppLog.w("share_metric_failed trace_id=$traceId error=${it.message}") }
        // 不直接暴露底层异常信息，避免显示无关的内部错误
        val userMessage = when {
            e is HttpRequestTimeoutException || e is ConnectTimeoutException || e is SocketTimeoutException ->
                "Request timed out, please try again."
            e.message?.contains("Unable to resolve host", ignoreCase = true) == true -> "Network unavailable, please check your connection."
            (e as? AppError.ApiException.HttpError)?.code == 401 -> "Auth expired, please reopen the app to login again."
            else -> "Collection failed, please try again."
        }
        return userMessage
    } finally {
        AppLog.flush()
    }
}

suspend fun logShareExtensionError(message: String) {
    AppLog.initialize(LogProcess.SHARE_EXTENSION)
    AppLog.e("share_extension_ui_failed error=$message")
    AppLog.flush()
}

private fun shareFailure(message: String): String {
    AppLog.w("share_validation_failed reason=$message")
    return message
}

@OptIn(ExperimentalTime::class)
private fun buildShareTraceId(): String = buildString {
    append(Clock.System.now().toEpochMilliseconds().toString(16))
    append('-')
    append(Random.nextLong().toULong().toString(16))
}

@OptIn(ExperimentalTime::class)
private fun logDnsDiagnostics(traceId: String, host: String) {
    shareDiagnosticScope.launch {
        val startedAt = Clock.System.now().toEpochMilliseconds()
        AppLog.i("share_dns_start trace_id=$traceId host=$host")
        val result = withTimeoutOrNull(SHARE_TIMEOUT_MILLIS) { runCatching { resolveSystemDns(host) } }
        val elapsed = Clock.System.now().toEpochMilliseconds() - startedAt
        when {
            result == null -> AppLog.w("share_dns_timeout trace_id=$traceId host=$host elapsed_ms=$elapsed")
            result.isSuccess -> AppLog.i(
                "share_dns_success trace_id=$traceId host=$host elapsed_ms=$elapsed " +
                    "addresses=${result.getOrThrow().joinToString(",")}"
            )
            else -> {
                val error = result.exceptionOrNull()
                AppLog.w("share_dns_failed trace_id=$traceId host=$host elapsed_ms=$elapsed error=${error?.message}", error)
            }
        }
    }
}
