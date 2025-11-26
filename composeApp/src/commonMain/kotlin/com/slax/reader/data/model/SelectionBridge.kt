package com.slax.reader.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 选择数据
 */
@Serializable
data class SelectionData(
    val paths: List<MarkPathItem>,
    val approx: MarkPathApprox? = null,
    val selection: List<SelectionItem>
)

/**
 * 选择项（文本或图片）
 */
@Serializable
sealed class SelectionItem {
    @Serializable
    @SerialName("text")
    data class Text(
        val type: String = "text",
        val text: String,
        @SerialName("start_offset")
        val startOffset: Int,
        @SerialName("end_offset")
        val endOffset: Int
    ) : SelectionItem()

    @Serializable
    @SerialName("image")
    data class Image(
        val type: String = "image",
        val src: String
    ) : SelectionItem()
}

/**
 * 标记路径项
 */
@Serializable
data class MarkPathItem(
    val type: String,
    val path: String,
    val start: Int? = null,
    val end: Int? = null
)

/**
 * 标记路径近似信息
 */
@Serializable
data class MarkPathApprox(
    val exact: String,
    val prefix: String,
    val suffix: String,
    @SerialName("raw_text")
    val rawText: String? = null
)

/**
 * 位置信息
 */
@Serializable
data class PositionInfo(
    val x: Double,
    val y: Double,
    val width: Double? = null,
    val height: Double? = null,
    val top: Double? = null,
    val left: Double? = null,
    val right: Double? = null,
    val bottom: Double? = null
)

/**
 * 标记数据
 */
@Serializable
data class MarkData(
    val id: String,
    val text: String,
    val classList: List<String>
)

/**
 * 标记详情（从后端获取）
 */
@Serializable
data class MarkDetail(
    @SerialName("mark_list")
    val markList: List<BackendMarkInfo>,
    @SerialName("user_list")
    val userList: Map<String, MarkUserInfo>
)

/**
 * 后端标记信息
 */
@Serializable
data class BackendMarkInfo(
    val id: Int,
    @SerialName("user_id")
    val userId: Int,
    val type: Int, // 1=LINE, 2=COMMENT, 3=REPLY, 4=ORIGIN_LINE, 5=ORIGIN_COMMENT
    val source: List<MarkPathItem>,
    @SerialName("approx_source")
    val approxSource: MarkPathApprox? = null,
    @SerialName("parent_id")
    val parentId: Int,
    @SerialName("root_id")
    val rootId: Int,
    val comment: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("is_deleted")
    val isDeleted: Boolean
)

/**
 * 标记用户信息
 */
@Serializable
data class MarkUserInfo(
    @SerialName("user_id")
    val userId: Int,
    val username: String,
    val avatar: String
)
