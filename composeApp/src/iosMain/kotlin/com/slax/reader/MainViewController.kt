package com.slax.reader

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeUIViewController
import com.slax.reader.core.SlaxNavigation
import com.slax.reader.di.appModule
import org.koin.core.context.startKoin


@OptIn(ExperimentalComposeUiApi::class)
fun MainViewController() = ComposeUIViewController(configure = { parallelRendering = true }) {
    startKoin {
        modules(appModule)
    }
    SlaxNavigation()
}