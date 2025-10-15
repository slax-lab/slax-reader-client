package com.slax.reader.utils

import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun timeUnix(): Long {
    val currentInstant = kotlin.time.Clock.System.now()
    return currentInstant.toEpochMilliseconds()
}
