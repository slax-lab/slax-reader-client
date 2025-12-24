package com.slax.reader.ui.bookmark

import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import com.slax.reader.data.database.model.UserBookmark
import com.slax.reader.ui.bookmark.components.BookmarkAlertDialog
import com.slax.reader.ui.bookmark.components.DetailScreenSkeleton
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

data class OverviewViewBounds(
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f
)

data class DetailScreenState(
    val overviewBounds: OverviewViewBounds,
    val onOverviewBoundsChanged: (OverviewViewBounds) -> Unit
)

@Composable
fun DetailScreen(bookmarkId: String, onBackClick: (() -> Unit)) {
    val detailView = koinViewModel<BookmarkDetailViewModel>()
    val backClickHandle = remember { onBackClick }

    var bookmarkDetail by remember { mutableStateOf<UserBookmark?>(null) }
    var htmlContent by remember { mutableStateOf<String?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(bookmarkId) {
        detailView.setBookmarkId(bookmarkId)

        detailView.viewModelScope.launch {
            detailView.bookmarkDetail.collect { details ->
                bookmarkDetail = details.firstOrNull()
            }
        }
        detailView.viewModelScope.runCatching {
            htmlContent = detailView.getBookmarkContent(bookmarkId)
        }.onFailure { e ->
            loadError = e.message ?: "加载失败"
        }
    }

    if (bookmarkDetail == null || htmlContent == null) {
        DetailScreenSkeleton()
        return
    }

    if (loadError != null) {
        BookmarkAlertDialog(loadError!!, onBackClick)
        return
    }

    var overviewBounds by remember { mutableStateOf(OverviewViewBounds()) }

    val screenState = remember {
        DetailScreenState(
            overviewBounds = OverviewViewBounds(),
            onOverviewBoundsChanged = { bounds -> overviewBounds = bounds }
        )
    }

    DetailScreen(
        detailViewModel = detailView,
        detail = bookmarkDetail!!,
        htmlContent = htmlContent!!,
        screenState = screenState.copy(overviewBounds = overviewBounds),
        onBackClick = backClickHandle,
    )
}

@Composable
expect fun DetailScreen(
    detailViewModel: BookmarkDetailViewModel,
    detail: UserBookmark,
    htmlContent: String,
    screenState: DetailScreenState,
    onBackClick: (() -> Unit),
)
