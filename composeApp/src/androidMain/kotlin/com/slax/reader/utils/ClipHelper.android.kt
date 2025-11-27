package com.slax.reader.utils

import androidx.compose.ui.platform.ClipEntry

actual suspend fun ClipEntry.getText(): String? {
    val itemCount = clipData.itemCount
    var textFull = ""
    for (i in 0 until itemCount) {
        val item = clipData.getItemAt(i)
        item?.text?.let { textFull += it }
    }
    return textFull.ifEmpty { null }
}