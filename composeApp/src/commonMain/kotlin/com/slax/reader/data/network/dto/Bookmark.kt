package com.slax.reader.data.network.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonClassDiscriminator

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
    val type: String,
    val id_token: String? = null
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
data class CreateIapOrderResult(
    val orderId: String
)

@Serializable
data class CheckIapParam(
    val jws_representation: String,
    val order_id: String,
    val product_id: String,
    val platform: String
)

@Serializable
data class CheckIapResult(
    val ok: Boolean
)

@Serializable
data class CreateIapOrderParam(
    val product_id: String,
    val platform: String
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

@Serializable
data class DeleteAccountData(
    val canDelete: Boolean = false,
    val reason: DeleteAccountReason? = null
)

@Serializable
data class ProductIdsResult(
    val products: List<String>
)

@Serializable
data class FeedbackParams(
    val bookmark_uuid: String?,
    val entry_point: String,
    val type: String,
    val content: String,
    val platform: String,
    val environment: String,
    val version: String,
    val allow_follow_up: Boolean,
    val target_url: String?
)

@Serializable
data class MarkDetail(
    val mark_list: List<MarkInfo> = emptyList(),
    val user_list: Map<String, MarkUserInfo> = emptyMap()
)

@Serializable
data class MarkInfo(
    val id: Long = 0,
    val user_id: Long = 0,
    val type: MarkType = MarkType.LINE,
    val source: List<MarkPathItem> = emptyList(),
    val approx_source: MarkPathApprox? = null,
    val parent_id: Long = 0,
    val root_id: Long = 0,
    val comment: String = "",
    val created_at: String = "",
    val is_deleted: Boolean = false,
    val children: List<MarkInfo> = emptyList()
)

@Serializable
data class MarkUserInfo(
    @SerialName("user_id") val id: Long = 0,
    val username: String = "",
    val avatar: String = ""
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
sealed class MarkPathItem {
    @Serializable
    @SerialName("text")
    data class Text(
        val path: String = "",
        val start: Int = 0,
        val end: Int = 0
    ) : MarkPathItem()

    @Serializable
    @SerialName("image")
    data class Image(
        val path: String = ""
    ) : MarkPathItem()
}

@Serializable
data class MarkPathApprox(
    val exact: String = "",
    val prefix: String = "",
    val suffix: String = "",
    val position_start: Int = 0,
    val position_end: Int = 0,
    val raw_text: String? = null
)

@Serializable(with = MarkTypeSerializer::class)
enum class MarkType(val value: Int) {
    LINE(1),
    COMMENT(2),
    REPLY(3),
    ORIGIN_LINE(4),
    ORIGIN_COMMENT(5)
}

object MarkTypeSerializer : KSerializer<MarkType> {
    override val descriptor = PrimitiveSerialDescriptor("MarkType", PrimitiveKind.INT)
    override fun serialize(encoder: Encoder, value: MarkType) = encoder.encodeInt(value.value)
    override fun deserialize(decoder: Decoder): MarkType {
        val v = decoder.decodeInt()
        return MarkType.entries.first { it.value == v }
    }
}

@Serializable
data class AddMarkParams(
    /** 书签 ID */
    val bookmark_uid: String,
    /** 划线/评论类型，复用 MarkType 枚举 */
    val type: MarkType,
    /** 划线路径，与 MarkInfo.source 保持一致 */
    val source: List<MarkPathItem> = emptyList(),
    /** 选中的内容片段 */
    val select_content: List<String> = emptyList(),
    /** 近似位置信息，用于在内容变更后仍能定位 */
    val approx_source: MarkPathApprox? = null,
    /** 评论文本，划线时可为空 */
    val comment: String = "",
    /** 回复目标的 Mark ID，仅 REPLY 类型使用 */
    val parent_id: Long? = null,
)

@Serializable
data class AddMarkResult(
    /** 新创建的 Mark ID */
    val mark_id: Long,
    /** 顶层 Mark ID，用于评论树定位 */
    val root_id: Long,
)

// ==================== JS Bridge strokeCurrentSelection 返回类型 ====================

/**
 * JS Bridge strokeCurrentSelection 返回的划线创建数据
 *
 * 包含本地渲染所需的 uuid 以及调用 /v1/mark/create 接口所需的全部字段。
 * 字段与 JS 侧 StrokeCreateData 接口一一对应。
 */
@Serializable
data class StrokeCreateData(
    /** 本地生成的 UUID，用于后续通过 updateMarkIdByUuid 关联后端 mark_id */
    val uuid: String,
    /** 后端接口 source 字段 */
    val source: List<StrokeCreateSource> = emptyList(),
    /** 后端接口 select_content 字段 */
    val select_content: List<StrokeCreateSelectContent> = emptyList(),
    /** 后端接口 approx_source 字段 */
    val approx_source: MarkPathApprox? = null,
)

/** 划线接口的 source 条目，字段映射：xpath → MarkPathItem.path */
@Serializable
data class StrokeCreateSource(
    val type: String = "text",
    /** CSS 选择器路径，对应后端接口字段 path */
    val xpath: String = "",
    /** 文本起始偏移（图片类型为 0） */
    val start_offset: Int = 0,
    /** 文本结束偏移（图片类型为 0） */
    val end_offset: Int = 0,
)

/** 划线接口的 select_content 条目 */
@Serializable
data class StrokeCreateSelectContent(
    val type: String = "text",
    /** 文本内容（图片类型为空字符串） */
    val text: String = "",
    /** 图片 src（文本类型为空字符串） */
    val src: String = "",
)