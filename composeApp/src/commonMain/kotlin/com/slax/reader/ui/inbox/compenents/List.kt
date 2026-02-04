package com.slax.reader.ui.inbox.compenents

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.FrameRateCategory
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.slax.reader.data.database.model.InboxListBookmarkItem
import com.slax.reader.ui.inbox.InboxListViewModel
import com.slax.reader.utils.i18n

@Composable
fun ArticleList(
    navCtrl: NavController,
    viewModel: InboxListViewModel,
    onEditTitle: (InboxListBookmarkItem) -> Unit,
) {
    println("[watch][UI] recomposition ArticleList")

    val bookmarks by viewModel.bookmarks.collectAsState()

    val lazyListState = rememberLazyListState()
    val dividerLine: @Composable () -> Unit = remember {
        { DividerLine() }
    }

    LaunchedEffect(Unit) {
        viewModel.scrollToTopEvent.collect {
            lazyListState.animateScrollToItem(0)
        }
    }

    Box(
        Modifier.fillMaxSize()
    ) {
        if (bookmarks.isEmpty()) {
            EmptyOrLoadingView(viewModel = viewModel)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().preferredFrameRate(FrameRateCategory.High),
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
                        onEditTitle = onEditTitle
                    )

                    dividerLine()
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 34.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "list_no_more".i18n(), style = TextStyle(
                                color = Color(0xFF999999),
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        )
                    }
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
fun EmptyOrLoadingView(viewModel: InboxListViewModel) {
    val isSyncedDataReady by viewModel.isSyncedDataReady.collectAsState()

    if (isSyncedDataReady) {
        EmptyView()
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = Color(0x336A6E83)
            )
        }
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
