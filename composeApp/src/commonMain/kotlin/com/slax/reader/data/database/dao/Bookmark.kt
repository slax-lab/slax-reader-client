package com.slax.reader.data.database.dao

import com.powersync.PowerSyncDatabase
import com.powersync.db.getString
import com.slax.reader.data.database.model.BookmarkDetails
import com.slax.reader.data.database.model.BookmarkMetadata
import com.slax.reader.data.database.model.UserBookmark
import com.slax.reader.data.database.model.UserTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class BookmarkDao(
    private val database: PowerSyncDatabase
) {

    fun getUserBookmarkList(): Flow<List<UserBookmark>> {
        return database.watch(
            "SELECT is_read, archive_status, is_starred, created_at, updated_at, alias_title, type, deleted_at, metadata, id FROM sr_user_bookmark ORDER BY created_at DESC"
        ) { cursor ->
            UserBookmark(
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
                metadataObj = null,
                metadataTitle = null,
                metadata = cursor.getString("metadata"),
            )
        }.flowOn(Dispatchers.IO)
            .catch { e ->
                println("Error watching user bookmarks: ${e.message}")
            }
    }

    fun getUserTags(): Flow<Map<String, UserTag>> {
        return database.watch(
            "SELECT id, user_id, tag_name, display, created_at FROM sr_user_tag"
        ) { cursor ->
            UserTag(
                id = cursor.getString("id"),
                tag_name = cursor.getString("tag_name"),
                display = cursor.getString("display"),
                created_at = cursor.getString("created_at")
            )
        }.flowOn(Dispatchers.IO)
            .catch { e ->
                println("Error watching user tags: ${e.message}")
            }
            .map { tagsList ->
                tagsList.associateBy { it.id }
            }
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