package com.slax.reader.data.database.model

import androidx.compose.runtime.Immutable
import com.powersync.db.SqlCursor
import com.powersync.db.getString

@Immutable
data class LocalBookmarkInfo(
    val id: String,
    val overview: String?,
    val keyTakeaways: String?,
    val downloadStatus: Int,
    val isAutoCached: Boolean = true
)

fun LocalBookmarkInfo.isDownloaded() : Boolean {
    return downloadStatus == 2
}

fun mapperToLocalBookmarkInfo(cursor: SqlCursor): LocalBookmarkInfo {
    return LocalBookmarkInfo(
        id = cursor.getString("id"),
        overview = try {
            cursor.getString("overview")
        } catch (_: Exception) {
            null
        },
        keyTakeaways = try {
            cursor.getString("key_takeaways")
        } catch (_: Exception) {
            null
        },
        downloadStatus = cursor.getString("is_downloaded").toIntOrNull() ?: 0,
        isAutoCached = try {
            cursor.getString("is_auto_cached").toIntOrNull() == 1
        } catch (_: Exception) {
            true
        }
    )
}