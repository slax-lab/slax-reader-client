package com.slax.reader.ui.inbox

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.slax.reader.testing.TestTags
import com.slax.reader.ui.inbox.compenents.AddLinkDialogContent
import com.slax.reader.ui.inbox.compenents.EmptyView
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

class InboxComponentsUiTest {
    @get:Rule
    val composeRule: ComposeContentTestRule = createComposeRule()

    @Test
    fun emptyView_is_present_in_the_real_semantics_tree() {
        composeRule.setContent { EmptyView() }

        composeRule.onNodeWithTag(TestTags.EmptyInbox)
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun addLinkDialog_rejects_invalid_url_and_accepts_http_url() {
        var submitted: String? = null
        composeRule.setContent {
            AddLinkDialogContent(
                onSubmit = { submitted = it },
                onDismissRequest = {}
            )
        }

        composeRule.onNodeWithTag(TestTags.AddLinkDialog)
            .assertExists()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.AddLinkInput)
            .assertExists()
            .performTextInput("not-a-url")
        composeRule.onNodeWithTag(TestTags.AddLinkConfirm)
            .assertHasClickAction()
            .performClick()

        composeRule.onNodeWithTag(TestTags.AddLinkError)
            .assertExists()
            .assertIsDisplayed()

        composeRule.onNodeWithTag(TestTags.AddLinkInput)
            .performTextReplacement("https://example.com/article")
        composeRule.onNodeWithTag(TestTags.AddLinkConfirm)
            .performClick()

        composeRule.runOnIdle {
            assertEquals("https://example.com/article", submitted)
        }
    }

    @Test
    fun addLinkDialog_close_button_invokes_dismiss_callback() {
        var dismissed = false
        composeRule.setContent {
            AddLinkDialogContent(
                onSubmit = {},
                onDismissRequest = { dismissed = true }
            )
        }

        composeRule.onNodeWithTag(TestTags.AddLinkClose)
            .assertExists()
            .assertHasClickAction()
            .performClick()
        composeRule.mainClock.advanceTimeBy(350)
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertEquals(true, dismissed)
        }
    }
}
