package com.slax.reader.ui.inbox.compenents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.slax.reader.ui.inbox.InboxListViewModel
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_cell_internet
import slax_reader_client.composeapp.generated.resources.ic_cell_more


@Composable
fun ArticleList(navCtrl: NavController) {
    val viewModel: InboxListViewModel = koinInject()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val bookmarkStatusMap by viewModel.bookmarkStatusFlow.collectAsState()
    val iconResource = painterResource(Res.drawable.ic_cell_internet)
    val moreResource = painterResource(Res.drawable.ic_cell_more)
    val iconPainter = remember { iconResource }
    val morePainter = remember { moreResource }
    val dividerLine: @Composable () -> Unit = remember {
        { DividerLine() }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        contentPadding = PaddingValues(bottom = 0.dp),
    ) {
        itemsIndexed(
            items = bookmarks,
            key = { _, bookmark -> bookmark.id },
            contentType = { _, _ -> "bookmark" }
        ) { index, bookmark ->
            val bookmarkStatus = remember {
                derivedStateOf { bookmarkStatusMap[bookmark.id]?.status }
            }.value
            BookmarkItemRow(
                navCtrl = navCtrl,
                bookmark = bookmark,
                iconPainter = iconPainter,
                morePainter = morePainter,
                downloadStatus = bookmarkStatus
            )

            if (index < bookmarks.lastIndex) {
                dividerLine()
            }
        }
    }
}
