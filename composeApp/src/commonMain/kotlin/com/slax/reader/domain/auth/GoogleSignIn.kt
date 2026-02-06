package com.slax.reader.domain.auth

data class GoogleSignInResult(
    val idToken: String,
    val email: String? = null,
    val displayName: String? = null
)

expect class GoogleSignInProvider() {
    suspend fun signIn(): Result<GoogleSignInResult>
    fun isAvailable(): Boolean
}
