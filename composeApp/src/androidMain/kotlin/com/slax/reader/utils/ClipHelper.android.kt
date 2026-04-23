package com.slax.reader.utils

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard

actual suspend fun ClipEntry.getText(): String? {
    val itemCount = clipData.itemCount
    var textFull = ""
    for (i in 0 until itemCount) {
        val item = clipData.getItemAt(i)
        item?.text?.let { textFull += it }
    }
    return textFull.ifEmpty { null }
}

actual suspend fun Clipboard.setPlainText(text: String) {
    setClipEntry(ClipEntry(ClipData.newPlainText("text", text)))
}