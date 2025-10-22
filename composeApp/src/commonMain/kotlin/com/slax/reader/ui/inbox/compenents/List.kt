package com.slax.reader.ui.inbox.compenents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.slax.reader.ui.inbox.InboxListViewModel
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.inbox_internet
import slax_reader_client.composeapp.generated.resources.inbox_list_more


@Composable
fun ArticleList(navCtrl: NavController) {
    val viewModel: InboxListViewModel = koinInject()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val iconResource = painterResource(Res.drawable.inbox_internet)
    val moreResource = painterResource(Res.drawable.inbox_list_more)
    val iconPainter = remember { iconResource }
    val morePainter = remember { moreResource }
    val dividerLine: @Composable () -> Unit = remember {
        { DividerLine() }
    }

    // println("[watch][UI] recomposition ArticleList")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            },
        verticalArrangement = Arrangement.spacedBy(0.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        itemsIndexed(
            items = bookmarks,
            key = { _, bookmark -> bookmark.id },
            contentType = { _, _ -> "bookmark" }
        ) { index, bookmark ->
            BookmarkItemRow(navCtrl, bookmark, iconPainter, morePainter)

            if (index < bookmarks.lastIndex) {
                dividerLine()
            }
        }
    }
}
