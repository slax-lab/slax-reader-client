package com.slax.reader.ui.login

import androidx.lifecycle.ViewModel
import com.mmk.kmpauth.google.GoogleUser
import com.slax.reader.const.AppError
import com.slax.reader.domain.auth.AppleSignInResult
import com.slax.reader.domain.auth.AuthDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class LoginViewModel(
    private val authDomain: AuthDomain,
) : ViewModel() {

    suspend fun appleSignIn(
        result: Result<AppleSignInResult>,
        onLoading: (isLoading: Boolean) -> Unit,
        onSuccess: () -> Unit,
        onError: (err: String) -> Unit
    ) {
        withContext(Dispatchers.Main) { onLoading(true) }
        try {
            result.onSuccess { appleResult ->
                val authResult = withContext(Dispatchers.IO) {
                    authDomain.signIn(code = appleResult.code, type = "apple", idToken = appleResult.idToken)
                }
                authResult.onSuccess {
                    withContext(Dispatchers.Main) { onSuccess() }
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
                    withContext(Dispatchers.Main) { onError(message) }
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    onError(it.message ?: "Apple Sign In failed")
                }
            }
        } catch (e: Exception) {
            println("Exception during appleSignIn: ${e.message}")
            withContext(Dispatchers.Main) {
                onError(e.message ?: "Unknown error occurred")
            }
        } finally {
            withContext(Dispatchers.Main) { onLoading(false) }
        }
    }

    suspend fun googleSignIn(
        result: GoogleUser?,
        onLoading: (isLoading: Boolean) -> Unit,
        onSuccess: () -> Unit,
        onError: (err: String) -> Unit
    ) {
        if (result == null) {
            return
        }

        if (result.idToken == "") {
            onError("Failed to get Google ID token")
            return
        }
        withContext(Dispatchers.Main) { onLoading(true) }
        val idToken = result.idToken
        try {
            val authResult = withContext(Dispatchers.IO) {
                authDomain.signIn(idToken, type = "google")
            }
            authResult.onSuccess {
                withContext(Dispatchers.Main) { onSuccess() }
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
                withContext(Dispatchers.Main) { onError(message) }
            }
        } catch (e: Exception) {
            println("Exception during signIn: ${e.message}")
            withContext(Dispatchers.Main) {
                onError(e.message ?: "Unknown error occurred")
            }
        } finally {
            withContext(Dispatchers.Main) { onLoading(false) }
        }
    }
}