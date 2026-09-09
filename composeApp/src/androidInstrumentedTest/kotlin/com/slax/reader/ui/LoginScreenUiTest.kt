package com.slax.reader.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation.compose.*
import com.slax.reader.const.InboxRoutes
import com.slax.reader.const.LoginRoutes
import com.slax.reader.domain.auth.AuthGateway
import com.slax.reader.domain.auth.GoogleSignInResult
import com.slax.reader.testing.TestTags
import com.slax.reader.ui.login.LoginScreen
import com.slax.reader.ui.login.LoginViewModel
import com.slax.reader.utils.AppWebViewState
import com.slax.reader.utils.i18n
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Only native web content is substituted; agreement UI and navigation are production code. */
private object AgreementWebHost : WebViewHost {
    @Composable override fun Url(url: String, modifier: Modifier, webState: AppWebViewState, contentInsets: PaddingValues?) {
        Text(url, modifier)
    }
    @Composable override fun Html(htmlContent: String, modifier: Modifier, webState: AppWebViewState, bookmarkId: String) {
        error("Login must not render article HTML")
    }
}

class LoginScreenUiTest {
    @get:Rule val rule: ComposeContentTestRule = createComposeRule()
    private var providerCalls = 0
    private var authCalls = 0

    private fun show(
        providerResult: Result<GoogleSignInResult> = Result.success(GoogleSignInResult("token")),
        authResult: Result<Unit> = Result.success(Unit),
        gate: CompletableDeferred<Unit>? = null,
    ) {
        val auth = object : AuthGateway {
            override suspend fun signIn(code: String, type: String, redirectUrl: String, idToken: String): Result<Unit> {
                authCalls++
                gate?.await()
                return authResult
            }
        }
        rule.setContent {
            val nav = rememberNavController()
            NavHost(nav, startDestination = LoginRoutes) {
                composable<LoginRoutes> {
                    LoginScreen(
                        navController = nav,
                        viewModel = androidx.compose.runtime.remember { LoginViewModel(auth, Dispatchers.Main, Dispatchers.Main) },
                        webViewHost = AgreementWebHost,
                        googleSignIn = { providerCalls++; providerResult }
                    )
                }
                composable<InboxRoutes> { Text("signed-in-destination") }
            }
        }
    }

    @Test
    fun disagree_does_not_start_provider_and_agree_continues_login_to_inbox() {
        show()
        rule.onNodeWithText("login_google".i18n()).performClick()
        rule.onNodeWithText("agreement_tab_terms".i18n()).assertIsDisplayed()
        rule.runOnIdle { assertEquals(0, providerCalls) }
        rule.onNodeWithText("agreement_btn_disagree".i18n()).performClick()
        rule.runOnIdle { assertEquals(0, providerCalls); assertEquals(0, authCalls) }
        rule.onNodeWithText("login_google".i18n()).performClick()
        rule.onNodeWithText("agreement_tab_privacy".i18n()).performClick()
        rule.onNodeWithText("/privacy", substring = true, useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("agreement_btn_agree".i18n()).performClick()
        rule.onNodeWithText("signed-in-destination").assertIsDisplayed()
        rule.runOnIdle { assertEquals(1, providerCalls); assertEquals(1, authCalls) }
    }

    @Test
    fun provider_error_is_displayed_and_does_not_call_auth() {
        show(providerResult = Result.failure(IllegalStateException("provider unavailable")))
        rule.onNodeWithText("login_google".i18n()).performClick()
        rule.onNodeWithText("agreement_btn_agree".i18n()).performClick()
        rule.onNodeWithText("provider unavailable").assertIsDisplayed()
        rule.onNodeWithText("btn_ok".i18n()).performClick()
        rule.onNodeWithText("login_google".i18n()).assertIsDisplayed()
        rule.runOnIdle { assertEquals(0, authCalls) }
    }

    @Test
    fun cancelled_provider_returns_to_login_without_error_dialog() {
        show(providerResult = Result.failure(IllegalStateException("user cancelled")))
        rule.onNodeWithText("login_google".i18n()).performClick()
        rule.onNodeWithText("agreement_btn_agree".i18n()).performClick()
        rule.onNodeWithText("login_failed".i18n()).assertDoesNotExist()
        rule.onNodeWithText("login_google".i18n()).assertIsDisplayed()
        rule.runOnIdle { assertEquals(1, providerCalls); assertEquals(0, authCalls) }
    }

    @Test
    fun authentication_pending_disables_button_then_error_restores_login() {
        val gate = CompletableDeferred<Unit>()
        show(authResult = Result.failure(IllegalStateException("offline auth")), gate = gate)
        rule.onNodeWithText("login_google".i18n()).performClick()
        rule.onNodeWithText("agreement_btn_agree".i18n()).performClick()
        rule.onNodeWithTag(TestTags.LoginButton).assertIsNotEnabled()
        rule.onNodeWithTag(TestTags.LoginLoadingIndicator).assertIsDisplayed()
        rule.runOnIdle { assertEquals(1, authCalls); gate.complete(Unit) }
        rule.onNodeWithText("offline auth").assertIsDisplayed()
        rule.onNodeWithText("btn_ok".i18n()).performClick()
        rule.onNodeWithTag(TestTags.LoginButton).assertIsEnabled()
        rule.onNodeWithTag(TestTags.LoginLoadingIndicator).assertDoesNotExist()
    }
}
