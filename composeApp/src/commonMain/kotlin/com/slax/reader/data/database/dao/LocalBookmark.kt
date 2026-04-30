package com.slax.reader.data.database.dao

import com.powersync.PowerSyncDatabase
import com.powersync.db.getStringOptional
import com.slax.reader.data.database.model.LocalBookmarkInfo
import com.slax.reader.data.database.model.mapperToLocalBookmarkInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json

class LocalBookmarkDao(
    private val scope: CoroutineScope,
    private val database: PowerSyncDatabase
) {
    private val _userLocalBookmarkListFlow: StateFlow<Map<String, LocalBookmarkInfo>> by lazy {
        println("[watch][database] _userLocalBookmarkListFlow")
        database.watch(
            """
            SELECT id,
                COALESCE(JSON_EXTRACT(data, '$.is_downloaded'), 0) as is_downloaded,
                JSON_EXTRACT(data, '$.is_auto_cached') as is_auto_cached
            FROM ps_data_local__local_bookmark_info
        """.trimIndent(),
            mapper = {
                mapperToLocalBookmarkInfo(it)
            }
        ).catch { e ->
            println("Error watching user bookmarks: ${e.message}")
        }
            .distinctUntilChanged()
            .map { bookmarkList ->
                bookmarkList.associateBy { it.id }
            }
            .stateIn(scope, SharingStarted.Eagerly, emptyMap())
    }

    fun watchUserLocalBookmarkMap(): StateFlow<Map<String, LocalBookmarkInfo>> = _userLocalBookmarkListFlow

    private suspend fun upsertFields(
        bookmarkId: String,
        fields: Map<String, Any?>
    ) = withContext(Dispatchers.IO) {
        val d = "$"
        val jsonKeys = fields.keys.joinToString(", ") { "'$it', ?" }
        val jsonSetParts = fields.keys.joinToString(", ") {
            "'${d}.$it', json_extract(excluded.data, '${d}.$it')"
        }
        database.writeTransaction { tx ->
            tx.execute(
                """
                INSERT INTO ps_data_local__local_bookmark_info (id, data)
                VALUES (?, json_object($jsonKeys))
                ON CONFLICT(id) DO UPDATE SET
                data = json_set(data, $jsonSetParts);
            """.trimIndent(),
                parameters = listOf(bookmarkId) + fields.values.toList()
            )
        }
    }

    private suspend fun getField(
        bookmarkId: String,
        field: String
    ): String? {
        val d = "$"
        return database.getOptional(
            """
            SELECT json_extract(data, '${d}.$field') AS value
            FROM ps_data_local__local_bookmark_info
            WHERE id = ?
            """.trimIndent(),
            parameters = listOf(bookmarkId),
            mapper = { cursor ->
                cursor.getStringOptional("value")?.takeIf { it.isNotEmpty() } ?: ""
            }
        )?.takeIf { it.isNotEmpty() }
    }

    suspend fun updateLocalBookmarkDownloadStatus(
        bookmarkId: String,
        downloadStatus: Int,
        isAutoCached: Boolean
    ) = upsertFields(bookmarkId, mapOf("is_downloaded" to downloadStatus, "is_auto_cached" to if (isAutoCached) 1 else 0))

    suspend fun batchResetDownloadStatus(
        bookmarkIds: List<String>,
        downloadStatus: Int = 0,
        isAutoCached: Boolean = false
    ) = withContext(Dispatchers.IO) {
        database.writeTransaction { tx ->
            val d = "$"
            val jsonKeys = "'is_downloaded', ?, 'is_auto_cached', ?"
            val jsonSetParts = "'${d}.is_downloaded', json_extract(excluded.data, '${d}.is_downloaded'), '${d}.is_auto_cached', json_extract(excluded.data, '${d}.is_auto_cached')"
            bookmarkIds.forEach { bookmarkId ->
                tx.execute(
                    """
                    INSERT INTO ps_data_local__local_bookmark_info (id, data)
                    VALUES (?, json_object($jsonKeys))
                    ON CONFLICT(id) DO UPDATE SET
                    data = json_set(data, $jsonSetParts);
                """.trimIndent(),
                    parameters = listOf(bookmarkId, downloadStatus, if (isAutoCached) 1 else 0)
                )
            }
        }
    }

    suspend fun updateLocalBookmarkOverview(
        bookmarkId: String,
        overview: String,
        keyTakeaways: String?
    ) = upsertFields(bookmarkId, mapOf("overview" to overview, "key_takeaways" to keyTakeaways))

    suspend fun getLocalBookmarkOverview(bookmarkId: String): Pair<String?, List<String>?> {
        val d = "$"
        val result = database.getOptional(
            """
            SELECT
                json_extract(data, '${d}.overview') AS overview,
                json_extract(data, '${d}.key_takeaways') AS key_takeaways
            FROM ps_data_local__local_bookmark_info
            WHERE id = ?
            """.trimIndent(),
            parameters = listOf(bookmarkId),
            mapper = { cursor ->
                val overview = cursor.getStringOptional("overview").let { if (it.isNullOrEmpty()) null else it }
                val keyTakeaways = cursor.getStringOptional("key_takeaways").let {
                    if (it.isNullOrEmpty()) {
                        null
                    } else {
                        try {
                            Json.decodeFromString<List<String>>(it)
                        } catch (e: Exception) {
                            println("Failed to deserialize keyTakeaways: ${e.message}")
                            null
                        }
                    }
                }
                Pair(overview, keyTakeaways)
            }
        ) ?: Pair(null, null)
        return result
    }

    suspend fun updateLocalBookmarkReadPosition(
        bookmarkId: String,
        readPosition: Float
    ) = upsertFields(bookmarkId, mapOf("read_position" to readPosition.toString()))

    suspend fun getLocalBookmarkReadPosition(bookmarkId: String): Float? =
        getField(bookmarkId, "read_position")?.toFloatOrNull()

    suspend fun updateLocalBookmarkOutline(
        bookmarkId: String,
        outline: String,
    ) = upsertFields(bookmarkId, mapOf("outline" to outline))

    suspend fun getLocalBookmarkOutline(bookmarkId: String): String? =
        getField(bookmarkId, "outline")

    suspend fun updateLocalBookmarkOutlineScrollPosition(
        bookmarkId: String,
        scrollPosition: Int
    ) = upsertFields(bookmarkId, mapOf("outline_read_position" to scrollPosition.toString()))

    suspend fun getLocalBookmarkOutlineScrollPosition(bookmarkId: String): Int? =
        getField(bookmarkId, "outline_read_position")?.toIntOrNull()

    suspend fun updateMarkUsers(bookmarkId: String, markUsersJson: String) =
        upsertFields(bookmarkId, mapOf("mark_users" to markUsersJson))

    suspend fun getMarkUsers(bookmarkId: String): String? =
        getField(bookmarkId, "mark_users")
}