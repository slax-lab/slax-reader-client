package com.slax.reader

import com.slax.reader.data.database.model.LocalBookmarkInfo
import com.slax.reader.data.database.model.isDownloaded
import com.slax.reader.domain.coordinator.hasNetworkConnection
import com.slax.reader.domain.sync.DownloadStatus
import com.slax.reader.domain.sync.shouldFetchBookmarkContent
import com.slax.reader.ui.inbox.compenents.ItemDownloadState
import com.slax.reader.ui.inbox.compenents.itemDownloadState
import dev.jordond.connectivity.Connectivity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ComposeAppCommonTest {

    @Test
    fun downloadStatusCodesMatchPersistedContract() {
        assertEquals(0, DownloadStatus.NONE.code)
        assertEquals(1, DownloadStatus.DOWNLOADING.code)
        assertEquals(2, DownloadStatus.COMPLETED.code)
        assertEquals(3, DownloadStatus.FAILED.code)
    }

    @Test
    fun onlyCompletedStatusIsDownloaded() {
        assertFalse(LocalBookmarkInfo("id", null, null, DownloadStatus.NONE.code).isDownloaded())
        assertFalse(LocalBookmarkInfo("id", null, null, DownloadStatus.DOWNLOADING.code).isDownloaded())
        assertTrue(LocalBookmarkInfo("id", null, null, DownloadStatus.COMPLETED.code).isDownloaded())
        assertFalse(LocalBookmarkInfo("id", null, null, DownloadStatus.FAILED.code).isDownloaded())
        assertFalse(LocalBookmarkInfo("id", null, null, 99).isDownloaded())
    }

    @Test
    fun failedAndUnknownStatusesAreNotRenderedAsCompleted() {
        assertEquals(ItemDownloadState.NONE, itemDownloadState(DownloadStatus.NONE.code))
        assertEquals(ItemDownloadState.DOWNLOADING, itemDownloadState(DownloadStatus.DOWNLOADING.code))
        assertEquals(ItemDownloadState.COMPLETED, itemDownloadState(DownloadStatus.COMPLETED.code))
        assertEquals(ItemDownloadState.FAILED, itemDownloadState(DownloadStatus.FAILED.code))
        assertEquals(ItemDownloadState.NONE, itemDownloadState(99))
    }

    @Test
    fun networkIsNeededOnlyWhenContentIsMissing() {
        assertFalse(shouldFetchBookmarkContent(hasCachedContent = true, networkAvailable = false))
        assertFalse(shouldFetchBookmarkContent(hasCachedContent = true, networkAvailable = true))
        assertFalse(shouldFetchBookmarkContent(hasCachedContent = false, networkAvailable = false))
        assertTrue(shouldFetchBookmarkContent(hasCachedContent = false, networkAvailable = true))
    }

    @Test
    fun onlyConnectedConnectivityStatesAllowNetworkRequests() {
        assertTrue(hasNetworkConnection(Connectivity.Status.Connected(metered = false)))
        assertTrue(hasNetworkConnection(Connectivity.Status.Connected(metered = true)))
        assertFalse(hasNetworkConnection(Connectivity.Status.Disconnected))
    }
}
