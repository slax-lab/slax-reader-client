package com.slax.reader.ui.inbox.compenents

import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.slax.reader.const.BookmarkRoutes
import com.slax.reader.data.database.model.InboxListBookmarkItem
import com.slax.reader.domain.sync.DownloadStatus


@Composable
fun BookmarkItemRow(
    navCtrl: NavController,
    bookmark: InboxListBookmarkItem,
    iconPainter: Painter,
    morePainter: Painter,
    downloadStatus: DownloadStatus?,
) {
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .combinedClickable(
                onClick = {
                    navCtrl.navigate(BookmarkRoutes(bookmarkId = bookmark.id))
                },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
//                    contextMenuPhotoId = photo.id
                },
//                onLongClickLabel = stringResource(R.string.open_context_menu)
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(24.dp)) {
                Image(
                    painter = iconPainter,
                    contentDescription = "Article",
                    modifier = Modifier.size(24.dp),
                    contentScale = ContentScale.Fit
                )

                if (downloadStatus == DownloadStatus.DOWNLOADING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF1DA1F2),
                        strokeWidth = 2.dp,
                        trackColor = Color.Transparent
                    )
                }
            }

            Text(
                text = bookmark.displayTitle(),
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Image(
            painter = morePainter,
            contentDescription = "More",
            modifier = Modifier.size(16.dp),
            contentScale = ContentScale.Fit
        )
    }
}
