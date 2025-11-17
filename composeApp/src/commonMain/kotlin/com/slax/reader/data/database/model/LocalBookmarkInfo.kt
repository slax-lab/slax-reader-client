package com.slax.reader.data.database.model

import androidx.compose.runtime.Immutable
import com.powersync.db.SqlCursor
import com.powersync.db.getString

@Immutable
data class LocalBookmarkInfo(
    /** 书签 ID（关联 sr_user_bookmark.id） */
    val id: String,

    /** 概要内容缓存 */
    val overview: String?,

    /** 关键要点缓存 */
    val keyTakeaways: String?,

    /** 下载状态：0=NONE, 1=下载中，2=已完成，3=失败 */
    val downloadStatus: Int
)

/**
 * 将 SqlCursor 映射为 LocalBookmarkInfo 对象
 */
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
        downloadStatus = cursor.getString("is_downloaded").toIntOrNull() ?: 0
    )
}