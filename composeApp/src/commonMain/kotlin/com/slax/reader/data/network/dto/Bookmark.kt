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

@Serializable
data class CollectionBookmarkParam(
    val target_url: String,
    val target_title: String? = null,
    val content: String? = null
)

@Serializable
data class CollectionBookmarkResult(
    val bmId: String
)

@Serializable
data class BookmarkOverviewParam(
    val bookmark_uid: String,
    val force: Boolean = false
)

@Serializable
data class TagInfo(
    val id: Int,
    val name: String
)

@Serializable
data class OverviewDataWrapper(
    val overview: String? = null,
    val tags: List<TagInfo>? = null,
    val tag: TagInfo? = null,
    val key_takeaways: List<String>? = null,
    val done: Boolean? = null
)

@Serializable
data class OverviewSocketData(
    val type: String,
    val data: OverviewDataWrapper? = null,
    val message: String? = null
)

sealed class OverviewResponse {
    data class Overview(val content: String) : OverviewResponse()
    data class Tags(val content: List<TagInfo>) : OverviewResponse()
    data class Tag(val content: TagInfo?) : OverviewResponse()
    data class KeyTakeaways(val content: List<String>) : OverviewResponse()
    data object Done : OverviewResponse()
    data class Error(val message: String) : OverviewResponse()
}