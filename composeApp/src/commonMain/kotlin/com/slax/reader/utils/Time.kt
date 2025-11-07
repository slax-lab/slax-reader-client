package com.slax.reader.utils

import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalTime::class)
fun timeUnix(): Long {
    val currentInstant = kotlin.time.Clock.System.now()
    return currentInstant.toEpochMilliseconds()
}

@OptIn(ExperimentalTime::class)
fun parseInstant(dateString: String): Instant {
    return try {
        Instant.parse(dateString)
    } catch (e: Exception) {
        val normalized = dateString
            .replace(" ", "T")  // 空格替换为 T
            .let {
                // 如果没有时区信息，添加 Z (UTC)
                // 检查是否已有时区：以 Z 结尾，或在日期部分后有 + 或 -
                val hasTimezone = it.endsWith("Z") ||
                        it.substring(10).contains("+") ||
                        it.substring(10).contains("-")
                if (hasTimezone) {
                    it
                } else {
                    "${it}Z"
                }
            }
        Instant.parse(normalized)
    }
}

/**
 * 将 UTC 时间字符串格式化为本地时区的显示时间
 * @param dateString UTC 时间字符串，格式如：2025-11-05 03:51:02.825
 * @return 本地时区的时间字符串，格式如：2025-11-05 11:51
 */
@OptIn(ExperimentalTime::class)
fun formatDisplayTime(dateString: String): String {
    return try {
        // 解析为 kotlinx.datetime.Instant
        val normalized = dateString
            .replace(" ", "T")
            .let {
                val hasTimezone = it.endsWith("Z") ||
                        it.substring(10).contains("+") ||
                        it.substring(10).contains("-")
                if (hasTimezone) it else "${it}Z"
            }
        val instant = Instant.parse(normalized)

        // 转换到本地时区
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

        // 格式化为 YYYY-MM-DD HH:mm
        val hour = localDateTime.hour.toString().padStart(2, '0')
        val minute = localDateTime.minute.toString().padStart(2, '0')
        "${localDateTime.date} $hour:$minute"
    } catch (_: Exception) {
        // 降级处理：解析失败时返回原始格式
        dateString.take(16)
    }
}
