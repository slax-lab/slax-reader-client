package com.slax.reader.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.powersync.DatabaseDriverFactory
import com.powersync.PowerSyncDatabase
import com.powersync.db.getString
import com.slax.reader.data.database.AppSchema
import com.slax.reader.data.database.dao.*
import com.slax.reader.data.database.model.BookmarkSortType
import com.slax.reader.data.database.model.UserBookmark
import com.slax.reader.data.network.dto.MarkType
import com.slax.reader.domain.sync.DownloadStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Executes production DAOs against the bundled PowerSync/SQLite driver.
 * Every test owns a unique on-disk database. No connection or backend is used.
 */
class PowerSyncDaoTest {
    private lateinit var context: Context
    private lateinit var filename: String
    private lateinit var database: PowerSyncDatabase
    private lateinit var daoScope: CoroutineScope
    private lateinit var bookmarks: BookmarkDao
    private lateinit var local: LocalBookmarkDao

    @Before
    fun open() {
        context = ApplicationProvider.getApplicationContext()
        filename = "dao-test-${UUID.randomUUID()}.db"
        openDatabase()
    }

    private fun openDatabase() {
        database = PowerSyncDatabase(DatabaseDriverFactory(context), schema = AppSchema, dbFilename = filename)
        daoScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        bookmarks = BookmarkDao(daoScope, database)
        local = LocalBookmarkDao(daoScope, database)
    }

    @After
    fun close() = runBlocking {
        daoScope.cancel()
        try {
            database.close()
        } finally {
            // Only the unique file created by this test, never powersync.db.
            context.deleteDatabase(filename)
        }
    }

    private suspend fun create(url: String = "https://example.test/offline"): String {
        bookmarks.createBookmark(url)
        return database.get("SELECT id FROM sr_user_bookmark WHERE JSON_EXTRACT(metadata, '$.bookmark.target_url') = ?", listOf(url)) {
            it.getString("id")
        }
    }

    private suspend fun detail(id: String): UserBookmark =
        withTimeout(5_000) { bookmarks.watchBookmarkDetail(id).first { it.isNotEmpty() }.single() }

    @Test
    fun offline_crud_updates_real_rows_and_leaves_uploads_pending(): Unit = runBlocking {
        val id = create()
        assertEquals("pending", detail(id).metadataObj?.bookmark?.status)
        bookmarks.updateBookmarkAliasTitle(id, "离线 ' title")
        bookmarks.updateBookmarkStar(id, 1)
        bookmarks.updateBookmarkArchive(id, 1)
        val updated = detail(id)
        assertEquals("离线 ' title", updated.displayTitle)
        assertEquals(1, updated.isStarred)
        assertEquals(1, updated.archiveStatus)

        bookmarks.deleteBookmark(id)
        assertNotNull(detail(id).deletedAt)
        val pending = requireNotNull(database.getCrudBatch())
        assertTrue(pending.crud.any { it.id == id && it.table == "sr_user_bookmark" })
        assertTrue("No acknowledgement was sent; the queue must remain pending", database.getCrudBatch() != null)
    }

    @Test
    fun list_filters_follow_star_archive_and_soft_delete_mutations(): Unit = runBlocking {
        val id = create()
        withTimeout(5_000) {
            assertEquals(id, bookmarks.watchUserBookmarkPaged(BookmarkSortType.UPDATED).first { !it.isNullOrEmpty() }!!.single().id)
        }
        bookmarks.updateBookmarkStar(id, 1)
        bookmarks.updateBookmarkArchive(id, 1)
        withTimeout(5_000) {
            assertEquals(id, bookmarks.watchUserBookmarkPaged(BookmarkSortType.STARRED).first { !it.isNullOrEmpty() }!!.single().id)
            assertEquals(id, bookmarks.watchUserBookmarkPaged(BookmarkSortType.ARCHIVED).first { !it.isNullOrEmpty() }!!.single().id)
            bookmarks.watchUserBookmarkPaged(BookmarkSortType.UPDATED).first { it != null && it.isEmpty() }
        }
        bookmarks.deleteBookmark(id)
        withTimeout(5_000) {
            bookmarks.watchUserBookmarkPaged(BookmarkSortType.STARRED).first { it != null && it.isEmpty() }
            bookmarks.watchUserBookmarkPaged(BookmarkSortType.ARCHIVED).first { it != null && it.isEmpty() }
        }
    }

