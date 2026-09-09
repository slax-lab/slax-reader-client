package com.slax.reader

import com.slax.reader.const.AppError
import com.slax.reader.data.network.dto.DeleteAccountData
import com.slax.reader.data.network.dto.HttpData
import com.slax.reader.domain.auth.AppleSignInResult
import com.slax.reader.domain.auth.AuthGateway
import com.slax.reader.domain.auth.GoogleSignInResult
import com.slax.reader.ui.inbox.InboxListViewModel
import com.slax.reader.ui.login.LoginViewModel
import com.slax.reader.ui.setting.DeleteAccountState
import com.slax.reader.ui.setting.SettingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.coroutines.EmptyCoroutineContext
import app.cash.turbine.test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun inboxViewModel_merges_local_download_state_and_emits_events() = runTest {
        val user = FakeUserRepository()
        val bookmarks = FakeBookmarkRepository()
        val local = FakeLocalBookmarkRepository()
        val coordinator = FakeNetworkCoordinator()
        val item = com.slax.reader.data.database.model.InboxListBookmarkItem(
            id = "bookmark-1", aliasTitle = "", updatedAt = "", archiveStatus = 0,
            isStarred = 0, metadataStatus = null, metadataTitle = "Title", metadataUrl = "url"
        )
        bookmarks.bookmarks.value = listOf(item)
        local.local.value = mapOf(
            "bookmark-1" to com.slax.reader.data.database.model.LocalBookmarkInfo(
                "bookmark-1", null, null, 2, true
            )
        )
        val viewModel = InboxListViewModel(user, bookmarks, local, coordinator)

        val merged = viewModel.bookmarks.first { it.isNotEmpty() }
        assertEquals(2, merged.single().downloadStatus)
        assertTrue(merged.single().isAutoCached)

        viewModel.setSortType(com.slax.reader.data.database.model.BookmarkSortType.ARCHIVED)
        assertEquals(com.slax.reader.data.database.model.BookmarkSortType.ARCHIVED, viewModel.sortType.value)
        viewModel.processingUrlEvent.test {
            viewModel.emitProcessingUrl("https://example.com")
            assertEquals("https://example.com", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.scrollToTopEvent.test {
            viewModel.scrollToTop()
            assertEquals(Unit, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun inboxViewModel_delegates_mutations_to_bookmark_repository() = runTest {
        val bookmarks = FakeBookmarkRepository()
        val viewModel = InboxListViewModel(
            FakeUserRepository(), bookmarks, FakeLocalBookmarkRepository(), FakeNetworkCoordinator()
        )
        viewModel.confirmEditTitle("id", "new")
        viewModel.toggleStar("id", true)
        viewModel.toggleArchive("id", false)
        viewModel.deleteBookmark("id")
        viewModel.addLinkBookmark("https://example.com")
        assertEquals(
            listOf("title:id:new", "star:id:1", "archive:id:0", "delete:id", "bookmark:https://example.com"),
            bookmarks.calls
        )
    }

    @Test
    fun settingViewModel_maps_delete_account_outcomes_and_exceptions() = runTest {
        val api = FakeAccountApi()
        val viewModel = SettingViewModel(api, FakeSettingsPreferences())
        viewModel.deleteAccount()
        assertEquals(DeleteAccountState.Success, viewModel.deleteAccountState.value)

        api.response = HttpData(DeleteAccountData(false, com.slax.reader.data.network.dto.DeleteAccountReason.ACTIVE_SUBSCRIPTION), "", 200)
        viewModel.resetState()
        viewModel.deleteAccount()
        assertEquals(
            DeleteAccountState.Error("无法删除账号：您有正在进行的订阅，请先取消订阅后再试"),
            viewModel.deleteAccountState.value
        )

        api.error = IllegalStateException("offline")
        viewModel.resetState()
        viewModel.deleteAccount()
        assertEquals(DeleteAccountState.Error("offline"), viewModel.deleteAccountState.value)

        api.error = null
        api.response = HttpData(null, "", 200)
        viewModel.resetState()
        viewModel.deleteAccount()
        assertEquals(DeleteAccountState.Error("删除账号时返回数据为空"), viewModel.deleteAccountState.value)
    }

    @Test
    fun settingViewModel_preferences_are_observable_and_mutable() = runTest {
        val prefs = FakeSettingsPreferences()
        val viewModel = SettingViewModel(FakeAccountApi(), prefs)
        viewModel.updateCacheCount(100)
        viewModel.updateDownloadImages(false)
        advanceUntilIdle()
        assertEquals(100, viewModel.cacheCount.value)
        assertFalse(viewModel.downloadImages.value)
    }

    @Test
    fun loginViewModel_reports_loading_success_and_api_errors() = runTest {
        val auth = object : AuthGateway {
            var result: Result<Unit> = Result.success(Unit)
            override suspend fun signIn(code: String, type: String, redirectUrl: String, idToken: String) = result
        }
        val login = LoginViewModel(auth, dispatcher, dispatcher)
        val loading = mutableListOf<Boolean>()
        var success = 0
        val errors = mutableListOf<String>()

        login.googleSignIn(
            Result.success(GoogleSignInResult("token")),
            loading::add,
            { success++ },
            errors::add
        )
        assertEquals(listOf(true, false), loading)
        assertEquals(1, success)
        assertTrue(errors.isEmpty())

        auth.result = Result.failure(AppError.ApiException.HttpError(401, "unauthorized"))
        login.googleSignIn(
            Result.success(GoogleSignInResult("token")),
            loading::add,
            { success++ },
            errors::add
        )
        assertEquals("Login failed (401): unauthorized", errors.last())

        login.googleSignIn(
            Result.success(GoogleSignInResult("")),
            loading::add,
            { success++ },
            errors::add
        )
        assertEquals("Failed to get Google ID token", errors.last())
    }

    @Test
    fun loginViewModel_handles_failed_provider_result_and_apple_success() = runTest {
        val auth = object : AuthGateway {
            override suspend fun signIn(code: String, type: String, redirectUrl: String, idToken: String) = Result.success(Unit)
        }
        val login = LoginViewModel(auth, EmptyCoroutineContext, EmptyCoroutineContext)
        val errors = mutableListOf<String>()
        var success = 0
        login.appleSignIn(
            Result.failure(IllegalArgumentException("provider failed")),
            {},
            { success++ },
            errors::add
        )
        assertEquals("provider failed", errors.single())
        login.appleSignIn(
            Result.success(AppleSignInResult("code", "id-token")),
            {},
            { success++ },
            errors::add
        )
        assertEquals(1, success)
    }
}
