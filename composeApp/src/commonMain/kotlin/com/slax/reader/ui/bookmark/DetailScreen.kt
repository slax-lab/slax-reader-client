package com.slax.reader.ui.bookmark

import androidx.compose.runtime.*
import com.slax.reader.ui.bookmark.components.CommentSidebar
import com.slax.reader.ui.bookmark.components.DetailScreenSkeleton
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DetailScreen(bookmarkId: String, onBackClick: (() -> Unit), onNavigateToSubscription: (() -> Unit)? = null) {
    val detailView = koinViewModel<BookmarkDetailViewModel>()

    LaunchedEffect(bookmarkId) {
        detailView.bind(bookmarkId)
    }

    LaunchedEffect(detailView) {
        detailView.effects.collect { effect ->
            when (effect) {
                BookmarkDetailEffect.NavigateBack -> onBackClick()
                BookmarkDetailEffect.NavigateToSubscription -> onNavigateToSubscription?.invoke()
                else -> {}
            }
        }
    }

    val contentState by detailView.contentState.collectAsState()

    if (contentState.htmlContent == null || contentState.isLoading) {
        DetailScreenSkeleton()
        return
    }

    CommentSidebar(bookmarkId)
    DetailScreen(htmlContent = contentState.htmlContent!!)
}

@Composable
expect fun DetailScreen(htmlContent: String)
