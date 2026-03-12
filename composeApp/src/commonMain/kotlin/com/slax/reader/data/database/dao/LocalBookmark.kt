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

    suspend fun updateLocalBookmarkDownloadStatus(
        bookmarkId: String,
        downloadStatus: Int,
        isAutoCached: Boolean
    ) = withContext(Dispatchers.IO) {
        database.writeTransaction { tx ->
            tx.execute(
                """
                INSERT INTO ps_data_local__local_bookmark_info (id, data)
                VALUES (?, json_object('is_downloaded', ?, 'is_auto_cached', ?))
                ON CONFLICT(id) DO UPDATE SET
                data = json_set(
                    data,
                    '$.is_downloaded', json_extract(excluded.data, '$.is_downloaded'),
                    '$.is_auto_cached', json_extract(excluded.data, '$.is_auto_cached')
                );
            """.trimIndent(),
                parameters = listOf(bookmarkId, downloadStatus, if (isAutoCached) 1 else 0)
            )
        }
    }

    suspend fun batchResetDownloadStatus(
        bookmarkIds: List<String>,
        downloadStatus: Int = 0,
        isAutoCached: Boolean = false
    ) = withContext(Dispatchers.IO) {
        database.writeTransaction { tx ->
            bookmarkIds.forEach { bookmarkId ->
                tx.execute(
                    """
                    INSERT INTO ps_data_local__local_bookmark_info (id, data)
                    VALUES (?, json_object('is_downloaded', ?, 'is_auto_cached', ?))
                    ON CONFLICT(id) DO UPDATE SET
                    data = json_set(
                        data,
                        '$.is_downloaded', json_extract(excluded.data, '$.is_downloaded'),
                        '$.is_auto_cached', json_extract(excluded.data, '$.is_auto_cached')
                    );
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
    ) = withContext(Dispatchers.IO) {
        database.writeTransaction { tx ->
            tx.execute(
                """
                INSERT INTO ps_data_local__local_bookmark_info (id, data)
                VALUES (?, json_object('overview', ?, 'key_takeaways', ?))
                ON CONFLICT(id) DO UPDATE SET
                data = json_set(
                    ps_data_local__local_bookmark_info.data,
                    '$.overview', json_extract(excluded.data, '$.overview'),
                    '$.key_takeaways', json_extract(excluded.data, '$.key_takeaways')
                );
            """.trimIndent(),
                parameters = listOf(bookmarkId, overview, keyTakeaways)
            )
        }
    }

    suspend fun getLocalBookmarkOverview(bookmarkId: String): Pair<String?, List<String>?> {
        val result = database.getOptional(
            """
            SELECT
                json_extract(data, '$.overview') AS overview,
                json_extract(data, '$.key_takeaways') AS key_takeaways
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
    ) = withContext(Dispatchers.IO) {
        database.writeTransaction { tx ->
            tx.execute(
                """
                INSERT INTO ps_data_local__local_bookmark_info (id, data)
                VALUES (?, json_object('read_position', ?))
                ON CONFLICT(id) DO UPDATE SET
                data = json_set(
                    ps_data_local__local_bookmark_info.data,
                    '$.read_position', json_extract(excluded.data, '$.read_position')
                );
            """.trimIndent(),
                parameters = listOf(bookmarkId, readPosition.toString())
            )
        }
    }

    suspend fun getLocalBookmarkReadPosition(bookmarkId: String): Float? {
        val raw = database.getOptional(
            """
            SELECT
                json_extract(data, '$.read_position') AS read_position
            FROM ps_data_local__local_bookmark_info
            WHERE id = ?
            """.trimIndent(),
            parameters = listOf(bookmarkId),
            mapper = { cursor ->
                cursor.getStringOptional("read_position") ?: ""
            }
        )
        return raw?.takeIf { it.isNotEmpty() }?.toFloatOrNull()
    }

    suspend fun updateLocalBookmarkOutline(
        bookmarkId: String,
        outline: String,
    ) = withContext(Dispatchers.IO) {
        database.writeTransaction { tx ->
            tx.execute(
                """
                INSERT INTO ps_data_local__local_bookmark_info (id, data)
                VALUES (?, json_object('outline', ?))
                ON CONFLICT(id) DO UPDATE SET
                data = json_set(
                    ps_data_local__local_bookmark_info.data,
                    '$.outline', json_extract(excluded.data, '$.outline')
                );
            """.trimIndent(),
                parameters = listOf(bookmarkId, outline)
            )
        }
    }

    suspend fun getLocalBookmarkOutline(bookmarkId: String): String? {
        return database.getOptional(
            """
            SELECT
                json_extract(data, '$.outline') AS outline
            FROM ps_data_local__local_bookmark_info
            WHERE id = ?
            """.trimIndent(),
            parameters = listOf(bookmarkId),
            mapper = { cursor ->
                cursor.getStringOptional("outline")?.takeIf { it.isNotEmpty() } ?: ""
            }
        )?.takeIf { it.isNotEmpty() }
    }
}