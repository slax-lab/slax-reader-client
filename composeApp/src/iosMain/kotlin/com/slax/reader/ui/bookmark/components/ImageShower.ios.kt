package com.slax.reader.ui.bookmark.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.UIKitView
import cocoapods.SDWebImage.SDWebImageManager
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.Foundation.NSURL
import platform.UIKit.UIColor
import platform.UIKit.UIImageView
import platform.UIKit.UIViewContentMode

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun ImageShower(
    url: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale,
    onImageSize: ((Size) -> Unit)?
) {
    val imageView = remember { UIImageView() }

    DisposableEffect(contentScale) {
        imageView.contentMode = when (contentScale) {
            ContentScale.Fit -> UIViewContentMode.UIViewContentModeScaleAspectFit
            ContentScale.Crop -> UIViewContentMode.UIViewContentModeScaleAspectFill
            ContentScale.FillBounds -> UIViewContentMode.UIViewContentModeScaleToFill
            else -> UIViewContentMode.UIViewContentModeScaleAspectFit
        }
        imageView.clipsToBounds = true
        imageView.backgroundColor = UIColor.blackColor
        onDispose { }
    }

    LaunchedEffect(url) {
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl != null) {
            SDWebImageManager.sharedManager.loadImageWithURL(
                nsUrl,
                options = 0u,
                progress = null
            ) { image, data, error, _, _, _ ->
                if (error == null && image != null) {
                    imageView.image = image

                    val size = image.size
                    onImageSize?.invoke(
                        Size(
                            size.useContents { width.toFloat() },
                            size.useContents { height.toFloat() }
                        )
                    )
                }
            }
        }
    }

    UIKitView(
        factory = { imageView },
        modifier = modifier
    )
}
