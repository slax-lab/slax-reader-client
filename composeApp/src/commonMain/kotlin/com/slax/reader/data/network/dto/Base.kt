package com.slax.reader.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class HttpData<T>(
    val data: T?,
    val message: String,
    val code: Int
)
