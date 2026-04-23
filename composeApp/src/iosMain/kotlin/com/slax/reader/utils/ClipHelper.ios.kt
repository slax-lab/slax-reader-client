package com.slax.reader.utils

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import platform.UIKit.UIPasteboard

@OptIn(ExperimentalComposeUiApi::class)
actual suspend fun ClipEntry.getText() = getPlainText()

actual suspend fun Clipboard.setPlainText(text: String) {
    UIPasteboard.generalPasteboard.string = text
}