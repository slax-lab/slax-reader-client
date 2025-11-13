package com.slax.reader.ui.inbox.compenents

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.slax.reader.data.database.model.InboxListBookmarkItem
import com.slax.reader.ui.inbox.InboxListViewModel
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_cell_internet

@Composable
fun ArticleList(
    navCtrl: NavController,
    viewModel: InboxListViewModel,
    onEditTitle: (InboxListBookmarkItem) -> Unit,
) {
    println("[watch][UI] recomposition ArticleList")

    val bookmarks by viewModel.bookmarks.collectAsState()
    val iconResource = painterResource(Res.drawable.ic_cell_internet)
    val iconPainter = remember { iconResource }
    val lazyListState = rememberLazyListState()
    val dividerLine: @Composable () -> Unit = remember {
        { DividerLine() }
    }

    Box(
        Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(bottom = 0.dp),
            state = lazyListState
        ) {
            itemsIndexed(
                items = bookmarks,
                key = { _, bookmark -> bookmark.id },
                contentType = { _, _ -> "bookmark" }
            ) { index, bookmark ->
                BookmarkItemRow(
                    navCtrl = navCtrl,
                    viewModel = viewModel,
                    bookmark = bookmark,
                    iconPainter = iconPainter,
                    onEditTitle = onEditTitle
                )

                if (index < bookmarks.lastIndex) {
                    dividerLine()
                }
            }
        }

        EdgeSwipeDetector(
            modifier = Modifier
                .width(50.dp)
                .fillMaxHeight()
                .align(Alignment.TopStart),
        )
    }
}

@Composable
fun EdgeSwipeDetector(
    modifier: Modifier = Modifier,
) {
    var startX by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .background(Color.Transparent)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    startX = down.position.x

                    // 只在左边缘 50dp 内才处理
                    if (startX <= 50.dp.toPx()) {
                        var isRightSwipe = false

                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.first()
                            val dragAmount = change.position.x - change.previousPosition.x

                            if (dragAmount > 0 && !isRightSwipe) {
                                isRightSwipe = true
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
            }
    )
}
