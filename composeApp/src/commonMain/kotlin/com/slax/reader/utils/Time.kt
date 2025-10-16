package com.slax.reader.utils

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
fun timeUnix(): Long {
    val currentInstant = kotlin.time.Clock.System.now()
    return currentInstant.toEpochMilliseconds()
}

@OptIn(ExperimentalTime::class)
fun now(): Instant = kotlin.time.Clock.System.now()

@OptIn(ExperimentalTime::class)
fun parser(unixTime: Long = 0, strTime: String = ""): Instant {
    if (unixTime > 0) {
        return Instant.fromEpochMilliseconds(unixTime)
    }
    if (strTime != "") {
        return Instant.parse(strTime)
    }
    throw Exception("Invalid time input")
}

@OptIn(ExperimentalTime::class)
fun isBefore(aTime: Instant, bTime: Instant): Boolean {
    return aTime < bTime
}