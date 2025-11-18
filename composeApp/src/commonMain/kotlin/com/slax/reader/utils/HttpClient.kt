package com.slax.reader.utils

import app.slax.reader.SlaxConfig
import com.slax.reader.data.preferences.AppPreferences
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

expect fun HttpClientConfig<*>.configureSslPinning(pins: List<String>)

fun getHttpClient(appPreferences: AppPreferences): HttpClient {
    val token = MutableStateFlow<String?>(null)
    CoroutineScope(Dispatchers.Default).launch {
        appPreferences.getAuthInfo().collect { info ->
            token.value = info?.token
        }
    }

    return HttpClient {
        if (SlaxConfig.BUILD_ENV == "release") {
//            configureSslPinning(SSLPins.allPins)
        }
        engine {

        }
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = SlaxConfig.BUILD_ENV == "dev"
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        defaultRequest {
            token.value?.let {
                bearerAuth(it)
            }
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
}