package com.slax.reader

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "slax-reader-client",
    ) {
        App()
    }
}