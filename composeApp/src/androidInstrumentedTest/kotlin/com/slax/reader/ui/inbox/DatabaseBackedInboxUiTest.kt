package com.slax.reader.ui.inbox

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.powersync.DatabaseDriverFactory
import com.powersync.PowerSyncDatabase
import com.powersync.db.getString
import com.slax.reader.data.database.AppSchema
import com.slax.reader.data.database.dao.BookmarkDao
import com.slax.reader.data.database.dao.LocalBookmarkDao
import com.slax.reader.data.database.dao.UserDao
import com.slax.reader.domain.coordinator.AppSyncState
import com.slax.reader.domain.coordinator.NetworkCoordinator
import com.slax.reader.domain.sync.DownloadStatus
import com.slax.reader.testing.TestTags
import com.slax.reader.ui.inbox.compenents.ArticleList
import com.slax.reader.ui.inbox.compenents.DownloadStateKey
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import java.util.UUID

/** Real DAO -> real ViewModel -> real Compose tree, with network fixed offline. */
class DatabaseBackedInboxUiTest {
    @get:Rule val rule: ComposeContentTestRule = createComposeRule()
    private lateinit var database: PowerSyncDatabase
    private lateinit var bookmarks: BookmarkDao
    private lateinit var local: LocalBookmarkDao
    private lateinit var daoScope: CoroutineScope
    private lateinit var filename: String
    private lateinit var context: Context
    private lateinit var viewModel: InboxListViewModel
    private val store = ViewModelStore()
    private var visible by mutableStateOf(true)

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        filename = "ui-database-${UUID.randomUUID()}.db"
        database = PowerSyncDatabase(DatabaseDriverFactory(context), AppSchema, filename)
        daoScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        bookmarks = BookmarkDao(daoScope, database)
        local = LocalBookmarkDao(daoScope, database)
        rule.runOnIdle {
            viewModel = InboxListViewModel(UserDao(daoScope, database), bookmarks, local,
                object : NetworkCoordinator {
                    override val syncState = MutableStateFlow<AppSyncState>(AppSyncState.NoNetwork)
                    override suspend fun isNetworkAvailable() = false
                })
            store.put("inbox", viewModel)
        }
        rule.setContent {
            if (visible) ArticleList(rememberNavController(), viewModel, onEditTitle = {})
        }
    }

    @After
    fun cleanup() {
        rule.runOnIdle { visible = false; store.clear() }
        rule.waitForIdle()
        runBlocking {
            daoScope.cancel()
            try { database.close() } finally { context.deleteDatabase(filename) }
        }
    }

    private fun awaitTitle(title: String) {
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText(title).assertIsDisplayed()
    }

    private fun createArticle(): String = runBlocking {
        viewModel.addLinkBookmark("https://example.test/offline-ui")
        database.get("SELECT id FROM sr_user_bookmark") { it.getString("id") }
    }

    @Test
    fun offline_create_rename_delete_updates_rendered_list_without_server() {
        rule.onNodeWithTag(TestTags.EmptyInbox).assertIsDisplayed()
        val id = createArticle()
        awaitTitle("https://example.test/offline-ui")
        runBlocking { viewModel.confirmEditTitle(id, "离线修改标题") }
        awaitTitle("离线修改标题")
        rule.onNodeWithText("https://example.test/offline-ui").assertDoesNotExist()
        runBlocking { viewModel.deleteBookmark(id) }
        rule.waitUntil(5_000) {
            rule.onAllNodesWithTag(TestTags.EmptyInbox).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag(TestTags.EmptyInbox).assertIsDisplayed()
        rule.onNodeWithText("离线修改标题").assertDoesNotExist()
        runBlocking { assertNotNull(database.getCrudBatch()) }
    }

    @Test
    fun persisted_download_status_transitions_reach_real_ui_semantics() {
        val id = createArticle()
        awaitTitle("https://example.test/offline-ui")
        for (status in listOf(DownloadStatus.FAILED, DownloadStatus.DOWNLOADING, DownloadStatus.COMPLETED)) {
            runBlocking { local.updateLocalBookmarkDownloadStatus(id, status.code, false) }
            rule.waitUntil(5_000) {
                rule.onAllNodesWithTag(TestTags.BookmarkItemStatus, useUnmergedTree = true)
                    .fetchSemanticsNodes().singleOrNull()?.config?.getOrElse(DownloadStateKey) { "" } == status.name
            }
            val statusNode = rule.onNodeWithTag(TestTags.BookmarkItemStatus, useUnmergedTree = true)
            statusNode.assertIsDisplayed()
            assertEquals(status.name, statusNode.fetchSemanticsNode().config[DownloadStateKey])
        }
    }
}
