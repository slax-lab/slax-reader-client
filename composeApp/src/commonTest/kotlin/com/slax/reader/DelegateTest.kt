package com.slax.reader

import com.slax.reader.data.network.MetricsType
import com.slax.reader.data.network.dto.OutlineResponse
import com.slax.reader.data.network.dto.OverviewResponse
import com.slax.reader.ui.bookmark.states.OutlineDelegate
import com.slax.reader.ui.bookmark.states.OutlineDialogStatus
import com.slax.reader.ui.bookmark.states.OverviewDelegate
import com.slax.reader.ui.bookmark.states.OverviewViewBounds
import com.slax.reader.ui.bookmark.states.BookmarkDelegate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DelegateTest {
    @Test
    fun outlineDelegate_transitions_dialog_and_loads_cached_content() = runTest {
        val local = FakeLocalBookmarkRepository()
        local.outline["id"] = "[Title](https://example.com)"
        local.outlineScroll["id"] = 7
        val api = FakeBookmarkAiApi()
        val delegate = OutlineDelegate(local, api, this, StandardTestDispatcher(testScheduler))

        delegate.loadOutline("id", networkAvailable = true)
        advanceUntilIdle()
        assertTrue(delegate.outlineState.value.outline.isNotEmpty())
        assertEquals(7, delegate.savedScrollPosition)

        delegate.showCollapsed()
        assertEquals(OutlineDialogStatus.COLLAPSED, delegate.dialogStatus.value)
        delegate.showDialog()
        advanceUntilIdle()
        assertEquals(OutlineDialogStatus.EXPANDED, delegate.dialogStatus.value)
        delegate.reset()
        assertEquals(OutlineDialogStatus.NONE, delegate.dialogStatus.value)
        assertEquals("", delegate.outlineState.value.outline)
    }

    @Test
    fun outlineDelegate_uses_network_stream_and_saves_final_outline() = runTest {
        val local = FakeLocalBookmarkRepository()
        val api = FakeBookmarkAiApi(
            outlineResponses = listOf(
                OutlineResponse.Outline("# Heading"),
                OutlineResponse.Done
            )
        )
        val delegate = OutlineDelegate(local, api, this, StandardTestDispatcher(testScheduler))
        delegate.loadOutline("id", networkAvailable = true)
        advanceUntilIdle()
        assertFalse(delegate.outlineState.value.isLoading)
        assertTrue(delegate.outlineState.value.outline.contains("Heading"))
        assertTrue(local.outline["id"].orEmpty().contains("Heading"))

        delegate.showDialog()
        advanceUntilIdle()
        assertEquals(listOf(MetricsType.SUMMARY), api.metrics)
    }

    @Test
    fun overviewDelegate_handles_offline_cache_miss_and_stream() = runTest {
        val local = FakeLocalBookmarkRepository()
        val api = FakeBookmarkAiApi(
            overviewResponses = listOf(
                OverviewResponse.Overview("A"),
                OverviewResponse.Overview("B"),
                OverviewResponse.KeyTakeaways(listOf("one", "two")),
                OverviewResponse.Done
            )
        )
        val delegate = OverviewDelegate(local, api, this, StandardTestDispatcher(testScheduler))
        delegate.loadOverview("id", networkAvailable = false)
        advanceUntilIdle()
        assertEquals("No network connection", delegate.overviewState.value.error)

        delegate.reset()
        delegate.loadOverview("id", networkAvailable = true)
        advanceUntilIdle()
        assertEquals("AB", delegate.overviewState.value.overview)
        assertEquals(listOf("one", "two"), delegate.overviewState.value.keyTakeaways)
        assertFalse(delegate.overviewState.value.isLoading)

        delegate.updateBounds(OverviewViewBounds(1f, 2f, 3f, 4f))
        assertEquals(OverviewViewBounds(1f, 2f, 3f, 4f), delegate.overviewBounds.value)
        delegate.sendOpenMetrics()
        advanceUntilIdle()
        assertEquals(MetricsType.OVERVIEW, api.metrics.last())
    }

    @Test
    fun bookmarkDelegate_writes_star_archive_title_and_tags() = runTest {
        val id = MutableStateFlow<String?>("bookmark-id")
        val bookmarks = FakeBookmarkRepository()
        val delegate = BookmarkDelegate(bookmarks, id, backgroundScope)

        delegate.toggleStar(true)
        delegate.toggleArchive(false)
        delegate.updateBookmarkTitle("Updated")
        delegate.updateBookmarkTags("bookmark-id", listOf("tag-a", "tag-b"))

        assertTrue(bookmarks.calls.any { it == "star:bookmark-id:1" })
        assertTrue(bookmarks.calls.any { it == "archive:bookmark-id:0" })
        assertTrue(bookmarks.calls.any { it == "title:bookmark-id:Updated" })
        assertTrue(bookmarks.calls.any { it.startsWith("metadata:bookmark-id:tags:") })
    }
}
