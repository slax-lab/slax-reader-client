package com.slax.reader.data.database.dao

import com.powersync.PowerSyncDatabase
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

    private val _userBookmarkListFlow: StateFlow<List<InboxListBookmarkItem>> by lazy {
        println("[database][watch] _userBookmarkListFlow")
        database.watch(
            """
            SELECT
                id,
                archive_status,
                is_starred,
                created_at,
                updated_at,
                alias_title,
                JSON_EXTRACT(metadata, '$.bookmark.title') as metadata_title,
                JSON_EXTRACT(metadata, '$.bookmark.target_url') as metadata_url,
                JSON_EXTRACT(metadata, '$.bookmark.status') as metadata_status
            FROM sr_user_bookmark WHERE archive_status = 0 AND deleted_at IS NULL
            ORDER BY created_at DESC
            """.trimIndent()
        ) { cursor ->
            mapperToInboxListBookmarkItem(cursor)
        }.catch { e ->
            println("Error watching user bookmarks: ${e.message}")
        }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.Eagerly, emptyList())
    }

    fun watchUserBookmarkList(): StateFlow<List<InboxListBookmarkItem>> = _userBookmarkListFlow

    fun watchBookmarkDetail(bookmarkId: String): Flow<List<UserBookmark>> {
        println("[database][watch] watchBookmarkDetail")
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

    private val _userTagListFlow: StateFlow<List<UserTag>> by lazy {
        println("[database][watch] _userTagListFlow")
        database.watch(
            """
            SELECT * FROM sr_user_tag
        """.trimIndent(), parameters = listOf(), mapper = { cursor ->
                mapperToUserTag(cursor)
            }
        )
            .distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, emptyList())
    }

    fun watchUserTag(): Flow<List<UserTag>> = _userTagListFlow

    suspend fun getTagsByIds(tagIds: List<String>): List<UserTag> {
        println("[database] getTagsByIds === ")

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

    suspend fun updateBookmarkAliasTitle(bookmarkId: String, title: String) {
        database.writeTransaction { tx ->
            tx.execute(
                "UPDATE sr_user_bookmark SET alias_title = ? WHERE id = ?",
                listOf(title, bookmarkId)
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

    @OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
    suspend fun createTag(tagName: String): UserTag {
        val tagId = Uuid.random().toString()
        val now = Clock.System.now().toString()

        database.writeTransaction { tx ->
            tx.execute(
                """INSERT INTO sr_user_tag (id, tag_name, display, created_at)
                   VALUES (?, ?, ?, ?)""",
                listOf(tagId, tagName, tagName, now)
            )
        }

        return UserTag(
            id = tagId,
            tag_name = tagName,
            display = tagName,
            created_at = now
        )
    }

    // ========== 本地书签信息操作（用于 Overview 缓存和下载状态管理） ==========

    /**
     * 获取本地书签信息
     * @param bookmarkId 书签 ID
     * @return LocalBookmarkInfo 或 null（如果不存在）
     */
    suspend fun getLocalBookmarkInfo(bookmarkId: String): LocalBookmarkInfo? {
        return database.getOptional(
            """
            SELECT id, overview, key_takeaways, is_downloaded
            FROM local_bookmark_info
            WHERE id = ?
            """.trimIndent(),
            parameters = listOf(bookmarkId),
            mapper = { cursor ->
                mapperToLocalBookmarkInfo(cursor)
            }
        )
    }

    /**
     * 保存或更新 Overview 缓存
     * @param bookmarkId 书签 ID
     * @param overview 概要内容
     * @param keyTakeaways 关键要点
     */
    suspend fun saveOverviewCache(bookmarkId: String, overview: String, keyTakeaways: String? = null) {
        database.writeTransaction { tx ->
            // 先查询记录是否存在
            val existingId: String? = tx.getOptional(
                "SELECT id FROM local_bookmark_info WHERE id = ?",
                listOf(bookmarkId),
                mapper = { cursor -> cursor.getString(0) ?: "" }
            )

            if (existingId != null && existingId.isNotEmpty()) {
                // 记录存在，执行更新
                tx.execute(
                    "UPDATE local_bookmark_info SET overview = ?, key_takeaways = ? WHERE id = ?",
                    listOf(overview, keyTakeaways, bookmarkId)
                )
            } else {
                // 记录不存在，执行插入
                tx.execute(
                    "INSERT INTO local_bookmark_info (id, overview, key_takeaways, is_downloaded) VALUES (?, ?, ?, 0)",
                    listOf(bookmarkId, overview, keyTakeaways)
                )
            }
        }
    }

    /**
     * 更新下载状态
     * @param bookmarkId 书签 ID
     * @param status 下载状态：0=下载中，1=已完成，2=失败
     */
    suspend fun updateDownloadStatus(bookmarkId: String, status: Int) {
        database.writeTransaction { tx ->
            // 先查询记录是否存在
            val existingId: String? = tx.getOptional(
                "SELECT id FROM local_bookmark_info WHERE id = ?",
                listOf(bookmarkId),
                mapper = { cursor -> cursor.getString(0) ?: "" }
            )

            if (existingId != null && existingId.isNotEmpty()) {
                // 记录存在，执行更新
                tx.execute(
                    "UPDATE local_bookmark_info SET is_downloaded = ? WHERE id = ?",
                    listOf(status, bookmarkId)
                )
            } else {
                // 记录不存在，执行插入
                tx.execute(
                    "INSERT INTO local_bookmark_info (id, overview, key_takeaways, is_downloaded) VALUES (?, NULL, NULL, ?)",
                    listOf(bookmarkId, status)
                )
            }
        }
    }

    /**
     * 获取所有已下载的书签 ID
     * @return 已完成下载的书签 ID 列表
     */
    suspend fun getAllDownloadedBookmarkIds(): List<String> {
        return database.getAll(
            """
            SELECT id
            FROM local_bookmark_info
            WHERE is_downloaded = ?
            """.trimIndent(),
            parameters = listOf(LocalBookmarkInfo.STATUS_COMPLETED),
            mapper = { cursor ->
                cursor.getString("id")
            }
        )
    }

    /**
     * 批量更新下载状态
     * @param statusMap 书签 ID 到状态的映射
     */
    suspend fun updateDownloadStatusBatch(statusMap: Map<String, Int>) {
        if (statusMap.isEmpty()) return

        database.writeTransaction { tx ->
            statusMap.forEach { (bookmarkId, status) ->
                // 先查询记录是否存在
                val existingId: String? = tx.getOptional(
                    "SELECT id FROM local_bookmark_info WHERE id = ?",
                    listOf(bookmarkId),
                    mapper = { cursor -> cursor.getString(0) ?: "" }
                )

                if (existingId != null && existingId.isNotEmpty()) {
                    // 记录存在，执行更新
                    tx.execute(
                        "UPDATE local_bookmark_info SET is_downloaded = ? WHERE id = ?",
                        listOf(status, bookmarkId)
                    )
                } else {
                    // 记录不存在，执行插入
                    tx.execute(
                        "INSERT INTO local_bookmark_info (id, overview, key_takeaways, is_downloaded) VALUES (?, NULL, NULL, ?)",
                        listOf(bookmarkId, status)
                    )
                }
            }
        }
    }
}