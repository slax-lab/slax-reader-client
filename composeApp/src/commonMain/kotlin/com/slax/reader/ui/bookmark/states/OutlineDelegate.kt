package com.slax.reader.ui.bookmark.states

import com.slax.reader.data.database.dao.LocalBookmarkRepository
import com.slax.reader.data.network.BookmarkAiApi
import com.slax.reader.data.network.MetricsType
import com.slax.reader.data.network.dto.OutlineResponse
import com.slax.reader.utils.MarkdownHelper
import com.slax.reader.utils.outlineEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


data class OutlineState(
    val outline: String = "",
    val isPending: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null
)

enum class OutlineDialogStatus {
    NONE,
    HIDDEN,
    COLLAPSED,
    EXPANDED
}

class OutlineDelegate(
    private val localBookmarkDao: LocalBookmarkRepository,
    private val apiService: BookmarkAiApi,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    companion object {
        private const val SAVE_DEBOUNCE_MS = 2000L
    }

    private val _outlineState = MutableStateFlow(OutlineState())
    val outlineState = _outlineState.asStateFlow()

    private val _dialogStatus = MutableStateFlow(OutlineDialogStatus.NONE)
    val dialogStatus = _dialogStatus.asStateFlow()

    var savedScrollPosition: Int = 0
        private set

    private var currentBookmarkId: String? = null
    private var currentScrollPosition: Int = -1
    private var saveScrollJob: Job? = null

    fun saveScrollPosition(position: Int) {
        savedScrollPosition = position
        currentScrollPosition = position
        saveScrollJob?.cancel()
        saveScrollJob = scope.launch {
            delay(SAVE_DEBOUNCE_MS)
            val id = currentBookmarkId ?: return@launch
            withContext(ioDispatcher) {
                localBookmarkDao.updateLocalBookmarkOutlineScrollPosition(id, position)
            }
        }
    }

    fun flushScrollPosition() {
        saveScrollJob?.cancel()
        val id = currentBookmarkId ?: return
        val position = currentScrollPosition
        if (position < 0) return
        scope.launch {
        withContext(NonCancellable + ioDispatcher) {
                localBookmarkDao.updateLocalBookmarkOutlineScrollPosition(id, position)
            }
        }
    }

    fun loadOutline(bookmarkId: String, networkAvailable: Boolean) {
        if (_outlineState.value.isLoading) {
            return
        }

        currentBookmarkId = bookmarkId

        scope.launch {
            val savedPos = withContext(ioDispatcher) {
                localBookmarkDao.getLocalBookmarkOutlineScrollPosition(bookmarkId)
            }

            if (savedPos != null && savedPos > 0) {
                savedScrollPosition = savedPos
            }

            val cacheOutline = withContext(ioDispatcher) {
                localBookmarkDao.getLocalBookmarkOutline(bookmarkId)
            }

            if (!cacheOutline.isNullOrEmpty()) {
                val fixedOutline = MarkdownHelper.fixMarkdownLinks(cacheOutline)
                _outlineState.update { state ->
                    state.copy(
                        outline = fixedOutline,
                        isLoading = false
                    )
                }
                return@launch
            }

            if (!networkAvailable) {
                _outlineState.value = OutlineState(
                    isLoading = false,
                    isPending = true,
                    error = "No network connection"
                )
                return@launch
            }

            _outlineState.value = OutlineState(isLoading = true, isPending = true)

            val streamProcessor = MarkdownHelper.createStreamProcessor()

            try {
                apiService.getBookmarkOutline(bookmarkId).collect { response ->
                    when (response) {
                        is OutlineResponse.Outline -> {
                            _outlineState.update { state ->
                                state.copy(
                                    outline = streamProcessor.process(response.content),
                                    isLoading = true,
                                    isPending = false
                                )
                            }
                        }

                        is OutlineResponse.Done -> {
                            val finalOutline = streamProcessor.flush()

                            _outlineState.update { state ->
                                state.copy(outline = finalOutline, isLoading = false)
                            }

                            if (finalOutline.isNotEmpty()) {
                                withContext(ioDispatcher) {
                                    localBookmarkDao.updateLocalBookmarkOutline(
                                        bookmarkId = bookmarkId,
                                        outline = finalOutline
                                    )
                                }
                            }
                        }

                        is OutlineResponse.Error -> {
                            _outlineState.update { state ->
                                state.copy(error = response.message, isLoading = false)
                            }
                        }

                        else -> {}
                    }
                }
            } catch (e: Exception) {
                _outlineState.update { state ->
                    state.copy(error = e.message ?: "Unknown error", isLoading = false)
                }
            }
        }
    }

    fun showCollapsed() {
        if (_dialogStatus.value == OutlineDialogStatus.NONE) {
            _dialogStatus.value = OutlineDialogStatus.COLLAPSED
        }
    }

    fun showDialog() {
        transitionTo(OutlineDialogStatus.EXPANDED)
        scope.launch { apiService.sendMetrics(MetricsType.SUMMARY) }
    }

    fun expandDialog() {
        val previousStatus = _dialogStatus.value
        transitionTo(OutlineDialogStatus.EXPANDED)

        when (previousStatus) {
            OutlineDialogStatus.COLLAPSED -> {
                outlineEvent.action("interact", "expand").send()
            }
            OutlineDialogStatus.NONE, OutlineDialogStatus.HIDDEN -> {
                outlineEvent.action("interact", "open").send()
            }
            else -> {}
        }
    }

    fun collapseDialog() {
        transitionTo(OutlineDialogStatus.COLLAPSED)
        outlineEvent.action("interact", "collapse").send()
    }

    fun hideDialog() {
        _dialogStatus.value = OutlineDialogStatus.HIDDEN
        outlineEvent.action("interact", "close").send()
    }

    fun reset() {
        flushScrollPosition()
        _outlineState.value = OutlineState()
        _dialogStatus.value = OutlineDialogStatus.NONE
        savedScrollPosition = 0
        currentScrollPosition = -1
        currentBookmarkId = null
    }

    private fun transitionTo(target: OutlineDialogStatus) {
        _dialogStatus.value = target
    }
}
