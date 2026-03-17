package com.slax.reader.extension

import com.slax.reader.data.network.ApiService
import com.slax.reader.data.preferences.getPreferences
import com.slax.reader.utils.bookmarkEvent
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlin.time.measureTime

fun getShareLabelText(key: String): String {
    when (key) {
        "collecting" -> return "Collecting"
        "success" -> return "Success!"
        "failed" -> return "Failed!"
    }
    return "Collecting"
}

// 本文件是 Slax Reader iOS Share Extension 专用的逻辑复用
// 请勿在此处使用任何 PowerSync / UI / DI 的逻辑
suspend fun collectionShare(content: String, title: String?, body: String?): String {
    val event = bookmarkEvent.action("add_start").channel("app").method("share_extension")
    try {
        val duration = measureTime {
            val regex = "^((https|http)?:\\/\\/)[^\\s]+".toRegex()
            val match = regex.find(content) ?: return "Not have URL content to collect."
            val url = match.value

            val preferences = getPreferences()
            val token = preferences.getAuthInfoSuspend()
            if (token.isNullOrEmpty()) return "Not have auth info, please login first."

            val httpClient = HttpClient {
                install(ContentNegotiation) {
                    json(Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    })
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = 15000
                    connectTimeoutMillis = 8000
                    socketTimeoutMillis = 8000
                }
                defaultRequest {
                    bearerAuth(token)
                }
            }
            val apiSvc = ApiService(httpClient)
            apiSvc.addBookmarkUrl(url, title)
        }
        event.param("status", "success").param("duration", duration).send()
        return "ok"
    } catch (e: Exception) {
        println("collectionShare failed: ${e.message}")
        event.param("status", "failed").send()
        // 不直接暴露底层异常信息，避免显示无关的内部错误
        val userMessage = when {
            e.message?.contains("timeout", ignoreCase = true) == true -> "Request timed out, please try again."
            e.message?.contains("Unable to resolve host", ignoreCase = true) == true -> "Network unavailable, please check your connection."
            e.message?.contains("401") == true -> "Auth expired, please reopen the app to login again."
            else -> "Collection failed, please try again."
        }
        return userMessage
    }
}
