package com.slax.reader.data.network.dto

import kotlinx.serialization.SerialName
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
data class UploadChangesResult(
    val status: Boolean = true
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
data class BookmarkOutlineItem(
    val content: String,
    val updated_at: String,
    val is_self: Boolean = false
)


@Serializable
data class BookmarkOutlinesResult(
    val data: List<BookmarkOutlineItem> = emptyList(),
)

@Serializable
data class BookmarkOutlineParam(
    val bookmark_uid: String,
    val force: Boolean = false
)
@Serializable
data class TagInfo(
    val id: Long,
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
data class OverviewEventData(
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

@Serializable
data class OutlineDataWrapper(
    val outline: String? = null,
    val done: Boolean? = null
)

@Serializable
data class OutlineEventData(
    val type: String,
    val data: OutlineDataWrapper? = null,
    val message: String? = null
)
sealed class OutlineResponse {
    data class Outline(val content: String) : OutlineResponse()
    data object Done : OutlineResponse()
    data class Error(val message: String) : OutlineResponse()
}

/**
 * 删除账号失败的原因枚举
 */
@Serializable
enum class DeleteAccountReason {
    /** 有活跃的订阅 */
    @SerialName("active_subscription")
    ACTIVE_SUBSCRIPTION,

    /** 存在 Stripe Connect 连接 */
    @SerialName("stripe_connect_exists")
    STRIPE_CONNECT_EXISTS,

    /** 集合有订阅者 */
    @SerialName("collection_has_subscribers")
    COLLECTION_HAS_SUBSCRIBERS,

    /** 有活跃的集合订阅 */
    @SerialName("active_collection_subscription")
    ACTIVE_COLLECTION_SUBSCRIPTION,

    /** 有免费订阅历史 */
    @SerialName("has_free_subscription_history")
    HAS_FREE_SUBSCRIPTION_HISTORY
}

/**
 * 删除账号响应数据
 * @param canDelete 是否可以删除账号
 * @param reason 不能删除的原因（仅在 canDelete=false 时返回）
 */
@Serializable
data class DeleteAccountData(
    val canDelete: Boolean = false,
    val reason: DeleteAccountReason? = null
)