package com.slax.reader.data.database.dao

import com.powersync.PowerSyncDatabase
import com.powersync.db.SqlCursor
import com.powersync.db.getString
import com.slax.reader.data.database.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class BookmarkDao(
    private val database: PowerSyncDatabase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun mapperToUserTag(cursor: SqlCursor): UserTag {
        return UserTag(
            id = cursor.getString("id"),
            tag_name = cursor.getString("tag_name"),
            display = cursor.getString("display"),
            created_at = cursor.getString("created_at"),
        )
    }

    private fun mapperToBookmark(cursor: SqlCursor): UserBookmark {
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

    private fun mapperToInboxListBookmarkItem(cursor: SqlCursor): InboxListBookmarkItem {
        return InboxListBookmarkItem(
            id = cursor.getString("id"),
            aliasTitle = cursor.getString("alias_title"),
            createdAt = cursor.getString("created_at"),
            metadataTitle = cursor.getString("metadata_title"),
            metadataUrl = cursor.getString("metadata_url"),
        )
    }

    private val _userBookmarkListFlow: StateFlow<List<InboxListBookmarkItem>> by lazy {
        database.watch(
            """
            SELECT
                id,
                created_at,
                alias_title,
                JSON_EXTRACT(metadata, '$.bookmark.title') as metadata_title,
                JSON_EXTRACT(metadata, '$.bookmark.target_url') as metadata_url
            FROM sr_user_bookmark
            ORDER BY created_at DESC
            """.trimIndent()
        ) { cursor ->
            mapperToInboxListBookmarkItem(cursor)
        }.catch { e ->
            println("Error watching user bookmarks: ${e.message}")
        }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun watchUserBookmarkList(): StateFlow<List<InboxListBookmarkItem>> = _userBookmarkListFlow

    fun watchBookmarkDetail(bookmarkId: String): Flow<List<UserBookmark>> {
        println("[data] watch bookmark detail")
        return database.watch(
            """
            SELECT
                id,
                is_read,
                archive_status,
                is_starred,
                created_at,
                updated_at,
                alias_title,
                type,
                deleted_at,
                metadata,
                JSON_EXTRACT(metadata, '$.bookmark.title') as metadata_title,
                JSON_EXTRACT(metadata, '$.bookmark.target_url') as metadata_url
            FROM sr_user_bookmark WHERE id = ?
            """.trimIndent(), listOf(bookmarkId)
        ) { cursor ->
            mapperToBookmark(cursor)
        }.catch { e ->
            println("Error watching user bookmarks: ${e.message}")
        }
            .distinctUntilChanged()
    }

    fun watchUserTag(): Flow<List<UserTag>> {
        println("[data] watch user tag =======")
        return database.watch(
            """
            SELECT * FROM sr_user_tag
        """.trimIndent(), parameters = listOf(), mapper = { cursor ->
                mapperToUserTag(cursor)
            }
        )
    }

    suspend fun getTagsByIds(tagIds: List<String>): List<UserTag> {
        println("[data] get tags by ids")
        if (tagIds.isEmpty()) return emptyList()

        val placeholders = tagIds.joinToString(",") { "?" }

        return database.getAll(
            """
            SELECT * FROM sr_user_tag WHERE id IN ($placeholders)
        """.trimIndent(),
            parameters = tagIds,
            mapper = { cursor ->
                UserTag(
                    id = cursor.getString("id"),
                    tag_name = cursor.getString("tag_name"),
                    display = cursor.getString("display"),
                    created_at = cursor.getString("created_at"),
                )
            }
        )
    }

    suspend fun updateMetadataField(
        bookmarkId: String,
        fieldPath: String,
        jsonValue: String
    ) {
        database.writeTransaction { tx ->
            tx.execute(
                "UPDATE sr_user_bookmark SET metadata = JSON_SET(COALESCE(metadata, '{}'), '$.$fieldPath', JSON(?)) WHERE id = ?",
                listOf(jsonValue, bookmarkId)
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun deleteBookmark(bookmarkId: String) {
        val now = Clock.System.now().toString()
        database.writeTransaction { tx ->
            tx.execute(
                "UPDATE sr_user_bookmark SET deleted_at = ? WHERE id = ?",
                listOf(now, bookmarkId)
            )
        }
    }

    suspend fun updateBookmarkArchive(bookmarkId: String, state: Int) {
        database.writeTransaction { tx ->
            tx.execute(
                "UPDATE sr_user_bookmark SET archive_status = ? WHERE id = ?",
                listOf(state, bookmarkId)
            )
        }
    }

    suspend fun updateBookmarkStar(bookmarkId: String, state: Int) {
        database.writeTransaction { tx ->
            tx.execute(
                "UPDATE sr_user_bookmark SET is_starred = ? WHERE id = ?",
                listOf(state, bookmarkId)
            )
        }
    }

    @OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
    suspend fun createBookmark(url: String) {
        val bookmarkId = Uuid.random().toString()
        val now = Clock.System.now().toString()

        database.writeTransaction { tx ->
            tx.execute(
                """INSERT INTO sr_user_bookmark
                                (id, is_read, archive_status, is_starred, created_at, updated_at,
                                 alias_title, type, deleted_at, metadata)
                                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                listOf(
                    bookmarkId,
                    0, // is_read
                    0, // archive_status
                    0, // is_starred
                    now, // created_at
                    now, // updated_at
                    "", // alias_title
                    0, // type
                    null, // deleted_at
                    Json.encodeToString(
                        BookmarkMetadata(
                            tags = emptyList(),
                            share = null,
                            bookmark = BookmarkDetails(
                                uuid = bookmarkId,
                                title = "New Bookmark",
                                byline = "",
                                status = "pending",
                                host_url = url,
                                site_name = "",
                                target_url = url,
                                description = "",
                                content_icon = "",
                                published_at = now,
                                content_cover = "",
                                content_word_count = 0
                            )
                        )
                    )
                )
            )
        }
    }
}