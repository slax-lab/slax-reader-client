package com.slax.reader.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.slax.reader.ui.about.AboutScreen
import com.slax.reader.ui.login.LoginButton
import com.slax.reader.testing.TestTags
import com.slax.reader.ui.setting.CacheCountStepper
import com.slax.reader.ui.setting.SettingScreen
import com.slax.reader.ui.setting.SettingViewModel
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.plusAssign
import androidx.navigation.NavHostController
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.slax.reader.data.network.AccountApi
import com.slax.reader.data.network.dto.DeleteAccountData
import com.slax.reader.data.network.dto.HttpData
import com.slax.reader.data.preferences.AuthTokenPreferences
import com.slax.reader.data.preferences.SettingsPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import com.slax.reader.utils.i18n
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class AccountAndSettingsUiTest {
    @get:Rule
    val rule: ComposeContentTestRule = createComposeRule()

    private class SettingsPrefs : SettingsPreferences, AuthTokenPreferences {
        val count = MutableStateFlow(50)
        val images = MutableStateFlow(true)
        override fun getCacheCount() = count.asStateFlow()
        override suspend fun setCacheCount(value: Int) { count.value = value }
        override fun getDownloadImages() = images.asStateFlow()
        override suspend fun setDownloadImages(value: Boolean) { images.value = value }
        override suspend fun getAuthInfoSuspend(): String? = "test"
    }

    private class Account : AccountApi {
        override suspend fun deleteAccount(): HttpData<DeleteAccountData> =
            HttpData(DeleteAccountData(true), "", 200)
    }

    @Test
    fun login_button_disables_during_loading_and_is_clickable_when_idle() {
        var clicks = 0
        rule.setContent {
            var loading by remember { mutableStateOf(false) }
            LoginButton(text = "Continue", isLoading = loading, onClick = {
                clicks++
                loading = true
            })
        }
        rule.onNodeWithText("Continue").assertIsDisplayed()
        rule.onNodeWithTag(TestTags.LoginButton).performClick()
        rule.runOnIdle { assertEquals(1, clicks) }
        rule.onNodeWithTag(TestTags.LoginButton).assertIsNotEnabled()
        rule.onNodeWithTag(TestTags.LoginLoadingIndicator).assertIsDisplayed()
    }

    @Test
    fun about_screen_back_and_hidden_debug_gesture_are_real_interactions() {
        var back = false
        var debug = false
        rule.setContent {
            AboutScreen(
                onBackClick = { back = true },
                onDebugClick = { debug = true }
            )
        }
        rule.onNodeWithContentDescription("btn_back".i18n()).performClick()
        rule.runOnIdle { assertTrue(back) }

        val version = "about_version".i18n()
        rule.onNodeWithText(version, substring = true)
            .assertIsDisplayed()
            .performClick()
            .performClick()
            .performClick()
            .performClick()
            .performClick()
        rule.runOnIdle { assertTrue(debug) }
    }

    @Test
    fun cache_count_stepper_reports_selected_value_and_all_choices() {
        var selected = 50
        rule.setContent {
            CacheCountStepper(value = selected, onValueChange = { selected = it })
        }
        rule.onNodeWithText("50").assertIsDisplayed()
        rule.onNodeWithText("100").performClick()
        rule.runOnIdle { assertEquals(100, selected) }
        rule.onNodeWithText("∞").performClick()
        rule.runOnIdle { assertEquals(-1, selected) }
    }

    @Test
    fun setting_screen_uses_injected_viewmodel_and_real_clicks() {
        val preferences = SettingsPrefs()
        val viewModel = SettingViewModel(Account(), preferences)
        val navController = NavHostController(ApplicationProvider.getApplicationContext<Context>())
        navController.navigatorProvider += ComposeNavigator()

        rule.setContent {
            SettingScreen(
                onBackClick = {},
                navController = navController,
                viewModel = viewModel
            )
        }

        rule.onNodeWithTag(TestTags.SettingScreen).assertIsDisplayed()
        rule.onNodeWithText("100").performClick()
        rule.onNodeWithText("∞").performClick()
        rule.onNodeWithText("setting_download_images".i18n()).performClick()
        rule.waitForIdle()

        assertEquals(-1, preferences.count.value)
        assertEquals(false, preferences.images.value)
    }
}
