package com.slax.reader.utils

import androidx.compose.ui.platform.ClipEntry

expect suspend fun ClipEntry.getText(): String?