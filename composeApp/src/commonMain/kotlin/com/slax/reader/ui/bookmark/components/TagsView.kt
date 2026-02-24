package com.slax.reader.ui.bookmark.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.slax.reader.ui.bookmark.BookmarkDetailViewModel
import com.slax.reader.ui.bookmark.states.BookmarkOverlay
import com.slax.reader.utils.bookmarkEvent
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_xs_yellow_plus

@Composable
fun TagsView(
    modifier: Modifier = Modifier,
) {
    println("[watch][UI] recomposition TagsView")
    val viewModel = koinViewModel<BookmarkDetailViewModel>()
    val currentTags by viewModel.bookmarkDelegate.selectedTagList.collectAsState()

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        currentTags.forEach { tag ->
            key(tag.id) {
                TagItem(
                    tag = tag.tag_name,
                    onClick = { }
                )
            }
        }

        Box(
            modifier = Modifier
                .size(21.dp)
                .clip(RoundedCornerShape(3.dp))
                .border(
                    width = 1.dp,
                    color = Color(0xFFE4D6BA),
                    shape = RoundedCornerShape(3.dp)
                ).then(
                    Modifier.clickable(
                        onClick = {
                            viewModel.overlayDelegate.showOverlay(BookmarkOverlay.Tags)
                            bookmarkEvent.action("use_tag").send()
                        },
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_xs_yellow_plus),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(8.dp)
            )
        }
    }
}
