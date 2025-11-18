package com.slax.reader.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.format.optional
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
