package com.slax.reader

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeUIViewController


@OptIn(ExperimentalComposeUiApi::class)
fun MainViewController() = ComposeUIViewController(configure = { parallelRendering = true }) {
    SlaxNavigation()
}