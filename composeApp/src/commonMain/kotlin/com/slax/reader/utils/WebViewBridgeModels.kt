package com.slax.reader.utils

import com.slax.reader.data.network.dto.MarkPathApprox
import com.slax.reader.data.network.dto.MarkPathItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val bridgeJson = Json {
    ignoreUnknownKeys = true
}

@Serializable
data class BridgeMarkReplyInfo(
    val id: Long = 0,
    val username: String = "",
    val userId: Long = 0,
    val avatar: String = "",
)

@Serializable
data class BridgeMarkCommentInfo(
    val markId: Long = 0,
    val comment: String = "",
    val userId: Long = 0,
    val username: String = "",
    val avatar: String = "",
    val isDeleted: Boolean = false,
    val children: List<BridgeMarkCommentInfo> = emptyList(),
    val createdAt: String = "",
    val rootId: Long? = null,
    val reply: BridgeMarkReplyInfo? = null,
)

@Serializable
data class BridgeMarkStrokeInfo(
    @SerialName("mark_id") val markId: Long? = null,
    val userId: Long = 0,
)

@Serializable
data class BridgeMarkItemInfo(
    val id: String = "",
    val source: List<MarkPathItem> = emptyList(),
    val stroke: List<BridgeMarkStrokeInfo> = emptyList(),
    val comments: List<BridgeMarkCommentInfo> = emptyList(),
    val approx: MarkPathApprox? = null,
)
