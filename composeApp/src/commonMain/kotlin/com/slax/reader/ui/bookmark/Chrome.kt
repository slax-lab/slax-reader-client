package com.slax.reader.ui.bookmark

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.multiplatform.webview.setting.PlatformWebSettings
import com.multiplatform.webview.util.KLogSeverity
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewState

@Composable
fun ChromeReaderView(nav: NavController, webState: WebViewState) {
    LaunchedEffect(Unit) {
        webState.webSettings.apply {
            isJavaScriptEnabled = true
            supportZoom = false
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
            logSeverity = KLogSeverity.Error

            androidWebSettings.apply {
                domStorageEnabled = true
                safeBrowsingEnabled = true
                allowFileAccess = false
                layerType = PlatformWebSettings.AndroidWebSettings.LayerType.HARDWARE
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        WebView(
            state = webState,
            modifier = Modifier.fillMaxSize(),
            captureBackPresses = true
        )
    }
}