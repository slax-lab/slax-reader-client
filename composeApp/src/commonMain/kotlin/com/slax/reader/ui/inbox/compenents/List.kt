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
import com.slax.reader.ui.inbox.InboxListViewModel
import com.slax.reader.data.database.model.InboxListBookmarkItem
import com.slax.reader.utils.i18n

@Composable
fun ArticleList(
    navCtrl: NavController,
    viewModel: InboxListViewModel,
    onEditTitle: (InboxListBookmarkItem) -> Unit,
) {
    println("[watch][UI] recomposition ArticleList")

    val groupedItems by viewModel.groupedBookmarks.collectAsState()
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

    if (bookmarks.isEmpty()) {
        val hasSynced by viewModel.hasSynced.collectAsState()
        Box(
            Modifier.fillMaxSize()
        ) {
            EmptyOrLoadingView(hasSynced = hasSynced)
        }
        return
    }

    Box(
        Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().preferredFrameRate(FrameRateCategory.High),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(bottom = 0.dp),
            state = lazyListState
        ) {
            itemsIndexed(
                items = groupedItems,
                key = { _, item ->
                    when (item) {
                        is InboxListItem.BookmarkItem -> item.bookmark.id
                        is InboxListItem.GroupSeparator -> "group_separator_${item.label}"
                    }
                },
                contentType = { _, item ->
                    when (item) {
                        is InboxListItem.BookmarkItem -> "bookmark"
                        is InboxListItem.GroupSeparator -> "separator"
                    }
                }
            ) { index, item ->
                when (item) {
                    is InboxListItem.BookmarkItem -> {
                        BookmarkItemRow(
                            navCtrl = navCtrl,
                            viewModel = viewModel,
                            bookmark = item.bookmark,
                            onEditTitle = onEditTitle
                        )
                        // 下一项是 GroupSeparator 时不加分割线，避免视觉重复
                        val nextItem = groupedItems.getOrNull(index + 1)
                        if (nextItem == null || nextItem is InboxListItem.BookmarkItem) {
                            dividerLine()
                        }
                    }

                    is InboxListItem.GroupSeparator -> {
                        GroupSeparatorRow(label = item.label)
                    }
                }
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

        EdgeSwipeDetector(
            modifier = Modifier
                .width(50.dp)
                .fillMaxHeight()
                .align(Alignment.TopStart),
        )
    }
}

@Composable
fun EmptyOrLoadingView(hasSynced: Boolean) {
    println("[watch][UI] recomposition EmptyOrLoadingView, hasSynced=$hasSynced")
    Box(modifier = Modifier.fillMaxSize()) {
        EmptyView()

        if (!hasSynced) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0x336A6E83)
                )
            }
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
