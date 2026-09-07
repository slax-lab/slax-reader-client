package com.slax.reader.ui

import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import com.slax.reader.const.*
import com.slax.reader.data.database.dao.SubscriptionRepository
import com.slax.reader.data.database.dao.UserRepository
import com.slax.reader.data.database.model.UserInfo
import com.slax.reader.data.database.model.UserSubscriptionInfo
import com.slax.reader.domain.coordinator.AppSyncState
import com.slax.reader.domain.coordinator.NetworkCoordinator
import com.slax.reader.ui.sidebar.SidebarViewModel
import com.slax.reader.ui.sidebar.compenents.FooterMenu
import com.slax.reader.utils.i18n
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SidebarUiTest {
    @get:Rule val rule: ComposeContentTestRule = createComposeRule()
    private val user = MutableStateFlow<UserInfo?>(null)
    private var dismissals = 0
    private var signOuts = 0
    private var navigatedRoute: FeedbackRoutes? = null

    private fun show() {
        val viewModel = SidebarViewModel(
            object : UserRepository { override fun watchUserInfo() = user },
            object : NetworkCoordinator {
                override val syncState = MutableStateFlow<AppSyncState>(AppSyncState.NoNetwork)
                override suspend fun isNetworkAvailable() = false
            },
            object : SubscriptionRepository {
                override fun watchSubscriptionInfo() = MutableStateFlow<UserSubscriptionInfo?>(null)
            }
        )
        rule.setContent {
            val nav = rememberNavController()
            NavHost(nav, startDestination = InboxRoutes) {
                composable<InboxRoutes> {
                    Column {
                        FooterMenu(nav, onDismiss = { dismissals++ }, viewModel = viewModel, onSignOut = { signOuts++ })
                    }
                }
                composable<SettingsRoutes> { Text("settings-destination") }
                composable<AboutRoutes> { Text("about-destination") }
                composable<FeedbackRoutes> { entry ->
                    val route = entry.toRoute<FeedbackRoutes>()
                    androidx.compose.runtime.SideEffect { navigatedRoute = route }
                    Column {
                        Text("feedback-destination")
                        Text(route.email.orEmpty(), Modifier.testTag("feedback-route-email"))
                    }
                }
            }
        }
    }

    @Test
    fun settings_navigation_is_available_offline_before_user_loaded() {
        show()
        rule.onNodeWithText("sidebar_settings".i18n()).performClick()
        rule.onNodeWithText("settings-destination").assertIsDisplayed()
        rule.runOnIdle { assertEquals(1, dismissals) }
    }

    @Test
    fun about_navigation_is_available_offline_before_user_loaded() {
        show()
        rule.onNodeWithText("sidebar_about".i18n()).performClick()
        rule.onNodeWithText("about-destination").assertIsDisplayed()
    }

    @Test
    fun logout_calls_injected_action_without_constructing_auth_domain() {
        show()
        rule.onNodeWithText("sidebar_logout".i18n()).performClick()
        rule.runOnIdle { assertEquals(1, signOuts) }
    }

    @Test
    fun loaded_user_feedback_passes_email_to_navigation() {
        user.value = UserInfo("id", "reader@example.test", "Reader", "", "", "", "en", "", "UTC", "", "", "")
        show()
        rule.onNodeWithText("sidebar_feedback".i18n()).performClick()
        rule.onNodeWithText("feedback-destination").assertIsDisplayed()
        rule.onNodeWithText("reader@example.test").assertIsDisplayed()
    }

    @Test
    fun feedback_before_user_loaded_does_not_crash() {
        show()
        rule.onNodeWithText("sidebar_feedback".i18n()).performClick()
        rule.onNodeWithText("feedback-destination").assertIsDisplayed()
        rule.onNodeWithTag("feedback-route-email").assertTextEquals("")
        rule.runOnIdle {
            assertEquals("", navigatedRoute?.email)
            assertEquals("inbox", navigatedRoute?.entryPoint)
            assertEquals(1, dismissals)
        }
    }

    @Test
    fun feedback_uses_user_loaded_after_initial_composition() {
        show()
        rule.runOnIdle {
            user.value = UserInfo("id", "late@example.test", "Reader", "", "", "", "en", "", "UTC", "", "", "")
        }
        rule.onNodeWithText("sidebar_feedback".i18n()).performClick()
        rule.onNodeWithText("feedback-destination").assertIsDisplayed()
        rule.onNodeWithTag("feedback-route-email").assertTextEquals("late@example.test")
    }

    @Test
    fun feedback_remains_available_when_user_becomes_unavailable() {
        user.value = UserInfo("id", "old@example.test", "Reader", "", "", "", "en", "", "UTC", "", "", "")
        show()
        rule.runOnIdle { user.value = null }
        rule.onNodeWithText("sidebar_feedback".i18n()).performClick()
        rule.onNodeWithText("feedback-destination").assertIsDisplayed()
        rule.onNodeWithTag("feedback-route-email").assertTextEquals("")
        rule.runOnIdle { assertEquals("", navigatedRoute?.email) }
    }
}
