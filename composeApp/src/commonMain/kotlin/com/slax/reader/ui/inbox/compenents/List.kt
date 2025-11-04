package com.slax.reader.ui.inbox.compenents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
}
