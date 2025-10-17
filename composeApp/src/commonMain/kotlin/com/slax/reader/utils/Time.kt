package com.slax.reader.utils

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

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
