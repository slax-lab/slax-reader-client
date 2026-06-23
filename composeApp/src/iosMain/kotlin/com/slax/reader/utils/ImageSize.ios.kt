@file:OptIn(ExperimentalForeignApi::class)

package com.slax.reader.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage

actual fun decodeImageSize(bytes: ByteArray): ImageSize? {
    if (bytes.isEmpty()) return null
    val data = bytes.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
    }
    val image = UIImage.imageWithData(data) ?: return null
    return image.size.useContents {
        val w = width.toInt()
        val h = height.toInt()
        if (w <= 0 || h <= 0) null else ImageSize(w, h)
    }
}
