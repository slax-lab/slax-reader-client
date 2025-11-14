package com.slax.reader.data.database.model

import androidx.compose.runtime.Immutable
import com.powersync.db.SqlCursor
import com.powersync.db.getString

/**
 * 本地书签信息模型，用于缓存 Overview 内容和下载状态
 * 对应数据库表 local_bookmark_info
 */
@Immutable
data class LocalBookmarkInfo(
    /** 书签 ID（关联 sr_user_bookmark.id） */
    val id: String,

    /** 概要内容缓存 */
    val overview: String?,

    /** 关键要点缓存 */
    val keyTakeaways: String?,

    /** 下载状态：0=下载中，1=已完成，2=失败 */
    val downloadStatus: Int
) {
    companion object {
        const val STATUS_DOWNLOADING = 0
        const val STATUS_COMPLETED = 1
        const val STATUS_FAILED = 2
    }
}

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