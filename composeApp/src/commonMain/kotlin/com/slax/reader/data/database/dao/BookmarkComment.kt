package com.slax.reader.data.database.dao

import com.powersync.PowerSyncDatabase
import com.slax.reader.data.database.model.BookmarkCommentPO
import com.slax.reader.data.database.model.mappingToBookmarkComment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn

class BookmarkCommentDao(
    private val database: PowerSyncDatabase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun watchComments(bookmarkId: String): StateFlow<List<BookmarkCommentPO>> {
        println("[watch][database] watchComments for bookmarkId: $bookmarkId")
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
        }.distinctUntilChanged().stateIn(scope, SharingStarted.WhileSubscribed(5000), initialValue = emptyList())
    }

    fun addComment(bookmarkId: String) {
        TODO()
    }

    fun deleteComment(bookmarkId: String, commentId : String) {
        TODO()
    }

    fun replyComment(bookmarkId: String, commentId : String) {
        TODO()
    }
}