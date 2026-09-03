@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.slax.reader.utils

import com.slax.reader.share.ShareBridge
import platform.Foundation.NSFileManager

actual fun platformLogDirectory(): String {
    val container = NSFileManager.defaultManager
        .containerURLForSecurityApplicationGroupIdentifier("group.app.slax.reader")
        ?: error("App Group container is unavailable")
    return "${container.path}/logs"
}

actual fun shareLogFile(path: String) {
    ShareBridge.shared.shareFileAtPath(path)
}
