package com.slax.reader.ui.inbox

import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.slax.reader.domain.sync.DownloadStatus
import com.slax.reader.ui.inbox.compenents.ItemDownloadState
import com.slax.reader.ui.inbox.compenents.ItemStatus
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

    private fun readDownloadState(rule: ComposeContentTestRule): String {
        val key = Class
            .forName("com.slax.reader.ui.inbox.compenents.BookmarkItemStatusKt")
            .getDeclaredField("DownloadStateKey")
            .apply { isAccessible = true }
            .get(null) as SemanticsPropertyKey<String>

        val node = rule
            .onNodeWithContentDescription("Article", useUnmergedTree = true)
            .assertExists()
            .fetchSemanticsNode()

        return node.config.getOrNull(key)
            ?: error("ItemStatus did not expose DownloadState in the semantics tree")
    }
}
