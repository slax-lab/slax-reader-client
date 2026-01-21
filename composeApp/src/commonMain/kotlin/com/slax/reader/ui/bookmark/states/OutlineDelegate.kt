package com.slax.reader.ui.bookmark.states

import com.slax.reader.data.database.dao.LocalBookmarkDao
import com.slax.reader.data.network.ApiService
import com.slax.reader.data.network.dto.OutlineResponse
import com.slax.reader.utils.MarkdownHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

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
    private val localBookmarkDao: LocalBookmarkDao,
    private val apiService: ApiService,
    private val scope: CoroutineScope
) {
    private val _outlineState = MutableStateFlow(OutlineState())
    val outlineState = _outlineState.asStateFlow()

    private val _dialogStatus = MutableStateFlow(OutlineDialogStatus.NONE)
    val dialogStatus = _dialogStatus.asStateFlow()

    fun loadOutline(bookmarkId: String) {
        if (_outlineState.value.isLoading) {
            return
        }

        scope.launch {
            val cacheOutline = withContext(Dispatchers.IO) {
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
                                withContext(Dispatchers.IO) {
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

    fun showDialog() {
        transitionTo(OutlineDialogStatus.EXPANDED)
    }

    fun expandDialog() {
        transitionTo(OutlineDialogStatus.EXPANDED)
    }

    fun collapseDialog() {
        transitionTo(OutlineDialogStatus.COLLAPSED)
    }

    fun hideDialog() {
        _dialogStatus.value = OutlineDialogStatus.HIDDEN
    }

    fun reset() {
        _outlineState.value = OutlineState()
        _dialogStatus.value = OutlineDialogStatus.NONE
    }

    private fun transitionTo(target: OutlineDialogStatus) {
        if (_dialogStatus.value == OutlineDialogStatus.NONE) {
            _dialogStatus.value = OutlineDialogStatus.HIDDEN
            scope.launch {
                yield()
                _dialogStatus.value = target
            }
        } else {
            _dialogStatus.value = target
        }
    }
}
