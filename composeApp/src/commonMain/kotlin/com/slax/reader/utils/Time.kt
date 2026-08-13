package com.slax.reader.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.alternativeParsing
import kotlinx.datetime.format.char
import kotlinx.datetime.format.optional
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalTime::class)
fun timeUnix(): Long {
    val currentInstant = kotlin.time.Clock.System.now()
    return currentInstant.toEpochMilliseconds()
}

private val OffsetTimestampFormat = DateTimeComponents.Format {
    date(LocalDate.Formats.ISO)
    // 解析时接受 'T' / 't' / ' ' 三种分隔符，格式化时统一输出 'T'
    alternativeParsing({ char('t') }, { char(' ') }) { char('T') }
    hour(); char(':'); minute()
    optional { char(':'); second() }
    optional { char('.'); secondFraction(minLength = 1, maxLength = 9) }
    // UtcOffset.Formats.ISO 已覆盖 'Z' / 'z' / '+08:00' / '+08:00:00'
    offset(UtcOffset.Formats.ISO)
}

private val LocalTimestampFormat = LocalDateTime.Format {
    date(LocalDate.Formats.ISO)
    alternativeParsing({ char('t') }, { char(' ') }) { char('T') }
    hour(); char(':'); minute()
    optional { char(':'); second() }
    optional { char('.'); secondFraction(minLength = 1, maxLength = 9) }
}

@OptIn(ExperimentalTime::class)
fun parseInstantOrNull(dateString: String): Instant? {
    if (dateString.isBlank()) return null

    // 先走标准 ISO 快路径
    Instant.parseOrNull(dateString)?.let { return it }

    OffsetTimestampFormat.parseOrNull(dateString)?.let { components ->
        runCatching { components.toInstantUsingOffset() }.getOrNull()?.let { return it }
    }

    // 无时区：按 UTC 解释
    return LocalTimestampFormat.parseOrNull(dateString)?.toInstant(UtcOffset.ZERO)
}

@OptIn(ExperimentalTime::class)
fun parseInstant(dateString: String): Instant =
    parseInstantOrNull(dateString) ?: throw IllegalArgumentException("Unparseable timestamp: $dateString")

@OptIn(ExperimentalTime::class)
fun String.toDateTime(): LocalDateTime =
    parseInstant(this).toLocalDateTime(TimeZone.currentSystemDefault())

/** 解析失败返回 null，供 UI 层「无法解析就原样展示」的场景使用。 */
@OptIn(ExperimentalTime::class)
fun String.toDateTimeOrNull(): LocalDateTime? =
    parseInstantOrNull(this)?.toLocalDateTime(TimeZone.currentSystemDefault())

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
