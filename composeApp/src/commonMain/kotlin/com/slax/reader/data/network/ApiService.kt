package com.slax.reader.data.network

import app.slax.reader.SlaxConfig
import com.slax.reader.const.AppError
import com.slax.reader.data.network.dto.*
import com.slax.reader.data.preferences.AppPreferences
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.first

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

    private suspend inline fun <reified T> get(
        pathName: String, query: Map<String, String>?, options: Options?
    ): HttpData<T> {
        return preferences.getAuthToken().first().let { token ->
            if (token.isEmpty() && options?.notAuthorized != true) {
                throw AppError.AuthException.TokenNotFound("No auth token found")
            }
            val url = buildUrl(pathName, query)
            val response = httpClient.get(url) {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
            }
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

            response.body<HttpData<T>>()
        }
    }

    private suspend inline fun <reified T> post(
        pathName: String, query: Map<String, String>?, body: Any?, options: Options = Options()
    ): HttpData<T> {
        return preferences.getAuthToken().first().let { token ->
            if (token.isEmpty() && !options.notAuthorized) {
                throw AppError.AuthException.TokenNotFound("No auth token found")
            }
            val url = buildUrl(pathName, query)
            val response = httpClient.post(url) {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
                if (body != null) {
                    setBody(body)
                }
            }
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

            response.body<HttpData<T>>()
        }
    }

    suspend fun getSyncToken(): HttpData<CredentialsData> {
        return post("/v1/sync/token", query = null, null)
    }

    suspend fun uploadChanges(changes: List<ChangesItem>): HttpData<Any> {
        return post("/v1/sync/changes", query = null, changes, Options(notAuthorized = false))
    }

    suspend fun login(params: AuthParams): HttpData<AuthResult> {
        return post("/v1/user/login", query = null, body = params, Options(notAuthorized = true))
    }
}
