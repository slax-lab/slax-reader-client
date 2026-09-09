package com.slax.reader.ui.login

import androidx.lifecycle.ViewModel
import com.slax.reader.const.AppError
import com.slax.reader.domain.auth.AppleSignInResult
import com.slax.reader.domain.auth.GoogleSignInResult
import com.slax.reader.domain.auth.AuthGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class LoginViewModel(
    private val authDomain: AuthGateway,
    private val mainContext: CoroutineContext = Dispatchers.Main,
    private val ioContext: CoroutineContext = Dispatchers.IO,
) : ViewModel() {

    suspend fun appleSignIn(
        result: Result<AppleSignInResult>,
        onLoading: (isLoading: Boolean) -> Unit,
        onSuccess: () -> Unit,
        onError: (err: String) -> Unit
    ) {
        withContext(mainContext) { onLoading(true) }
        try {
            result.onSuccess { appleResult ->
                val authResult = withContext(ioContext) {
                    authDomain.signIn(code = appleResult.code, type = "apple", idToken = appleResult.idToken)
                }
                authResult.onSuccess {
                    withContext(mainContext) { onSuccess() }
                }.onFailure {
                    val message = when (it) {
                        is AppError.ApiException.HttpError -> {
                            "Login failed (${it.code}): ${it.message}"
                        }

                        is AppError.AuthException -> {
                            "Auth error: ${it.message}"
                        }

                        else -> {
                            it.message ?: "Unknown error occurred"
                        }
                    }
                    withContext(mainContext) { onError(message) }
                }
            }.onFailure {
                withContext(mainContext) {
                    onError(it.message ?: "Apple Sign In failed")
                }
            }
        } catch (e: Exception) {
            println("Exception during appleSignIn: ${e.message}")
            withContext(mainContext) {
                onError(e.message ?: "Unknown error occurred")
            }
        } finally {
            withContext(mainContext) { onLoading(false) }
        }
    }

    suspend fun googleSignIn(
        result: Result<GoogleSignInResult>,
        onLoading: (isLoading: Boolean) -> Unit,
        onSuccess: () -> Unit,
        onError: (err: String) -> Unit
    ) {
        withContext(mainContext) { onLoading(true) }
        try {
            result.onSuccess { googleResult ->
                if (googleResult.idToken.isEmpty()) {
                    withContext(mainContext) { onError("Failed to get Google ID token") }
                    return
                }
                val authResult = withContext(ioContext) {
                    authDomain.signIn(googleResult.idToken, type = "google")
                }
                authResult.onSuccess {
                    withContext(mainContext) { onSuccess() }
                }.onFailure {
                    val message = when (it) {
                        is AppError.ApiException.HttpError -> {
                            "Login failed (${it.code}): ${it.message}"
                        }

                        is AppError.AuthException -> {
                            "Auth error: ${it.message}"
                        }

                        else -> {
                            it.message ?: "Unknown error occurred"
                        }
                    }
                    withContext(mainContext) { onError(message) }
                }
            }.onFailure {
                withContext(mainContext) {
                    onError(it.message ?: "Google Sign In failed")
                }
            }
        } catch (e: Exception) {
            println("Exception during signIn: ${e.message}")
            withContext(mainContext) {
                onError(e.message ?: "Unknown error occurred")
            }
        } finally {
            withContext(mainContext) { onLoading(false) }
        }
    }
}
