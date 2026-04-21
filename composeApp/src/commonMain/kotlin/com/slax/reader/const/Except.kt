package com.slax.reader.const

sealed class AppError : Exception() {
    abstract override val message: String

    sealed class AuthException : AppError() {
        data class TokenNotFound(override val message: String) : AuthException()
        data class TokenExpired(override val message: String) : AuthException()
        data class Unauthorized(override val message: String) : AuthException()
    }

    sealed class ApiException : AppError() {
        data class HttpError(
            val code: Int,
            override val message: String
        ) : ApiException()

        data class NetworkError(override val message: String) : ApiException()
        data class ParseError(override val message: String) : ApiException()
        data class Timeout(override val message: String) : ApiException()
    }

    sealed class StorageException : AppError() {
        data class ReadError(override val message: String) : StorageException()
        data class WriteError(override val message: String) : StorageException()
    }

    sealed class CommentException : AppError() {
        data object TooLong : CommentException() {
            override val message: String = "Comment must not exceed 1500 characters"
        }
        data object EmptyComment : CommentException() {
            override val message: String = "Comment content is required"
        }
    }
}