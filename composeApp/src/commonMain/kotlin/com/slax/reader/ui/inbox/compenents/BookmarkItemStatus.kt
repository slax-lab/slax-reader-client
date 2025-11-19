package com.slax.reader.ui.inbox.compenents

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.slax.reader.ui.inbox.InboxListViewModel
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_cell_internet

@Composable
fun ItemStatus(bookmarkId: String, viewModel: InboxListViewModel) {
    val iconResource = painterResource(Res.drawable.ic_cell_internet)
    val iconPainter = remember { iconResource }

    val downloadStatus by remember {
        derivedStateOf {
            viewModel.localBookmarkMap.value[bookmarkId]?.downloadStatus
        }
    }

    Box(
        modifier = Modifier
            .size(18.dp)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val isDownloading = downloadStatus == 1
        val isCompleted = downloadStatus == 2

        Surface(
            modifier = Modifier.size(18.dp),
            shape = RoundedCornerShape(50),
            color = Color(0xFFF5F5F3),
            border = BorderStroke(0.5.dp, if (isCompleted) Color(0xFFC4C4C2) else Color.Transparent)
        ) {}

        Image(
            painter = iconPainter,
            contentDescription = "Article",
            modifier = Modifier.size(12.dp),
            contentScale = ContentScale.Fit
        )

        if (isDownloading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = Color(0xFF1DA1F2),
                strokeWidth = 2.dp,
                trackColor = Color.Transparent
            )
        }
    }
}