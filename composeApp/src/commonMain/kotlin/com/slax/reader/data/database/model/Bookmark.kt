package com.slax.reader.data.database.model

import androidx.compose.runtime.Immutable
import com.powersync.db.SqlCursor
import com.powersync.db.getString
import com.slax.reader.utils.toDateTime
import com.slax.reader.utils.toISODateFormat
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
data class BookmarkCommentPO(
    val id: String,

    val userBookmarkUuid: String,
    val type: Int,
    val source: String,
    val comment: String,
    val approx_source: String,
    val content: String,
    val is_deleted: Int,
    val created_at: String,
    val metadataObj: BookmarkCommentMetadata?
)

@Immutable
@Serializable
data class BookmarkCommentMetadata(
    val root_id: String,
    val user_id: String,
    val parent_id: String?,
    val source_id: String,
    val bookmark_id: String
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
data class InboxListBookmarkItem(
    val id: String,
    val aliasTitle: String,
    val createdAt: String,
    val updatedAt: String,

    val archiveStatus: Int,
    val isStarred: Int,
    val metadataStatus: String?,
    val metadataTitle: String?,
    val metadataUrl: String?
) {
    fun displayTitle(): String {
        return when {
            aliasTitle.isNotEmpty() -> aliasTitle
            !metadataTitle.isNullOrEmpty() -> metadataTitle
            !metadataUrl.isNullOrEmpty() -> metadataUrl
            else -> id.take(5)
        }
    }
}

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

    var metadataTitle: String?,
    var metadataUrl: String?
) {
    val metadataObj: BookmarkMetadata? by lazy {
        metadata?.let { Json.decodeFromString<BookmarkMetadata>(it) }
    }

    val displayTitle: String by lazy {
        when {
            aliasTitle.isNotEmpty() -> aliasTitle
            !metadataTitle.isNullOrEmpty() -> metadataTitle!!
            !metadataUrl.isNullOrEmpty() -> metadataUrl!!
            else -> id.take(5)
        }
    }

    val displayTime: String
        get() = createdAt.toDateTime().toISODateFormat().take(16)
}


fun mapperToUserTag(cursor: SqlCursor): UserTag {
    return UserTag(
        id = cursor.getString("id"),
        tag_name = cursor.getString("tag_name"),
        display = cursor.getString("display"),
        created_at = cursor.getString("created_at"),
    )
}

fun mapperToBookmark(cursor: SqlCursor): UserBookmark {
    return UserBookmark(
        id = cursor.getString("id"),
        isRead = cursor.getString("is_read").toIntOrNull() ?: 0,
        archiveStatus = cursor.getString("archive_status").toIntOrNull() ?: 0,
        isStarred = cursor.getString("is_starred").toIntOrNull() ?: 0,
        createdAt = cursor.getString("created_at"),
        updatedAt = cursor.getString("updated_at"),
        aliasTitle = cursor.getString("alias_title"),
        type = cursor.getString("type").toIntOrNull() ?: 0,
        deletedAt = try {
            cursor.getString("deleted_at")
        } catch (_: Exception) {
            null
        },
        metadataTitle = cursor.getString("metadata_title"),
        metadataUrl = cursor.getString("metadata_url"),
        metadata = cursor.getString("metadata"),
    )
}

fun mapperToInboxListBookmarkItem(cursor: SqlCursor): InboxListBookmarkItem {
    return InboxListBookmarkItem(
        id = cursor.getString("id"),
        aliasTitle = cursor.getString("alias_title"),
        archiveStatus = cursor.getString("archive_status").toIntOrNull() ?: 0,
        isStarred = cursor.getString("is_starred").toIntOrNull() ?: 0,
        createdAt = cursor.getString("created_at"),
        updatedAt = cursor.getString("updated_at"),
        metadataTitle = cursor.getString("metadata_title"),
        metadataUrl = cursor.getString("metadata_url"),
        metadataStatus = cursor.getString("metadata_status"),
    )
}

fun mappingToBookmarkComment(cursor: SqlCursor): BookmarkCommentPO {
    return BookmarkCommentPO(
        id = cursor.getString("id"),
        userBookmarkUuid = cursor.getString("user_bookmark_uuid"),
        type = cursor.getString("type").toIntOrNull() ?: 0,
        source = cursor.getString("source"),
        comment = cursor.getString("comment"),
        approx_source = cursor.getString("approx_source"),
        content = cursor.getString("content"),
        is_deleted = cursor.getString("is_deleted").toIntOrNull() ?: 0,
        created_at = cursor.getString("created_at"),
        metadataObj = try {
            val metadataStr = cursor.getString("metadata")
            if (metadataStr.isNotEmpty()) {
                Json.decodeFromString<BookmarkCommentMetadata>(metadataStr)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    )
}