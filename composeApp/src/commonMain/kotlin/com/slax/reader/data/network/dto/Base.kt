package com.slax.reader.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class HttpData<T>(
    val data: T?,
    val message: String,
    val code: Int
)

@Serializable
data class ErrorResponse(
    /** Error name from the server envelope, e.g. LAB_FEATURE_DISABLED */
    val data: String? = null,
    val message: String = "",
    val code: Int = 0
)
