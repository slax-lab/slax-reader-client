package com.slax.reader.ui.bookmark.components

import androidx.compose.runtime.*
import com.slax.reader.const.component.EditNameDialog
import com.slax.reader.ui.bookmark.BookmarkDetailViewModel
import com.slax.reader.ui.bookmark.states.BookmarkOverlay
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BookmarkDetailOverlays() {
    println("[watch][UI] recomposition BookmarkDetailOverlays")
    val viewModel = koinViewModel<BookmarkDetailViewModel>()

    val currentOverlay by viewModel.overlayDelegate.overlay.collectAsState()
    val imageViewerState by viewModel.overlayDelegate.imageViewerState.collectAsState()

    when (currentOverlay) {
        BookmarkOverlay.Tags -> {
            TagsManageBottomSheet(enableDrag = false)
        }
        BookmarkOverlay.Overview -> {
            OverviewDialog(
                sourceBounds = viewModel.overviewDelegate.overviewBounds.value
            )
        }
        BookmarkOverlay.Toolbar -> {
            BottomToolbarSheet()
        }
        BookmarkOverlay.EditTitle -> {
            EditNameDialog(
                initialTitle = viewModel.bookmarkDelegate.bookmarkDetailState.value.displayTitle,
                onConfirm = { title ->
                    viewModel.bookmarkDelegate.onUpdateBookmarkTitle(title)
                    viewModel.overlayDelegate.dismissOverlay(BookmarkOverlay.EditTitle)
                },
                onDismissRequest = { viewModel.overlayDelegate.dismissOverlay(BookmarkOverlay.EditTitle) }
            )
        }
        BookmarkOverlay.SubscriptionRequired -> {
            LaunchedEffect(Unit) {
                viewModel.requestNavigateToSubscription()
                viewModel.overlayDelegate.dismissOverlay(BookmarkOverlay.SubscriptionRequired)
            }
        }
        null -> {
        }
    }

    if (imageViewerState != null) {
        ImageViewer(
            imageUrls = imageViewerState!!.allImageUrls,
            initialImageUrl = imageViewerState!!.currentImageUrl,
            onDismiss = {
                viewModel.overlayDelegate.dismissImageViewer()
            }
        )
    }
}
