package com.slax.reader.domain.auth

interface AuthGateway {
    suspend fun signIn(
        code: String,
        type: String,
        redirectUrl: String = "",
        idToken: String = ""
    ): Result<Unit>
}
