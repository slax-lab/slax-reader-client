package com.slax.reader.ui.inbox

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.slax.reader.testing.TestTags
import com.slax.reader.ui.inbox.compenents.EmptyOrLoadingView
import org.junit.Rule
import org.junit.Test

class EmptyOrLoadingUiTest {
    @get:Rule
    val rule: ComposeContentTestRule = createComposeRule()

    @Test
    fun unsynced_state_shows_real_loading_indicator() {
        rule.setContent { EmptyOrLoadingView(hasSynced = false) }
        rule.onNodeWithTag(TestTags.EmptyOrLoading).assertIsDisplayed()
        rule.onNodeWithTag(TestTags.EmptyInbox).assertIsDisplayed()
        rule.onNodeWithTag(TestTags.LoadingIndicator).assertIsDisplayed()
    }

    @Test
    fun synced_state_keeps_empty_view_but_hides_loading_indicator() {
        rule.setContent { EmptyOrLoadingView(hasSynced = true) }
        rule.onNodeWithTag(TestTags.EmptyOrLoading).assertIsDisplayed()
        rule.onNodeWithTag(TestTags.EmptyInbox).assertIsDisplayed()
        rule.onNodeWithTag(TestTags.LoadingIndicator).assertDoesNotExist()
    }
}
