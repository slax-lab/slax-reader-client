package com.slax.reader

import com.slax.reader.data.database.model.InboxListBookmarkItem
import com.slax.reader.domain.sync.DownloadStatus
import com.slax.reader.domain.sync.shouldFetchBookmarkContent
import com.slax.reader.data.database.model.isDownloaded
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Application-level offline contract tests.
 *
 * PowerSync owns upload compensation; these tests deliberately assert only
 * the client-side policy: cached content wins offline and a missing cache is
 * fetched only when the caller reports connectivity.
 */
class OfflinePolicyTest {
    @Test
    fun cached_content_is_used_without_network() {
        assertFalse(shouldFetchBookmarkContent(hasCachedContent = true, networkAvailable = false))
        assertFalse(shouldFetchBookmarkContent(hasCachedContent = true, networkAvailable = true))
    }

    @Test
    fun missing_content_is_only_fetchable_when_online() {
        assertFalse(shouldFetchBookmarkContent(hasCachedContent = false, networkAvailable = false))
        assertTrue(shouldFetchBookmarkContent(hasCachedContent = false, networkAvailable = true))
    }

    @Test
    fun failed_download_is_not_treated_as_completed() {
        val failed = com.slax.reader.data.database.model.LocalBookmarkInfo(
            id = "id",
            overview = null,
            keyTakeaways = null,
            downloadStatus = DownloadStatus.FAILED.code,
            isAutoCached = false
        )
        assertFalse(failed.isDownloaded())
        assertEquals(DownloadStatus.FAILED.code, failed.downloadStatus)
    }

    @Test
    fun next_processing_attempt_is_allowed_by_policy_after_failed_status() {
        val failed = com.slax.reader.data.database.model.LocalBookmarkInfo(
            "id", null, null, DownloadStatus.FAILED.code, false
        )
        assertTrue(shouldFetchBookmarkContent(failed.isDownloaded(), networkAvailable = true))
    }

    @Test
    fun bookmark_model_remains_stable_when_local_status_is_absent() {
        val item = InboxListBookmarkItem(
            "id", "", "", 0, 0, "success", "Title", "https://example.com"
        )
        assertEquals(DownloadStatus.NONE.code, item.downloadStatus)
        assertFalse(item.isAutoCached)
    }
}
