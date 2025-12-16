package com.slax.reader.ui.bookmark.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.slax.reader.ui.bookmark.CommentViewModel
import org.koin.compose.koinInject

@Composable
fun CommentSidebar(bookmarkId: String) {
    val bookmarkViewModel: CommentViewModel = koinInject()

    LaunchedEffect(bookmarkId) {
        bookmarkViewModel.setBookmarkId(bookmarkId)
    }

    val status by bookmarkViewModel.status.collectAsState()
    val comments by bookmarkViewModel.comments.collectAsState()

    if (status == null || !status!!.subscription.hasSynced) {
        return
    }

    println("============= comment ${comments.size} $comments")
}
