package com.slax.reader.ui.bookmark.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
actual fun ImageShower(
    url: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale,
    onImageSize: ((Size) -> Unit)?
) {
    val context = LocalPlatformContext.current
    val imageRequest = remember(url, context) {
        ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .build()
    }

    AsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        contentScale = contentScale,
        onState = { state ->
            if (state is AsyncImagePainter.State.Success) {
                val intrinsicSize = state.painter.intrinsicSize
                if (intrinsicSize.width > 0f && intrinsicSize.height > 0f) {
                    onImageSize?.invoke(intrinsicSize)
                }
            }
        },
        modifier = modifier
    )
}
