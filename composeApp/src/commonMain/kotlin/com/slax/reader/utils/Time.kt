package com.slax.reader.utils

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.format.optional
import kotlinx.datetime.isoDayNumber
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

@OptIn(ExperimentalTime::class)
fun String.toDateTime(): LocalDateTime {
    val normalized = this
        .replace(" ", "T")
        .let {
            val hasTimezone = it.endsWith("Z") ||
                    it.substring(10).contains("+") ||
                    it.substring(10).contains("-")
            if (hasTimezone) it else "${it}Z"
        }
    val instant = Instant.parse(normalized)

    return instant.toLocalDateTime(TimeZone.currentSystemDefault())
}

val isoDateFormat = LocalDateTime.Format {
    date(LocalDate.Formats.ISO)
    char(' ')
    hour(); char(':'); minute()
    optional {
        char(':'); second()
        optional {
            char('.'); secondFraction(minLength = 3)
        }
    }
}

fun LocalDateTime.toISODateFormat(): String {
    return this.format(isoDateFormat)
}

/**
 * 将 ISO 日期字符串转换为时间分组标签。
 * 分组规则：今天 / 昨天 / 本周 / 上周 / 本月 / 更早
 */
@OptIn(ExperimentalTime::class)
fun String.toTimeGroup(): String {
    val dateTime = this.toDateTime()
    val nowInstant = kotlin.time.Clock.System.now()
    val nowKtx = kotlinx.datetime.Instant.fromEpochMilliseconds(nowInstant.toEpochMilliseconds())
    val now = nowKtx.toLocalDateTime(TimeZone.currentSystemDefault())
    val today = now.date
    val itemDate = dateTime.date

    if (itemDate == today) return "今天"
    if (itemDate == today.minus(1, DateTimeUnit.DAY)) return "昨天"

    // 计算本周一 (ISO: 周一=1)
    val thisMonday = today.minus(today.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
    if (itemDate >= thisMonday) return "本周"

    // 计算上周一
    val lastMonday = thisMonday.minus(7, DateTimeUnit.DAY)
    if (itemDate >= lastMonday) return "上周"

    // 本月
    val firstDayOfMonth = LocalDate(today.year, today.month, 1)
    if (itemDate >= firstDayOfMonth) return "本月"

    return "更早"
}
