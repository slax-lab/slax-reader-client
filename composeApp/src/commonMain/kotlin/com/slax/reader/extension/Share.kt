package com.slax.reader.extension

import com.slax.reader.data.network.ApiService
import com.slax.reader.data.preferences.getPreferences
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

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
    val regex = "^((https|http)?:\\/\\/)[^\\s]+".toRegex()
    val match = regex.find(content) ?: return "Not have URL content to collect."
    val url = match.value
    println("match url find: ${url} \n title: ${title}")

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
        defaultRequest {
            bearerAuth(token)
        }
    }
    val apiSvc = ApiService(httpClient)

    try {
        if (body == null) {
            apiSvc.addBookmarkUrl(url, title)
        } else {
            apiSvc.addBookmarkWithContent(url, title, body)
        }
        return "ok"
    } catch (e: Exception) {
        println("Collection failed: ${e.message}")
        return e.message ?: "Collection failed."
    }
}
