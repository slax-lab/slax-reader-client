package com.slax.reader.data.database.dao

import com.powersync.PowerSyncDatabase
import com.powersync.db.getStringOptional
import com.slax.reader.data.database.model.LocalBookmarkInfo
import com.slax.reader.data.database.model.mapperToLocalBookmarkInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json

class LocalBookmarkDao(
    private val database: PowerSyncDatabase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _userLocalBookmarkListFlow: StateFlow<Map<String, LocalBookmarkInfo>> by lazy {
        println("[watch][database] _userLocalBookmarkListFlow")
        database.watch(
            """
            SELECT id, is_downloaded
            FROM local_bookmark_info
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

    suspend fun updateLocalBookmarkDownloadStatus(bookmarkId: String, downloadStatus: Int) =
        withContext(Dispatchers.IO) {
            println("[LocalBookmarkDao] Updating download status: bookmarkId=$bookmarkId, downloadStatus=$downloadStatus")
            database.writeTransaction { tx ->
                tx.execute(
                    """
                INSERT INTO ps_data_local__local_bookmark_info (id, data)
                VALUES (?, json_object('is_downloaded', ?))
                ON CONFLICT(id) DO UPDATE SET
                data = json_set(
                    data, '$.is_downloaded', json_extract(excluded.data, '$.is_downloaded')
                );
            """.trimIndent(),
                    parameters = listOf(bookmarkId, downloadStatus)
                )
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
}