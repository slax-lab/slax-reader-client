package com.slax.reader.ui.inbox.compenents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

            dividerLine()
        }


        if (bookmarks.isEmpty()) {
            return@LazyColumn
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 34.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "没有更多了", style = TextStyle(
                        color = Color(0xFF999999),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                )
            }
        }
    }
}
