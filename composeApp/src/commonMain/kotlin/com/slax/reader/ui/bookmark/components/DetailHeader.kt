package com.slax.reader.ui.bookmark.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slax.reader.data.database.model.UserBookmark
import com.slax.reader.ui.bookmark.BookmarkDetailViewModel
import com.slax.reader.ui.bookmark.OverviewViewBounds

@Composable
fun HeaderContent(
    detailView: BookmarkDetailViewModel,
    detail: UserBookmark,
    onHeightChanged: (Float) -> Unit,
    onTagClick: () -> Unit,
    onOverviewExpand: () -> Unit,
    onOverviewBoundsChanged: (OverviewViewBounds) -> Unit
) {
    println("[watch][UI] recomposition HeaderContent")

    val displayTitle = remember(detail.displayTitle) { detail.displayTitle }
    val displayTime = remember(detail.displayTime) { detail.displayTime }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .onGloballyPositioned { coordinates ->
                    val newHeight = coordinates.size.height.toFloat()
                    onHeightChanged(newHeight)
                }
        ) {
            Spacer(modifier = Modifier.height(44.dp))

            Text(
                text = displayTitle,
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 30.sp,
                    color = Color(0xFF0f1419)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = displayTime,
                    style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, color = Color(0xFF999999))
                )
                Text(
                    "查看原网页",
                    modifier = Modifier.padding(start = 16.dp).clickable {
                        println("被点击了")
                    },
                    style = TextStyle(color = Color(0xFF5490C2), fontSize = 14.sp, lineHeight = 20.sp)
                )
            }

            TagsView(
                modifier = Modifier.padding(top = 16.dp),
                onAddTagClick = onTagClick,
                detailView = detailView
            )

            OverviewView(
                detailView = detailView,
                modifier = Modifier.padding(top = 20.dp),
                onExpand = onOverviewExpand,
                onBoundsChanged = onOverviewBoundsChanged
            )
        }
    }
}