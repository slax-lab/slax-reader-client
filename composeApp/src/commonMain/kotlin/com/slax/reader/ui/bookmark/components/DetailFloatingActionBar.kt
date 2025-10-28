package com.slax.reader.ui.bookmark.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.slax.reader.data.database.model.UserBookmark
import com.slax.reader.ui.bookmark.BookmarkDetailViewModel

@Composable
fun BoxScope.DetailFloatingActionBar(
    detail: UserBookmark,
    detailView: BookmarkDetailViewModel,
    manuallyVisible: Boolean,
    onMoreClick: () -> Unit,
) {
    println("[watch][UI] recomposition DetailFloatingActionBar")

    val density = LocalDensity.current
    val hiddenOffsetPx = remember(density) { with(density) { 150.dp.toPx() } }

    val translationY = remember(hiddenOffsetPx) { Animatable(if (manuallyVisible) 0f else hiddenOffsetPx) }

    LaunchedEffect(manuallyVisible, hiddenOffsetPx) {
        translationY.animateTo(
            targetValue = if (manuallyVisible) 0f else hiddenOffsetPx,
            animationSpec = tween(durationMillis = 300)
        )
    }

    FloatingActionBar(
        detail = detail,
        detailView = detailView,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 58.dp)
            .graphicsLayer {
                this.translationY = translationY.value
            },
        onMoreClick = onMoreClick
    )
}
