package com.slax.reader.ui.inbox

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slax.reader.domain.sync.DownloadStatus
import com.slax.reader.testing.TestTags
import com.slax.reader.ui.inbox.compenents.ItemDownloadState
import com.slax.reader.ui.inbox.compenents.ItemStatus
import com.slax.reader.ui.inbox.compenents.DownloadStateKey
import com.slax.reader.data.database.model.BookmarkSortType
import androidx.compose.ui.semantics.getOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class BookmarkItemStatusInstrumentedTest {
    @get:Rule
    val composeTestRule: ComposeContentTestRule = createComposeRule()

    @Test
    fun failedDownloadIsExposedAsFailedUiState() {
        composeTestRule.setContent {
            ItemStatus(downloadStatus = DownloadStatus.FAILED.code)
        }

        val state = readDownloadState(composeTestRule)

        assertEquals(ItemDownloadState.FAILED.name, state)
        assertNotEquals(ItemDownloadState.COMPLETED.name, state)
    }

    @Test
    fun completedDownloadIsExposedAsCompletedUiState() {
        composeTestRule.setContent {
            ItemStatus(downloadStatus = DownloadStatus.COMPLETED.code)
        }

        assertEquals(ItemDownloadState.COMPLETED.name, readDownloadState(composeTestRule))
    }

    @Test
    fun every_download_status_has_a_real_semantics_state() {
        var currentStatus by mutableStateOf(DownloadStatus.NONE.code)
        composeTestRule.setContent {
            ItemStatus(downloadStatus = currentStatus)
        }
        mapOf(
            DownloadStatus.NONE.code to ItemDownloadState.NONE,
            DownloadStatus.DOWNLOADING.code to ItemDownloadState.DOWNLOADING,
            DownloadStatus.COMPLETED.code to ItemDownloadState.COMPLETED,
            DownloadStatus.FAILED.code to ItemDownloadState.FAILED,
            999 to ItemDownloadState.NONE
        ).forEach { (nextStatus, expected) ->
            composeTestRule.runOnIdle { currentStatus = nextStatus }
            composeTestRule.waitForIdle()
            assertEquals(expected.name, readDownloadState(composeTestRule))
        }
    }

    @Test
    fun starred_and_archived_sort_modes_are_exposed_as_real_nodes() {
        composeTestRule.setContent {
            ItemStatus(
                downloadStatus = DownloadStatus.FAILED.code,
                sortType = BookmarkSortType.STARRED
            )
        }
        composeTestRule.onNodeWithTag(TestTags.BookmarkItemStatus).assertExists()
    }

    private fun readDownloadState(rule: ComposeContentTestRule): String {
        val node = rule
            .onNodeWithTag(TestTags.BookmarkItemStatus, useUnmergedTree = true)
            .assertExists()
            .fetchSemanticsNode()
        return node.config.getOrNull(DownloadStateKey)
            ?: error("ItemStatus did not expose DownloadState in the semantics tree")
    }
}
