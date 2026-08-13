package com.slax.reader.utils

import app.slax.reader.SlaxConfig
import com.slax.reader.data.preferences.AppPreferences
import io.ktor.client.*
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.request
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

expect fun platformEngine(): HttpClientEngine

internal fun bearerToken(authorization: String?): String? {
    val value = authorization?.trim() ?: return null
    val separator = value.indexOf(' ')
    if (separator <= 0 || !value.substring(0, separator).equals("Bearer", ignoreCase = true)) return null
    return value.substring(separator + 1).trim().takeIf { it.isNotEmpty() }
}

internal fun isSameOrigin(requestUrl: Url, apiUrl: Url): Boolean =
    requestUrl.protocol == apiUrl.protocol &&
        requestUrl.host.equals(apiUrl.host, ignoreCase = true) &&
        requestUrl.port == apiUrl.port

internal fun shouldClearAuth(
    status: HttpStatusCode,
    requestUrl: Url,
    apiUrl: Url,
    requestToken: String?,
    currentToken: String?,
): Boolean = status == HttpStatusCode.Unauthorized &&
    isSameOrigin(requestUrl, apiUrl) &&
    requestToken != null &&
    requestToken == currentToken

fun getHttpClient(appPreferences: AppPreferences): HttpClient {
    val apiUrl = Url(AppEnv.apiBaseUrl)
    val client = HttpClient(platformEngine()) {
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 15_000
            requestTimeoutMillis = 30_000
        }
        install(HttpRequestRetry) {
            maxRetries = 2
            exponentialDelay()
        }
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = SlaxConfig.BUILD_ENV == "dev"
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        defaultRequest {
            userAgent("SlaxReader/${platformName} ${SlaxConfig.APP_VERSION_NAME} (${SlaxConfig.APP_VERSION_CODE})")
        }
        HttpResponseValidator {
            validateResponse { response ->
                val requestToken = bearerToken(response.request.headers[HttpHeaders.Authorization])
                if (
                    response.status == HttpStatusCode.Unauthorized &&
                    isSameOrigin(response.request.url, apiUrl) &&
                    requestToken != null
                ) {
                    val currentToken = appPreferences.getAuthInfoSuspend()
                    if (shouldClearAuth(response.status, response.request.url, apiUrl, requestToken, currentToken)) {
                        appPreferences.clearAuthTokenIfMatches(requestToken)
                    }
                }
            }
        }
    }

    client.plugin(HttpSend).intercept { request ->
        if (isSameOrigin(request.url.build(), apiUrl)) {
            appPreferences.getAuthInfoSuspend()?.let { request.bearerAuth(it) }
        }
        execute(request)
    }

    return client
}
