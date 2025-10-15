package com.slax.reader.data.network

import app.slax.reader.SlaxConfig
import com.slax.reader.const.AppError
import com.slax.reader.data.network.dto.*
import com.slax.reader.data.preferences.AppPreferences
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

data class Options(
    val notAuthorized: Boolean = false
)

class ApiService(
    private val httpClient: HttpClient,
    private val preferences: AppPreferences,
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
        pathName: String, query: Map<String, String>?, options: Options?
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
        pathName: String, query: Map<String, String> = emptyMap(), body: Any? = null, options: Options = Options()
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

    suspend fun getSyncToken(): HttpData<CredentialsData> {
        return post("/v1/sync/token")
    }

    suspend fun uploadChanges(changes: List<ChangesItem>): HttpData<Any> {
        return post("/v1/sync/changes", body = changes, options = Options(notAuthorized = false))
    }

    suspend fun login(params: AuthParams): HttpData<AuthResult> {
        return post("/v1/user/login", body = params, options = Options(notAuthorized = true))
    }

    suspend fun refresh(): HttpData<RefreshResult> {
        return post("/v1/user/refresh")
    }
}