    @Test
    fun tag_creation_lookup_and_metadata_update_preserve_article_fields(): Unit = runBlocking {
        val id = create()
        val tag = bookmarks.createTag("中文 ' tag")
        assertEquals(listOf(tag), bookmarks.getTagsByIds(listOf(tag.id, "missing")))
        assertTrue(bookmarks.getTagsByIds(emptyList()).isEmpty())
        bookmarks.updateMetadataField(id, "tags", """["${tag.id}"]""")
        assertEquals(listOf(tag.id), detail(id).metadataObj?.tags)
        assertEquals("https://example.test/offline", detail(id).metadataObj?.bookmark?.target_url)
    }

    @Test
    fun failed_status_is_persisted_then_replaced_by_explicit_next_attempt(): Unit = runBlocking {
        local.updateLocalBookmarkDownloadStatus("failed", DownloadStatus.FAILED.code, false)
        withTimeout(5_000) {
            val item = local.watchUserLocalBookmarkMap().first { it["failed"]?.downloadStatus == DownloadStatus.FAILED.code }
            assertFalse(item.getValue("failed").isAutoCached)
        }
        local.updateLocalBookmarkDownloadStatus("failed", DownloadStatus.DOWNLOADING.code, false)
        withTimeout(5_000) {
            local.watchUserLocalBookmarkMap().first { it["failed"]?.downloadStatus == DownloadStatus.DOWNLOADING.code }
        }
        local.updateLocalBookmarkDownloadStatus("failed", DownloadStatus.COMPLETED.code, false)
        withTimeout(5_000) {
            local.watchUserLocalBookmarkMap().first { it["failed"]?.downloadStatus == DownloadStatus.COMPLETED.code }
        }
        assertNull("Local-only writes must not enter PowerSync upload queue", database.getCrudBatch())
    }

    @Test
    fun batch_reset_preserves_unselected_rows_and_other_cached_fields(): Unit = runBlocking {
        for (id in listOf("a", "b", "c")) {
            local.updateLocalBookmarkDownloadStatus(id, DownloadStatus.COMPLETED.code, true)
            local.updateLocalBookmarkOutline(id, "outline-$id")
            local.updateLocalBookmarkReadPosition(id, 42.5f)
        }
        local.batchResetDownloadStatus(emptyList())
        local.batchResetDownloadStatus(listOf("a", "c"))
        val rows = withTimeout(5_000) {
            local.watchUserLocalBookmarkMap().first { it["a"]?.downloadStatus == 0 && it["c"]?.downloadStatus == 0 }
        }
        assertEquals(DownloadStatus.COMPLETED.code, rows.getValue("b").downloadStatus)
        assertFalse(rows.getValue("a").isAutoCached)
        assertTrue(rows.getValue("b").isAutoCached)
        for (id in listOf("a", "b", "c")) {
            assertEquals("outline-$id", local.getLocalBookmarkOutline(id))
            assertEquals(42.5f, local.getLocalBookmarkReadPosition(id))
        }
    }

    @Test
    fun cached_fields_and_pending_crud_survive_database_close_and_reopen(): Unit = runBlocking {
        val id = create()
        local.updateLocalBookmarkReadPosition(id, 18.5f)
        local.updateLocalBookmarkOutline(id, "cached outline")
        local.updateLocalBookmarkOutlineScrollPosition(id, 87)
        local.updateLocalBookmarkOverview(id, "cached overview", """["a","中文"]""")
        local.updateMarkUsers(id, """[{"id":"reader"}]""")
        daoScope.cancel()
        database.close()
        openDatabase()
        assertEquals("cached outline", local.getLocalBookmarkOutline(id))
        assertEquals(87, local.getLocalBookmarkOutlineScrollPosition(id))
        assertEquals(18.5f, local.getLocalBookmarkReadPosition(id))
        assertEquals("cached overview" to listOf("a", "中文"), local.getLocalBookmarkOverview(id))
        assertEquals("""[{"id":"reader"}]""", local.getMarkUsers(id))
        assertEquals(id, detail(id).id)
        assertNotNull(database.getCrudBatch())
    }

