package com.slax.reader

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.multiplatform.webview.util.addTempDirectoryRemovalHook
import com.slax.reader.core.SlaxNavigation
import com.slax.reader.di.appModule
import org.koin.core.context.startKoin

fun main() = application {
    startKoin {
        modules(appModule)
    }

    addTempDirectoryRemovalHook()

    Window(
        onCloseRequest = ::exitApplication,
        title = "slax-reader-client",
    ) {
        SlaxNavigation()
    }
}