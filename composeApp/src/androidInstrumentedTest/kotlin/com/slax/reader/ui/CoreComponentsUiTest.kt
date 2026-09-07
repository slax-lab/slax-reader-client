package com.slax.reader.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slax.reader.domain.coordinator.AppSyncState
import com.slax.reader.testing.TestTags
import com.slax.reader.ui.bookmark.components.PageIndicator
import com.slax.reader.ui.bookmark.components.SelectionAction
import com.slax.reader.ui.bookmark.components.SelectionActionBar
import com.slax.reader.ui.sidebar.compenents.SyncStatusBar
import org.junit.Rule
import org.junit.Test
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_menu_action_copy
import org.junit.Assert.assertEquals

class CoreComponentsUiTest {
    @get:Rule
    val rule: ComposeContentTestRule = createComposeRule()

    @Test
    fun sync_status_bar_exposes_no_network_state_in_real_semantics_tree() {
        rule.setContent { SyncStatusBar(AppSyncState.NoNetwork) }

        rule.onNodeWithTag(TestTags.SyncStatusBar)
            .assertIsDisplayed()
        rule.onNodeWithTag(TestTags.SyncStatusText)
            .assertIsDisplayed()
    }

    @Test
    fun sync_status_bar_renders_all_non_connected_states_without_crashing() {
        var state by mutableStateOf<AppSyncState>(AppSyncState.Connecting)
        rule.setContent { SyncStatusBar(state) }
        listOf(
            AppSyncState.Connecting,
            AppSyncState.Uploading,
            AppSyncState.Downloading(0f),
            AppSyncState.Downloading(1f),
            AppSyncState.Error("sync error"),
            AppSyncState.NoNetwork
        ).forEach { nextState ->
            rule.runOnIdle { state = nextState }
            rule.onNodeWithTag(TestTags.SyncStatusBar)
                .assertIsDisplayed()
            rule.waitForIdle()
        }
    }

    @Test
    fun sync_status_bar_renders_download_progress_percentage() {
        rule.setContent { SyncStatusBar(AppSyncState.Downloading(0.42f)) }

        rule.onNodeWithTag(TestTags.SyncStatusBar).assertIsDisplayed()
        rule.onNodeWithText("42%").assertIsDisplayed()
        rule.onNodeWithTag(TestTags.SyncProgress).assertIsDisplayed()
    }

    @Test
    fun page_indicator_renders_one_real_node_per_page_and_active_page_is_present() {
        rule.setContent { PageIndicator(pageCount = 3, currentPage = 1) }

        rule.onNodeWithTag(TestTags.PageIndicator).assertIsDisplayed()
        assertEquals(
            1,
            rule.onAllNodesWithTag("${TestTags.PageIndicatorItemPrefix}0")
                .fetchSemanticsNodes().size
        )
        rule.onNodeWithTag("${TestTags.PageIndicatorItemPrefix}1").assertIsDisplayed()
        rule.onNodeWithTag("${TestTags.PageIndicatorItemPrefix}2").assertIsDisplayed()
    }

    @Test
    fun page_indicator_is_absent_for_single_page() {
        rule.setContent { PageIndicator(pageCount = 1, currentPage = 0) }
        rule.onNodeWithTag(TestTags.PageIndicator).assertDoesNotExist()
    }

    @Test
    fun selection_action_bar_displays_actions_and_reports_real_click() {
        var clicked: String? = null
        val action = SelectionAction(
            id = "copy",
            label = "Copy",
            iconRes = Res.drawable.ic_menu_action_copy
        )
        rule.setContent {
            SelectionActionBar(
                visible = true,
                actions = listOf(action),
                onActionClick = { clicked = it }
            )
        }

        rule.onNodeWithTag(TestTags.SelectionActionBar)
            .assertIsDisplayed()
        rule.onNodeWithText("Copy")
            .performClick()
        rule.runOnIdle { assertEquals("copy", clicked) }
    }
}
