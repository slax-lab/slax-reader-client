package com.slax.reader

import com.slax.reader.data.database.model.BookmarkSortType
import com.slax.reader.data.database.model.InboxListBookmarkItem
import com.slax.reader.data.database.model.isDownloaded
import com.slax.reader.domain.sync.DownloadStatus
import com.slax.reader.ui.bookmark.states.BookmarkOverlay
import com.slax.reader.ui.bookmark.states.MarkInteractionState
import com.slax.reader.ui.bookmark.states.OverlayDelegate
import com.slax.reader.ui.inbox.compenents.toSwipeConfig
import com.slax.reader.utils.BridgeMarkItemInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModelAndStateTest {
    @Test
    fun displayTitle_prefers_alias_then_metadata_then_url_then_id() {
        fun item(alias: String, title: String?, url: String?) = InboxListBookmarkItem(
            id = "abcdef", aliasTitle = alias, updatedAt = "", archiveStatus = 0,
            isStarred = 0, metadataStatus = null, metadataTitle = title, metadataUrl = url
        )
        assertEquals("Alias", item("Alias", "Title", "url").displayTitle())
        assertEquals("Title", item("", "Title", "url").displayTitle())
        assertEquals("url", item("", null, "url").displayTitle())
        assertEquals("abcde", item("", null, null).displayTitle())
    }

    @Test
    fun sortTypes_expose_expected_database_contract() {
        assertEquals("updated_at", BookmarkSortType.UPDATED.column)
        assertEquals("starred_at", BookmarkSortType.STARRED.column)
        assertEquals("archived_at", BookmarkSortType.ARCHIVED.column)
        assertTrue(BookmarkSortType.STARRED.whereClause.contains("is_starred = 1"))
        assertTrue(BookmarkSortType.ARCHIVED.whereClause.contains("archive_status = 1"))
    }

    @Test
    fun overlayDelegate_replaces_only_matching_overlay_and_resets_everything() {
        val delegate = OverlayDelegate()
        delegate.showOverlay(BookmarkOverlay.Toolbar)
        delegate.dismissOverlay(BookmarkOverlay.EditTitle)
        assertEquals(BookmarkOverlay.Toolbar, delegate.overlay.value)
        delegate.dismissOverlay(BookmarkOverlay.Toolbar)
        assertNull(delegate.overlay.value)

        delegate.showOverlay(BookmarkOverlay.Tags)
        delegate.onWebViewImageClick("current", listOf("current", "other"))
        assertEquals("current", delegate.imageViewerState.value?.currentImageUrl)
        delegate.reset()
        assertNull(delegate.overlay.value)
        assertNull(delegate.imageViewerState.value)
    }

    @Test
    fun markInteractionState_tracks_selection_and_panel_modes() {
        val state = MarkInteractionState()
        state.onTextSelected("text", 42f)
        assertEquals("text", state.selectedText)
        assertEquals(42f, state.selectionY)
        assertTrue(state.menuVisible)

        val mark = BridgeMarkItemInfo(
            id = "mark-1",
            source = emptyList(),
            approx = null,
            stroke = emptyList(),
            comments = emptyList()
        )
        state.openPanelForNewComment("selected", mark)
        assertTrue(state.panelVisible)
        assertTrue(state.shouldAutoFocus)
        state.dismissPanel()
        assertFalse(state.panelVisible)
        assertNull(state.selectedMark)
    }

    @Test
    fun local_download_only_completed_is_downloaded() {
        assertFalse(com.slax.reader.data.database.model.LocalBookmarkInfo("id", null, null, DownloadStatus.NONE.code).isDownloaded())
        assertTrue(com.slax.reader.data.database.model.LocalBookmarkInfo("id", null, null, DownloadStatus.COMPLETED.code).isDownloaded())
    }

    @Test
    fun swipe_configuration_matches_each_inbox_mode() {
        val updated = BookmarkSortType.UPDATED.toSwipeConfig()
        assertTrue(updated.showStarAction)
        assertTrue(updated.showArchiveAction)
        assertEquals(130, updated.maxSwipeWidthDp.value.toInt())

        val starred = BookmarkSortType.STARRED.toSwipeConfig()
        assertTrue(starred.showStarAction)
        assertFalse(starred.showArchiveAction)
        assertEquals(70, starred.maxSwipeWidthDp.value.toInt())

        val archived = BookmarkSortType.ARCHIVED.toSwipeConfig()
        assertFalse(archived.showStarAction)
        assertTrue(archived.showArchiveAction)
        assertEquals(70, archived.maxSwipeWidthDp.value.toInt())
    }
}
