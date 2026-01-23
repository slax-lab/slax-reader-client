package com.slax.reader.ui.bookmark.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.slax.reader.const.component.EditNameDialog
import com.slax.reader.ui.bookmark.BookmarkDetailViewModel
import com.slax.reader.ui.bookmark.states.BookmarkOverlay
import com.slax.reader.utils.i18n
import com.slax.reader.utils.platformType
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
            when (platformType) {
                "android" -> {
                    AlertDialog(
                        onDismissRequest = {
                            viewModel.overlayDelegate.dismissOverlay(BookmarkOverlay.SubscriptionRequired)
                        },
                        containerColor = Color.White,
                        title = {
                            Text(
                                text = "subscription_required_title".i18n(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        text = {
                            Text(
                                text = "subscription_required_message".i18n(),
                                fontSize = 16.sp,
                                lineHeight = 24.sp
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.overlayDelegate.dismissOverlay(BookmarkOverlay.SubscriptionRequired)
                                }
                            ) {
                                Text(
                                    text = "subscription_required_btn_ok".i18n(),
                                    color = Color(0xFF16b998),
                                    fontSize = 16.sp
                                )
                            }
                        }
                    )
                }
                "ios" -> {
                    LaunchedEffect(Unit) {
                        viewModel.requestNavigateToSubscription()
                        viewModel.overlayDelegate.dismissOverlay(BookmarkOverlay.SubscriptionRequired)
                    }
                }
            }
        }
        BookmarkOverlay.FeedbackRequired -> {
            LaunchedEffect(Unit) {
                viewModel.requestNavigateToFeedback()
                viewModel.overlayDelegate.dismissOverlay(BookmarkOverlay.FeedbackRequired)
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
