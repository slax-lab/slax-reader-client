package com.slax.reader.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class AppLogTest {
    @Test
    fun redactsCredentialsFromLogMessages() {
        val message = redactLogMessage(
            "Authorization: Bearer secret.jwt.value token=abc123 Cookie: session=sensitive\n" +
                "https://example.com/path?secret=value"
        )

        assertFalse("secret.jwt.value" in message)
        assertFalse("abc123" in message)
        assertFalse("session=sensitive" in message)
        assertFalse("secret=value" in message)
        assertEquals(
            "Authorization: <redacted> token=<redacted> Cookie: <redacted>\n" +
                "https://example.com/path?<redacted>",
            message,
        )
    }

    @Test
    @OptIn(ExperimentalTime::class)
    fun expiresOnlyLogFilesOlderThanSevenDays() {
        val now = Instant.parse("2026-08-18T15:00:00Z").toEpochMilliseconds()
        val sevenDays = 7L * 24 * 60 * 60 * 1_000

        assertTrue(isExpiredLogFile("slax-app-2026-08-11.log", now, now))
        assertFalse(isExpiredLogFile("slax-app-2026-08-12.log", now, now))
        assertTrue(isExpiredLogFile("slax-app.log", now - sevenDays - 1, now))
        assertFalse(isExpiredLogFile("slax-app.log", now - sevenDays, now))
        assertFalse(isExpiredLogFile("slax-share.log", null, now))
    }
}
