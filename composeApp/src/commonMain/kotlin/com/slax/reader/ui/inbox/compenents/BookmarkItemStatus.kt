package com.slax.reader.ui.inbox.compenents

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
    
    Box(
        modifier = Modifier
            .size(18.dp)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val downloadStatus by remember(bookmarkId) {
            derivedStateOf {
                val status = viewModel.localBookmarkMap.value[bookmarkId]?.downloadStatus
                if (status != null && status != 0) status else null
            }
        }

        val isDownloading = downloadStatus == 1
        val isCompleted = downloadStatus == 2

        Surface(
            modifier = Modifier.size(18.dp),
            shape = RoundedCornerShape(50),
            color = Color(if (isCompleted) 0xFFC4C4C2 else 0xFFF5F5F3)
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