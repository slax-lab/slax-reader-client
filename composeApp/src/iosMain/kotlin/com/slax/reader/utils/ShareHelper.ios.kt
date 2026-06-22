@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.slax.reader.utils

import com.slax.reader.share.ShareBridge
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create

actual fun shareContent(title: String, text: String, url: String, imageBytes: ByteArray?) {
    val imageData = imageBytes?.takeIf { it.isNotEmpty() }?.toNSData()
    ShareBridge.shared.shareWithTitle(
        title = title,
        text = text,
        urlString = url,
        imageData = imageData
    )
}

private fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}
