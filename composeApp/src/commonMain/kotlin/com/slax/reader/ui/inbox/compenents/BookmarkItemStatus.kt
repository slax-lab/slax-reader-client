package com.slax.reader.ui.inbox.compenents

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.slax.reader.data.database.model.BookmarkSortType
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_cell_archived
import slax_reader_client.composeapp.generated.resources.ic_cell_internet
import slax_reader_client.composeapp.generated.resources.ic_cell_starred

@Composable
fun ItemStatus(downloadStatus: Int, sortType: BookmarkSortType = BookmarkSortType.UPDATED) {
    val iconPainter = painterResource(
        when (sortType) {
            BookmarkSortType.STARRED -> Res.drawable.ic_cell_starred
            BookmarkSortType.ARCHIVED -> Res.drawable.ic_cell_archived
            else -> Res.drawable.ic_cell_internet
        }
    )

    Box(
        modifier = Modifier
            .size(18.dp)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(18.dp),
            shape = RoundedCornerShape(50),
            color = Color(0xFFF5F5F3),
            border = BorderStroke(0.5.dp, if (downloadStatus == 2) Color(0xFFC4C4C2) else Color.Transparent)
        ) {}

        Image(
            painter = iconPainter,
            contentDescription = "Article",
            modifier = Modifier.size(12.dp),
            contentScale = ContentScale.Fit
        )
    }
}