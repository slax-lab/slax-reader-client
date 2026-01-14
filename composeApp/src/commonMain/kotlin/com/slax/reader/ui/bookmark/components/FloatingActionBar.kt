package com.slax.reader.ui.bookmark.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.slax.reader.data.database.model.UserBookmark
import com.slax.reader.ui.bookmark.BookmarkDetailViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.*

@Composable
fun FloatingActionBar(
    detail: UserBookmark,
    detailView: BookmarkDetailViewModel,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    onMoreClick: () -> Unit = {}
) {
    println("[watch][UI] recomposition FloatingActionBar")

    val density = LocalDensity.current
    val hiddenOffsetPx = remember(density) { with(density) { 150.dp.toPx() } }
    val translationY = remember { Animatable(if (visible) 0f else hiddenOffsetPx) }

    LaunchedEffect(visible) {
        translationY.animateTo(
            targetValue = if (visible) 0f else hiddenOffsetPx,
            animationSpec = tween(durationMillis = 300)
        )
    }

    Box(
        modifier = modifier.graphicsLayer {
            this.translationY = translationY.value
        }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .dropShadow(
                        shape = RoundedCornerShape(25.dp),
                        shadow = Shadow(
                            radius = 40.dp,
                            spread = 0.dp,
                            color = Color.Black.copy(alpha = 0.25f),
                            offset = DpOffset(x = 0.dp, y = 10.dp)
                        )
                    )
                    .clip(RoundedCornerShape(25.dp))
                    .border(
                        width = 1.dp,
                        color = Color.White,
                        shape = RoundedCornerShape(25.dp)
                    )
                    .background(Color(0xFFFFFFFF))
            ) {
                Surface(
                    onClick = {
                        detailView.viewModelScope.launch {
                            detailView.toggleStar(detail.isStarred != 1)
                        }
                    },
                    modifier = Modifier
                        .size(50.dp),
                    color = Color.Transparent,
                ) {
                    Box(
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Icon(
                            painter = painterResource(
                                if (detail.isStarred == 1)
                                    Res.drawable.ic_floating_panel_starred
                                else Res.drawable.ic_floating_panel_star
                            ),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.padding(start = 17.5.dp).size(20.dp)
                        )
                    }
                }

                Surface(
                    onClick = {
                        detailView.viewModelScope.launch {
                            detailView.toggleArchive(detail.archiveStatus != 1)
                        }
                    },
                    modifier = Modifier
                        .size(50.dp),
                    color = Color.Transparent,
                ) {
                    Box(
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(
                            painter = painterResource(if (detail.archiveStatus == 1) Res.drawable.ic_floating_panel_archieved else Res.drawable.ic_floating_panel_archieve),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.padding(end = 17.5.dp).size(20.dp)
                        )
                    }
                }
            }

            Box(modifier = Modifier.width(12.dp))

            Surface(
                onClick = onMoreClick,
                modifier = Modifier
                    .size(50.dp)
                    .dropShadow(
                        shape = RoundedCornerShape(25.dp),
                        shadow = Shadow(
                            radius = 40.dp,
                            spread = 0.dp,
                            color = Color.Black.copy(alpha = 0.25f),
                            offset = DpOffset(x = 0.dp, y = 10.dp)
                        )
                    ),
                color = Color(0xFFFFFFFF),
                shape = RoundedCornerShape(25.dp),
                border = BorderStroke(1.dp, Color.White)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_floating_panel_more),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
