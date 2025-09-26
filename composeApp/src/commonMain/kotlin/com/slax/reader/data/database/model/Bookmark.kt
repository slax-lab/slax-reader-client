package com.slax.reader.data.database.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Immutable
@Serializable
data class BookmarkMetadata(
    val tags: List<String>,
    val share: ShareSettings?,
    val bookmark: BookmarkDetails
)

@Immutable
@Serializable
data class ShareSettings(
    val uuid: String,
    val is_enable: Boolean,
    val show_line: Boolean,
    val allow_line: Boolean,
    val created_at: String,
    val share_code: String,
    val show_comment: Boolean,
    val allow_comment: Boolean,
    val show_userinfo: Boolean
)

@Immutable
@Serializable
data class BookmarkDetails(
    val uuid: String,
    val title: String,
    val byline: String,
    val status: String,
    val host_url: String,
    val site_name: String,
    val target_url: String,
    val description: String,
    val content_icon: String,
    val published_at: String,
    val content_cover: String,
    val content_word_count: Int
)

@Immutable
@Serializable
data class UserTag(
    val id: String,
    val tag_name: String,
    val display: String,
    val created_at: String
)

@Immutable
data class UserBookmark(
    val id: String,
    val isRead: Int,
    val archiveStatus: Int,
    val isStarred: Int,
    val createdAt: String,
    val updatedAt: String,
    val aliasTitle: String,
    val type: Int,
    val deletedAt: String?,
    val metadata: String?,

    var metadataObj: BookmarkMetadata?,
    var metadataTitle: String?
) {
    val displayTitle: String
        get() = aliasTitle.ifEmpty { displayTitle() }

    fun displayTitle(): String {
        return metadataObj?.bookmark?.title ?: id.take(5)
    }

    fun parsedMetadata(): BookmarkMetadata? {
        return metadataObj ?: getTypedMetadata()
    }

    fun getTypedMetadata(): BookmarkMetadata? {
        return metadata?.let { json ->
            try {
                val obj = Json.decodeFromString<BookmarkMetadata>(json)
                metadataObj = obj
                obj
            } catch (e: Exception) {
                println("Error parsing metadata: ${e.message}")
                null
            }
        }
    }
}