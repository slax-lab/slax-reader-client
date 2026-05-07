package com.slax.reader.domain.auth

import androidx.compose.runtime.Composable

data class GoogleSignInResult(
    val idToken: String,
    val email: String? = null,
    val displayName: String? = null
)

expect class GoogleSignInProvider() {
    suspend fun signIn(): Result<GoogleSignInResult>
}

@Composable
expect fun rememberGoogleSignInProvider(): GoogleSignInProvider
