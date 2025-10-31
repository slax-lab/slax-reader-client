package com.slax.reader.ui.inbox.compenents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.slax.reader.ui.inbox.InboxListViewModel
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_cell_internet


@Composable
fun ArticleList(
    navCtrl: NavController,
    viewModel: InboxListViewModel,
) {
    println("[watch][UI] recomposition ArticleList")

    val bookmarks by viewModel.bookmarks.collectAsState()
    val iconResource = painterResource(Res.drawable.ic_cell_internet)
    val iconPainter = remember { iconResource }
    val lazyListState = rememberLazyListState()
    val dividerLine: @Composable () -> Unit = remember {
        { DividerLine() }
    }

    val isScrolling by remember {
        derivedStateOf { lazyListState.isScrollInProgress }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                return if (isScrolling) {
                    Offset(available.x, 0f)
                } else Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                return Offset(available.x, 0f)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().nestedScroll(nestedScrollConnection),
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
            )

            if (index < bookmarks.lastIndex) {
                dividerLine()
            }
        }
    }
}
