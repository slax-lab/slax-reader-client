package com.slax.reader.utils

import app.slax.reader.SlaxConfig
import com.slax.reader.data.preferences.AppPreferences
import io.ktor.client.*
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

expect fun platformEngine(): HttpClientEngine

fun getHttpClient(appPreferences: AppPreferences): HttpClient {
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
                if (response.status == HttpStatusCode.Unauthorized) {
                    CoroutineScope(Dispatchers.Default).launch {
                        appPreferences.clearAuthToken()
                    }
                }
            }
        }
    }

    client.plugin(HttpSend).intercept { request ->
        appPreferences.getAuthInfoSuspend()?.let { request.bearerAuth(it) }
        execute(request)
    }

    return client
}