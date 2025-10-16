package com.slax.reader.ui.space

import androidx.compose.runtime.Composable
import com.slax.reader.data.file.FileManager
import org.koin.compose.koinInject

@Composable
fun SpaceManager() {
    val fileManager = koinInject<FileManager>()
}