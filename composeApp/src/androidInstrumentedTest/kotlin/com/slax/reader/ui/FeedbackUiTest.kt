package com.slax.reader.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import com.slax.reader.testing.TestTags
import com.slax.reader.ui.feedback.FeedbackScreen
import com.slax.reader.data.network.FeedbackApi
import com.slax.reader.data.network.dto.FeedbackParams
import com.slax.reader.data.network.dto.HttpData
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import com.slax.reader.utils.i18n
import kotlinx.coroutines.CompletableDeferred

private class InstrumentedFeedbackApi : FeedbackApi {
    var calls = 0
    var lastParam: FeedbackParams? = null
    var error: Exception? = null
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun sendFeedback(param: FeedbackParams): HttpData<String> {
        calls++
        lastParam = param
        gate?.await()
        error?.let { throw it }
        return HttpData("", "", 200)
    }
}

class FeedbackUiTest {
    @get:Rule
    val rule: ComposeContentTestRule = createComposeRule()

    @Test
    fun feedback_input_and_submit_use_real_semantics_nodes_and_send_payload() {
        val api = InstrumentedFeedbackApi()
        rule.setContent {
            FeedbackScreen(
                title = "Cached article",
                href = "https://example.com/article",
                email = "reader@example.com",
                bookmarkId = "bookmark-1",
                entryPoint = "parse_error",
                api = api,
                onBackClick = {}
            )
        }

        rule.onNodeWithTag(TestTags.FeedbackScreen).assertIsDisplayed()
        rule.onNodeWithTag(TestTags.FeedbackInput)
            .assertIsDisplayed()
            .performTextInput("offline parse failed")
        rule.onNodeWithTag(TestTags.FeedbackSubmit)
            .assertIsDisplayed()
            .performClick()

        rule.waitForIdle()
        rule.onNodeWithText("feedback_success_title".i18n()).assertIsDisplayed()
        assertEquals(1, api.calls)
        assertEquals("offline parse failed", api.lastParam?.content)
        assertEquals("bookmark-1", api.lastParam?.bookmark_uuid)
        assertEquals("https://example.com/article", api.lastParam?.target_url)
    }

    @Test
    fun blank_input_is_disabled_and_request_content_is_trimmed() {
        val api = InstrumentedFeedbackApi()
        rule.setContent { FeedbackScreen(api = api, onBackClick = {}) }
        rule.onNodeWithTag(TestTags.FeedbackSubmit).assertIsNotEnabled()
        rule.onNodeWithTag(TestTags.FeedbackInput).performTextInput("   \n  ")
        rule.onNodeWithTag(TestTags.FeedbackSubmit).assertIsNotEnabled()
        rule.runOnIdle { assertEquals(0, api.calls) }
        rule.onNodeWithTag(TestTags.FeedbackInput).performTextReplacement("  actual report  ")
        rule.onNodeWithTag(TestTags.FeedbackSubmit).assertIsEnabled().performClick()
        rule.onNodeWithText("feedback_success_title".i18n()).assertIsDisplayed()
        rule.runOnIdle { assertEquals("actual report", api.lastParam?.content) }
    }

    @Test
    fun offline_error_keeps_input_and_allows_explicit_retry() {
        val api = InstrumentedFeedbackApi().apply { error = IllegalStateException("offline") }
        var backs = 0
        rule.setContent { FeedbackScreen(api = api, onBackClick = { backs++ }) }
        rule.onNodeWithTag(TestTags.FeedbackInput).performTextInput("retained report")
        rule.onNodeWithTag(TestTags.FeedbackSubmit).performClick()
        rule.onNodeWithText("feedback_error_title".i18n()).assertIsDisplayed()
        rule.onNodeWithText("offline", substring = true).assertIsDisplayed()
        rule.onNodeWithText("btn_ok".i18n()).performClick()
        rule.onNodeWithTag(TestTags.FeedbackInput).assertTextEquals("retained report")
        rule.runOnIdle { assertEquals(0, backs); api.error = null }
        rule.onNodeWithTag(TestTags.FeedbackSubmit).assertIsEnabled().performClick()
        rule.onNodeWithText("feedback_success_title".i18n()).assertIsDisplayed()
        rule.onNodeWithText("btn_ok".i18n()).performClick()
        rule.runOnIdle { assertEquals(2, api.calls); assertEquals(1, backs) }
    }

    @Test
    fun pending_submission_disables_duplicate_submit_until_result_arrives() {
        val completion = CompletableDeferred<Unit>()
        val api = InstrumentedFeedbackApi().apply { gate = completion }
        rule.setContent { FeedbackScreen(api = api, onBackClick = {}) }
        rule.onNodeWithTag(TestTags.FeedbackInput).performTextInput("pending")
        rule.onNodeWithTag(TestTags.FeedbackSubmit).performClick()
        rule.onNodeWithTag(TestTags.FeedbackSubmit).assertIsNotEnabled()
        rule.onNodeWithText("feedback_submitting".i18n()).assertIsDisplayed()
        rule.runOnIdle { assertEquals(1, api.calls); completion.complete(Unit) }
        rule.onNodeWithText("feedback_success_title".i18n()).assertIsDisplayed()
    }
}
