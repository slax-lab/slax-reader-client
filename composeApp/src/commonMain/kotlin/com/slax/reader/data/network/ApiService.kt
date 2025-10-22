package com.slax.reader.data.network

import app.slax.reader.SlaxConfig
import com.slax.reader.const.AppError
import com.slax.reader.data.network.dto.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class ApiService(
    private val httpClient: HttpClient,
) {

    private fun buildUrl(pathName: String, query: Map<String, String>?): String {
        val builder = URLBuilder()
        builder.takeFrom(SlaxConfig.API_BASE_URL)
        builder.path(pathName)
        query?.map { (k, v) -> builder.parameters.append(k, v) }
        return builder.toString()
    }

    private suspend inline fun <reified T> processResult(response: HttpResponse): HttpData<T> {
        if (response.status != HttpStatusCode.OK) {
            val errorMessage = try {
                val errorBody = response.body<ErrorResponse>()
                errorBody.message
            } catch (e: Exception) {
                "Error: ${response.status}"
            }
            throw AppError.ApiException.HttpError(
                code = response.status.value,
                message = errorMessage
            )
        }

        return response.body<HttpData<T>>()
    }

    private suspend inline fun <reified T> get(
        pathName: String, query: Map<String, String>?
    ): HttpData<T> {
        val url = buildUrl(pathName, query)
        val response = httpClient.get(url) {
            headers {
                append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }
        }
        return processResult(response)
    }

    private suspend inline fun <reified T> post(
        pathName: String, query: Map<String, String> = emptyMap(), body: Any? = null
    ): HttpData<T> {
        val url = buildUrl(pathName, query)
        val response = httpClient.post(url) {
            headers {
                append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }
            if (body != null) {
                setBody(body)
            }
        }
        return processResult(response)
    }

    private fun streamPost(
        pathName: String,
        query: Map<String, String> = emptyMap(),
        body: Any? = null
    ): Flow<String> = flow {
        val url = buildUrl(pathName, query)

        httpClient.preparePost(url, {
            headers {
                append(HttpHeaders.ContentType, "application/json")
                append(HttpHeaders.Accept, "text/event-stream")
            }
            setBody(body = body)
        }).execute { resp ->
            val channel = resp.bodyAsChannel()

            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: continue
                emit(line)
            }
        }
    }

    suspend fun getSyncToken(): HttpData<CredentialsData> {
        return post("/v1/sync/token")
    }

    suspend fun uploadChanges(changes: List<ChangesItem>): HttpData<Any> = withContext(Dispatchers.IO) {
        return@withContext post("/v1/sync/changes", body = changes)
    }

    suspend fun login(params: AuthParams): HttpData<AuthResult> {
        return post("/v1/user/login", body = params)
    }

    suspend fun refresh(): HttpData<RefreshResult> {
        return post("/v1/user/refresh")
    }

    suspend fun getBookmarkContent(id: String): String {
        val contentBuilder = StringBuilder()
        val resp = streamBookmarkContent(id)
        resp.collect { chunk ->
            contentBuilder.append(chunk)
        }
        return contentBuilder.toString()
    }

    private fun streamBookmarkContent(id: String): Flow<String> {
        return streamPost("/v1/bookmark/content", body = BookmarkContentParam(id))
    }

    suspend fun addBookmarkUrl(url: String, title: String?): HttpData<CollectionBookmarkResult> {
        return post(
            "/v1/bookmark/add_url", body = CollectionBookmarkParam(
                url, target_title = title
            )
        )
    }

    suspend fun addBookmarkWithContent(
        url: String,
        content: String?,
        title: String?
    ): HttpData<CollectionBookmarkResult> {
        return post(
            "/v1/bookmark/add_url", body = CollectionBookmarkParam(
                target_url = url, content = content, target_title = title
            )
        )
    }

}
