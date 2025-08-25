package com.slax.reader

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.multiplatform.webview.util.addTempDirectoryRemovalHook

fun main() = application {
    addTempDirectoryRemovalHook()

    Window(
        onCloseRequest = ::exitApplication,
        title = "slax-reader-client",
    ) {
        SlaxNavigation()
    }
}