package com.slax.reader.ui

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.slax.reader.testing.TestTags
import com.slax.reader.ui.bookmark.components.BookmarkAlertDialog
import com.slax.reader.ui.bookmark.components.DetailScreenSkeleton
import com.slax.reader.ui.bookmark.components.TagItem
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BookmarkComponentsUiTest {
    @get:Rule
    val rule: ComposeContentTestRule = createComposeRule()

    @Test
    fun detail_skeleton_is_a_real_visible_ui_state() {
        rule.setContent { DetailScreenSkeleton() }
        rule.onNodeWithTag(TestTags.DetailSkeleton).assertIsDisplayed()
    }

    @Test
    fun tag_without_delete_is_clickable_and_reports_click() {
        var clicked = false
        rule.setContent {
            TagItem(tag = "offline", onClick = { clicked = true })
        }
        rule.onNodeWithTag(TestTags.TagItem)
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
        rule.runOnIdle { assertTrue(clicked) }
    }

    @Test
    fun tag_with_delete_exposes_text_and_delete_action() {
        var deleted = false
        rule.setContent {
            TagItem(
                tag = "to-delete",
                onClick = {},
                showDeleteButton = true,
                onDelete = { deleted = true }
            )
        }
        rule.onNodeWithTag(TestTags.TagItem).assertIsDisplayed()
        rule.onNodeWithText("to-delete").assertIsDisplayed()
        rule.onNodeWithContentDescription("删除标签")
            .assertHasClickAction()
            .performClick()
        rule.runOnIdle { assertTrue(deleted) }
    }

    @Test
    fun bookmark_alert_dialog_shows_error_content_in_real_ui_tree() {
        rule.setContent {
            BookmarkAlertDialog(
                errText = "offline",
                backClickHandle = {}
            )
        }
        rule.onNodeWithTag(TestTags.BookmarkAlertDialog).assertIsDisplayed()
        rule.onNodeWithText("offline").assertIsDisplayed()
    }
}
