package com.slax.reader.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class CredentialsData(
    val endpoint: String = "",
    val token: String = ""
)

@Serializable
data class ChangesItem(
    val table: String = "",
    val id: String = "",
    val op: String = "",
    val data: Map<String, String?>? = null,
    val preData: Map<String, String?>? = null
)

@Serializable
data class AuthResult(
    val token: String = "",
    val user_id: String = "",
)

@Serializable
data class RefreshResult(
    val token: String = ""
)

@Serializable
data class AuthParams(
    val code: String,
    val redirect_uri: String,
    val platform: String,
    val type: String
)

@Serializable
data class BookmarkContentParam(
    val bookmark_uid: String
)