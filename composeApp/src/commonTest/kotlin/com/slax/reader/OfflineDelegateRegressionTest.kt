package com.slax.reader

import com.slax.reader.data.network.dto.OutlineResponse
import com.slax.reader.data.network.dto.OverviewResponse
import com.slax.reader.ui.bookmark.states.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineDelegateRegressionTest {
    @Test
    fun outline_cache_hit_offline_restores_content_and_position_without_api_call() = runTest {
        val local = FakeLocalBookmarkRepository().apply {
            outline["article"] = "# Offline outline"
            outlineScroll["article"] = 123
        }
        val api = FakeBookmarkAiApi()
        val delegate = OutlineDelegate(local, api, backgroundScope, StandardTestDispatcher(testScheduler))
        delegate.loadOutline("article", networkAvailable = false)
        runCurrent()
        assertTrue(delegate.outlineState.value.outline.contains("Offline outline"))
        assertEquals(123, delegate.savedScrollPosition)
        assertNull(delegate.outlineState.value.error)
        assertFalse(delegate.outlineState.value.isLoading)
        assertTrue(api.outlineRequests.isEmpty())
    }

    @Test
    fun overview_cache_hit_offline_restores_takeaways_without_api_call() = runTest {
        val local = FakeLocalBookmarkRepository().apply {
            overview["article"] = "saved overview" to listOf("one", "two")
        }
        val api = FakeBookmarkAiApi()
        val delegate = OverviewDelegate(local, api, backgroundScope, StandardTestDispatcher(testScheduler))
        delegate.loadOverview("article", networkAvailable = false)
        runCurrent()
        assertEquals("saved overview", delegate.overviewState.value.overview)
        assertEquals(listOf("one", "two"), delegate.overviewState.value.keyTakeaways)
        assertNull(delegate.overviewState.value.error)
        assertTrue(api.overviewRequests.isEmpty())
    }

    @Test
    fun outline_offline_miss_then_explicit_online_load_fetches_and_persists() = runTest {
        val local = FakeLocalBookmarkRepository()
        val api = FakeBookmarkAiApi(outlineResponses = listOf(OutlineResponse.Outline("# Recovered"), OutlineResponse.Done))
        val delegate = OutlineDelegate(local, api, backgroundScope, StandardTestDispatcher(testScheduler))
        delegate.loadOutline("article", false)
        runCurrent()
        assertEquals("No network connection", delegate.outlineState.value.error)
        assertTrue(api.outlineRequests.isEmpty())
        delegate.loadOutline("article", true)
        runCurrent()
        assertEquals(listOf("article"), api.outlineRequests)
        assertNull(delegate.outlineState.value.error)
        assertFalse(delegate.outlineState.value.isLoading)
        assertTrue(local.outline.getValue("article")!!.contains("Recovered"))
    }

    @Test
    fun stream_errors_stop_loading_and_do_not_persist_partial_content() = runTest {
        val local = FakeLocalBookmarkRepository()
        val api = FakeBookmarkAiApi(
            outlineResponses = listOf(OutlineResponse.Outline("partial"), OutlineResponse.Error("lost connection")),
            overviewResponses = listOf(OverviewResponse.Overview("partial"), OverviewResponse.Error("lost connection"))
        )
        val dispatcher = StandardTestDispatcher(testScheduler)
        val outline = OutlineDelegate(local, api, backgroundScope, dispatcher)
        val overview = OverviewDelegate(local, api, backgroundScope, dispatcher)
        outline.loadOutline("article", true)
        overview.loadOverview("article", true)
        runCurrent()
        assertEquals("lost connection", outline.outlineState.value.error)
        assertEquals("lost connection", overview.overviewState.value.error)
        assertFalse(outline.outlineState.value.isLoading)
        assertFalse(overview.overviewState.value.isLoading)
        assertTrue(local.outline.isEmpty())
        assertTrue(local.overview.isEmpty())
    }

    @Test
    fun completed_overview_persists_decoded_takeaways_and_skips_duplicate_fetch() = runTest {
        val local = FakeLocalBookmarkRepository()
        val api = FakeBookmarkAiApi(overviewResponses = listOf(
            OverviewResponse.Overview("A"), OverviewResponse.Overview("B"),
            OverviewResponse.KeyTakeaways(listOf("中文", "\"quote\"")), OverviewResponse.Done
        ))
        val delegate = OverviewDelegate(local, api, backgroundScope, StandardTestDispatcher(testScheduler))
        delegate.loadOverview("article", true)
        runCurrent()
        assertEquals("AB" to listOf("中文", "\"quote\""), local.overview["article"])
        delegate.loadOverview("article", true)
        runCurrent()
        assertEquals(listOf("article"), api.overviewRequests)
        delegate.reset()
        delegate.loadOverview("article", false)
        runCurrent()
        assertEquals("AB", delegate.overviewState.value.overview)
        assertEquals(listOf("article"), api.overviewRequests)
    }

    @Test
    fun scroll_debounce_writes_only_latest_position_and_flush_writes_immediately() = runTest {
        val local = FakeLocalBookmarkRepository().apply { outline["article"] = "# cached" }
        val delegate = OutlineDelegate(local, FakeBookmarkAiApi(), backgroundScope, StandardTestDispatcher(testScheduler))
        delegate.loadOutline("article", false)
        runCurrent()
        delegate.saveScrollPosition(10)
        advanceTimeBy(1_000)
        delegate.saveScrollPosition(20)
        advanceTimeBy(1_999)
        assertNull(local.outlineScroll["article"])
        advanceTimeBy(1)
        runCurrent()
        assertEquals(20, local.outlineScroll["article"])
        delegate.saveScrollPosition(30)
        delegate.flushScrollPosition()
        runCurrent()
        assertEquals(30, local.outlineScroll["article"])
    }

    @Test
    fun reset_flushes_old_article_position_without_writing_to_next_article() = runTest {
        val local = FakeLocalBookmarkRepository().apply {
            outline["old"] = "# old"
            outline["next"] = "# next"
        }
        val delegate = OutlineDelegate(local, FakeBookmarkAiApi(), backgroundScope, StandardTestDispatcher(testScheduler))
        delegate.loadOutline("old", false)
        runCurrent()
        delegate.saveScrollPosition(65)
        delegate.reset()
        delegate.loadOutline("next", false)
        runCurrent()
        advanceTimeBy(2_001)
        runCurrent()
        assertEquals(65, local.outlineScroll["old"])
        assertNull(local.outlineScroll["next"])
    }

    @Test
    fun bookmark_actions_without_selected_id_do_not_mutate_repository() = runTest {
        val repository = FakeBookmarkRepository()
        val delegate = BookmarkDelegate(repository, MutableStateFlow(null), backgroundScope)
        delegate.toggleStar(true)
        delegate.toggleArchive(true)
        delegate.updateBookmarkTitle("orphan")
        delegate.deleteBookmark()
        assertTrue(repository.calls.isEmpty())
    }
}
