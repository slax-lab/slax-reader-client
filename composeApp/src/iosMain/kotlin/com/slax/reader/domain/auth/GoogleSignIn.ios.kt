package com.slax.reader.domain.auth

import app.slax.reader.SlaxConfig
import com.slax.reader.googlesignin.GoogleSignInBridge
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
actual class GoogleSignInProvider {

    actual suspend fun signIn(): Result<GoogleSignInResult> =
        suspendCancellableCoroutine { continuation ->
            val bridge = GoogleSignInBridge.shared

            bridge.signInWithServerClientId(SlaxConfig.GOOGLE_AUTH_SERVER_ID) { idToken, email, displayName, error ->
                if (error != null) {
                    if (error.contains("cancel", ignoreCase = true)) {
                        continuation.resume(Result.failure(Exception("User canceled")))
                    } else {
                        continuation.resume(Result.failure(Exception("Sign in failed: $error")))
                    }
                    return@signInWithServerClientId
                }

                if (idToken != null) {
                    val result = GoogleSignInResult(
                        idToken = idToken,
                        email = email,
                        displayName = displayName
                    )
                    continuation.resume(Result.success(result))
                } else {
                    continuation.resume(Result.failure(Exception("Failed to get ID token")))
                }
            }

            continuation.invokeOnCancellation {
            }
        }
}
