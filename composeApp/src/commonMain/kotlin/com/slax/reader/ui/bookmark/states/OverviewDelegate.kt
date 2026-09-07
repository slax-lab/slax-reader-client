package com.slax.reader.ui.bookmark.states

import com.slax.reader.data.database.dao.LocalBookmarkRepository
import com.slax.reader.data.network.BookmarkAiApi
import com.slax.reader.data.network.MetricsType
import com.slax.reader.data.network.dto.OverviewResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

data class OverviewState(
    val overview: String = "",
    val keyTakeaways: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class OverviewViewBounds(
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f
)

class OverviewDelegate(
    private val localBookmarkDao: LocalBookmarkRepository,
    private val apiService: BookmarkAiApi,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    private val _overviewState = MutableStateFlow(OverviewState())
    val overviewState = _overviewState.asStateFlow()

    private val _overviewBounds = MutableStateFlow(OverviewViewBounds())
    val overviewBounds = _overviewBounds.asStateFlow()

    fun loadOverview(bookmarkId: String, networkAvailable: Boolean) {
        if (_overviewState.value.overview.isNotEmpty() || _overviewState.value.isLoading) return

        scope.launch {
            val (cachedOverview, cachedKeyTakeaways) = withContext(ioDispatcher) {
                localBookmarkDao.getLocalBookmarkOverview(bookmarkId)
            }

            if (!cachedOverview.isNullOrEmpty()) {
                _overviewState.update { state ->
                    state.copy(
                        overview = cachedOverview,
                        keyTakeaways = cachedKeyTakeaways.orEmpty(),
                        isLoading = false,
                    )
                }
                return@launch
            }

            if (!networkAvailable) {
                _overviewState.value = OverviewState(
                    isLoading = false,
                    error = "No network connection"
                )
                return@launch
            }

            _overviewState.value = OverviewState(isLoading = true)

            var fullOverview = ""
            var fullKeyTakeaways = emptyList<String>()

            try {
                apiService.getBookmarkOverview(bookmarkId).collect { response ->
                    when (response) {
                        is OverviewResponse.Overview -> {
                            fullOverview += response.content
                            _overviewState.update { state ->
                                state.copy(overview = fullOverview, isLoading = true)
                            }
                        }

                        is OverviewResponse.KeyTakeaways -> {
                            fullKeyTakeaways = response.content
                            _overviewState.update { state ->
                                state.copy(keyTakeaways = response.content)
                            }
                        }

                        is OverviewResponse.Done -> {
                            _overviewState.update { state ->
                                state.copy(isLoading = false)
                            }

                            if (fullOverview.isNotEmpty()) {
                                withContext(ioDispatcher) {
                                    val keyTakeawaysJson = if (fullKeyTakeaways.isNotEmpty()) {
                                        Json.encodeToString(fullKeyTakeaways)
                                    } else {
                                        null
                                    }
                                    localBookmarkDao.updateLocalBookmarkOverview(
                                        bookmarkId = bookmarkId,
                                        overview = fullOverview,
                                        keyTakeaways = keyTakeawaysJson
                                    )
                                }
                            }
                        }

                        is OverviewResponse.Error -> {
                            _overviewState.update { state ->
                                state.copy(error = response.message, isLoading = false)
                            }
                        }

                        else -> {}
                    }
                }
            } catch (e: Exception) {
                _overviewState.update { state ->
                    state.copy(error = e.message ?: "Unknown error", isLoading = false)
                }
            }
        }
    }

    fun sendOpenMetrics() {
        scope.launch { apiService.sendMetrics(MetricsType.OVERVIEW) }
    }

    fun updateBounds(bounds: OverviewViewBounds) {
        if (_overviewBounds.value != bounds) {
            _overviewBounds.value = bounds
        }
    }

    fun reset() {
        _overviewState.value = OverviewState()
        _overviewBounds.value = OverviewViewBounds()
    }
}
