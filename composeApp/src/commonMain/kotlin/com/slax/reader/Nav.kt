package com.slax.reader

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.multiplatform.webview.web.rememberWebViewState
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData


@Composable
fun SlaxNavigation() {
    val navCtrl = rememberNavController()

    NavHost(
        navController = navCtrl,
        startDestination = "home",
        modifier = androidx.compose.ui.Modifier.fillMaxSize()
    ) {
        composable("chrome") {
            val webState = rememberWebViewStateWithHTMLData(optimizedHtml)
            ChromeReaderView(navCtrl, webState)
        }
        composable("home") {
            App(navCtrl)
        }
        composable("hyper") {
            HybridReaderView(navCtrl)
        }
        composable("rich") {
            RichRender(navCtrl)
        }
        composable("raw_webview") {
            val webState = rememberWebViewState(url = "https://r.slax.com/s/P1A0aa4387")
            ChromeReaderView(navCtrl, webState)
        }
    }
}
