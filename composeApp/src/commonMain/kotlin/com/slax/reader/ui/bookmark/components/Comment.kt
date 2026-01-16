package com.slax.reader.ui.bookmark.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.slax.reader.ui.bookmark.BookmarkDetailViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CommentSidebar(bookmarkId: String) {
    val viewModel = koinViewModel<BookmarkDetailViewModel>()
    val commentState by viewModel.commentDelegate.commentState.collectAsState()

    LaunchedEffect(bookmarkId) {
        viewModel.commentDelegate.bind(bookmarkId)
    }

    when {
        commentState.isDone -> {
            println("============= comment ${commentState.comments.size} ${commentState.comments}")
        }
        commentState.isLoading -> {
            println("============= comment loading...")
        }
        commentState.errStr != null -> {
            println("============= comment error: ${commentState.errStr}")
        }
    }
}
