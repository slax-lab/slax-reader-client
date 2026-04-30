package com.slax.reader.utils

import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard

expect suspend fun ClipEntry.getText(): String?

expect suspend fun Clipboard.setPlainText(text: String)