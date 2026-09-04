package com.slax.reader.ui.inbox.compenents

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
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.slax.reader.data.database.model.BookmarkSortType
import com.slax.reader.domain.sync.DownloadStatus
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_cell_archived
import slax_reader_client.composeapp.generated.resources.ic_cell_internet
import slax_reader_client.composeapp.generated.resources.ic_cell_internet_downloading
import slax_reader_client.composeapp.generated.resources.ic_cell_internet_uncached
import slax_reader_client.composeapp.generated.resources.ic_cell_starred

internal enum class ItemDownloadState {
    NONE,
    DOWNLOADING,
    COMPLETED,
    FAILED,
}

internal fun itemDownloadState(downloadStatus: Int): ItemDownloadState = when (downloadStatus) {
    DownloadStatus.DOWNLOADING.code -> ItemDownloadState.DOWNLOADING
    DownloadStatus.COMPLETED.code -> ItemDownloadState.COMPLETED
    DownloadStatus.FAILED.code -> ItemDownloadState.FAILED
    else -> ItemDownloadState.NONE
}

private val DownloadStateKey = SemanticsPropertyKey<String>("DownloadState")

@Composable
fun ItemStatus(downloadStatus: Int, sortType: BookmarkSortType = BookmarkSortType.UPDATED) {
    val itemState = itemDownloadState(downloadStatus)
    val iconPainter = painterResource(
        when (sortType) {
            BookmarkSortType.STARRED -> Res.drawable.ic_cell_starred
            BookmarkSortType.ARCHIVED -> Res.drawable.ic_cell_archived
            else -> when (itemState) {
                ItemDownloadState.DOWNLOADING -> Res.drawable.ic_cell_internet_downloading
                ItemDownloadState.COMPLETED -> Res.drawable.ic_cell_internet
                ItemDownloadState.NONE, ItemDownloadState.FAILED -> Res.drawable.ic_cell_internet_uncached
            }
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
        ) {}

        Image(
            painter = iconPainter,
            contentDescription = "Article",
            modifier = Modifier
                .size(12.dp)
                .semantics { this[DownloadStateKey] = itemState.name },
            contentScale = ContentScale.Fit
        )
    }
}
