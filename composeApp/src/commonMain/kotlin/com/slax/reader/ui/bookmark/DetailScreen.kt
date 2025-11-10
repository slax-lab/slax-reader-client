package com.slax.reader.ui.bookmark

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.slax.reader.data.database.model.UserBookmark
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

    LaunchedEffect(bookmarkId) {
        detailView.setBookmarkId(bookmarkId)
    }

    val details by detailView.bookmarkDetail.collectAsState()
    val detail = details.firstOrNull()

    var overviewBounds by remember { mutableStateOf(OverviewViewBounds()) }

    val screenState = remember {
        DetailScreenState(
            overviewBounds = OverviewViewBounds(),
            onOverviewBoundsChanged = { bounds -> overviewBounds = bounds }
        )
    }

    if (detail == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF16B998))
        }
        return
    }

    DetailScreen(
        detailViewModel = detailView,
        detail = detail,
        screenState = screenState.copy(overviewBounds = overviewBounds),
        onBackClick = backClickHandle
    )
}

@Composable
expect fun DetailScreen(
    detailViewModel: BookmarkDetailViewModel,
    detail: UserBookmark,
    screenState: DetailScreenState,
    onBackClick: (() -> Unit)
)
