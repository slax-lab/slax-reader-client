package com.slax.reader.data.database.dao

import com.powersync.PowerSyncDatabase
import com.slax.reader.data.database.model.BookmarkCommentMetadata
import com.slax.reader.data.database.model.BookmarkCommentPO
import com.slax.reader.data.database.model.mappingToBookmarkComment
import com.slax.reader.data.network.dto.MarkPathApprox
import com.slax.reader.data.network.dto.MarkPathItem
import com.slax.reader.data.network.dto.MarkType
import com.slax.reader.data.network.dto.StrokeCreateSelectContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class BookmarkCommentDao(
    private val database: PowerSyncDatabase
) {
    companion object {
        private val json = Json { encodeDefaults = true }
    }

    fun watchComments(bookmarkId: String): Flow<List<BookmarkCommentPO>> {
        return database.watch(
            """
            SELECT  id,
                    user_bookmark_uuid,
                    type,
                    source,
                    comment,
                    approx_source,
                    content,
                    is_deleted,
                    created_at,
                    metadata
            FROM sr_bookmark_comment
            WHERE user_bookmark_uuid = ?
        """.trimIndent(), listOf(bookmarkId)
        ) { cursor ->
            mappingToBookmarkComment(cursor)
        }.distinctUntilChanged()
    }

    @OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
    suspend fun addMark(
        bookmarkId: String,
        userId: String,
        type: MarkType,
        source: List<MarkPathItem> = emptyList(),
        approxSource: MarkPathApprox? = null,
        selectContent: List<StrokeCreateSelectContent> = emptyList(),
        comment: String = "",
        rootId: String? = null,
        parentId: String? = null,
    ): String {
        val id = Uuid.random().toString()
        val now = Clock.System.now().toString()
        val metadata = BookmarkCommentMetadata(
            root_id = rootId,
            user_id = userId,
            parent_id = parentId,
            source_id = null,
            bookmark_id = null
        )

        database.writeTransaction { tx ->
            tx.execute(
                """INSERT INTO sr_bookmark_comment
                    (id, type, source, user_bookmark_uuid, comment, approx_source, content, is_deleted, created_at, metadata)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                listOf(
                    id,
                    type.value,
                    if (source.isNotEmpty()) json.encodeToString(source) else "",
                    bookmarkId,
                    comment,
                    approxSource?.let { json.encodeToString(it) } ?: "",
                    json.encodeToString(selectContent),
                    0,
                    now,
                    json.encodeToString(metadata)
                )
            )
        }
        return id
    }

    suspend fun deleteComment(commentId: String) {
        database.writeTransaction { tx ->
            tx.execute(
                "UPDATE sr_bookmark_comment SET is_deleted = 1 WHERE id = ?",
                listOf(commentId)
            )
        }
    }
}