    @Test
    fun absent_and_malformed_local_cache_values_are_safe(): Unit = runBlocking {
        assertNull(local.getLocalBookmarkReadPosition("missing"))
        assertNull(local.getLocalBookmarkOutline("missing"))
        assertNull(local.getLocalBookmarkOutlineScrollPosition("missing"))
        assertEquals(null to null, local.getLocalBookmarkOverview("missing"))
        local.updateLocalBookmarkOverview("bad", "still readable", "not-json")
        assertEquals("still readable" to null, local.getLocalBookmarkOverview("bad"))
        database.execute(
            "UPDATE ps_data_local__local_bookmark_info SET data = JSON_SET(data, '$.read_position', 'invalid', '$.outline_read_position', 'invalid') WHERE id = ?",
            listOf("bad")
        )
        assertNull(local.getLocalBookmarkReadPosition("bad"))
        assertNull(local.getLocalBookmarkOutlineScrollPosition("bad"))
    }

    @Test
    fun failed_write_transaction_rolls_back_rows_and_upload_queue(): Unit = runBlocking {
        try {
            database.writeTransaction { tx ->
                tx.execute("INSERT INTO sr_user_tag (id, tag_name) VALUES (?, ?)", listOf("rollback", "temporary"))
                error("simulate interrupted write")
            }
            fail("Transaction should throw")
        } catch (e: Exception) {
            assertTrue(e.toString().contains("simulate interrupted write"))
        }
        assertTrue(bookmarks.getTagsByIds(listOf("rollback")).isEmpty())
        assertNull(database.getCrudBatch())
    }

    @Test
    fun comment_insert_and_soft_delete_keep_other_bookmarks_untouched(): Unit = runBlocking {
        val comments = BookmarkCommentDao(database)
        val id = comments.addMark("article", "user", MarkType.COMMENT, comment = "offline comment")
        comments.addMark("other", "user", MarkType.COMMENT, comment = "other comment")
        val row = withTimeout(5_000) { comments.watchComments("article").first { it.isNotEmpty() }.single() }
        assertEquals("offline comment", row.comment)
        assertEquals("user", row.metadataObj?.user_id)
        comments.deleteComment(id)
        withTimeout(5_000) {
            assertEquals(1, comments.watchComments("article").first { it.singleOrNull()?.is_deleted == 1 }.single().is_deleted)
            assertEquals(0, comments.watchComments("other").first { it.isNotEmpty() }.single().is_deleted)
        }
    }

    @Test
    fun user_watch_maps_optional_nulls_and_observes_removal(): Unit = runBlocking {
        val users = UserDao(daoScope, database)
        database.execute(
            "INSERT INTO sr_user (id, email, lang, timezone) VALUES (?, ?, ?, ?)",
            listOf("reader", "reader@example.test", "zh", "Asia/Shanghai")
        )
        val user = withTimeout(5_000) { users.watchUserInfo().first { it != null }!! }
        assertEquals("reader", user.id)
        assertEquals("", user.name)
        assertEquals("", user.picture)
        assertEquals("zh", user.lang)
        database.execute("DELETE FROM sr_user WHERE id = ?", listOf("reader"))
        withTimeout(5_000) { users.watchUserInfo().first { it == null } }
    }

    @Test
    fun subscription_watch_maps_null_fields_and_observes_updates(): Unit = runBlocking {
        val subscriptions = SubscriptionDao(daoScope, database)
        database.execute("INSERT INTO sr_user_subscription (id) VALUES (?)", listOf("sub"))
        val initial = withTimeout(5_000) { subscriptions.watchSubscriptionInfo().first { it != null }!! }
        assertEquals("", initial.stripe_subscription_id)
        assertEquals(0, initial.subscribed)
        database.execute("UPDATE sr_user_subscription SET subscribed = 1, auto_renew = 1 WHERE id = ?", listOf("sub"))
        val updated = withTimeout(5_000) { subscriptions.watchSubscriptionInfo().first { it?.subscribed == 1 }!! }
        assertEquals(1, updated.auto_renew)
    }
}
