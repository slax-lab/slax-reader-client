package com.slax.reader.ui.bookmark.states

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ImageViewerState(
    val currentImageUrl: String,
    val allImageUrls: List<String>,
)

sealed interface BookmarkOverlay {
    data object Tags : BookmarkOverlay
    data object Overview : BookmarkOverlay
    data object Toolbar : BookmarkOverlay
    data object EditTitle : BookmarkOverlay
    data object SubscriptionRequired : BookmarkOverlay

    data object FeedbackRequired : BookmarkOverlay
}

class OverlayDelegate {
    private val _overlay = MutableStateFlow<BookmarkOverlay?>(null)
    val overlay = _overlay.asStateFlow()

    private val _imageViewerState = MutableStateFlow<ImageViewerState?>(null)
    val imageViewerState = _imageViewerState.asStateFlow()

    fun showOverlay(overlay: BookmarkOverlay) {
        _overlay.value = overlay
    }

    fun dismissOverlay(overlay: BookmarkOverlay? = null) {
        if (overlay == null || _overlay.value == overlay) {
            _overlay.value = null
        }
    }

    fun onWebViewImageClick(src: String, allImages: List<String>) {
        _imageViewerState.value = ImageViewerState(currentImageUrl = src, allImageUrls = allImages)
    }

    fun dismissImageViewer() {
        _imageViewerState.value = null
    }

    fun reset() {
        _overlay.value = null
        _imageViewerState.value = null
    }
}
