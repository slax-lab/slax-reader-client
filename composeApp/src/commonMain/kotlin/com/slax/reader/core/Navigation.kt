package com.slax.reader.core

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.multiplatform.webview.web.rememberWebViewState
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import com.slax.reader.ui.*


@Composable
fun SlaxNavigation() {
    val navCtrl = rememberNavController()

    NavHost(
        navController = navCtrl,
        startDestination = "home",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("chrome") {
            val webState = rememberWebViewStateWithHTMLData(optimizedHtml)
            ChromeReaderView(navCtrl, webState)
        }
        composable("home") {
            HomeScreen().Screen(navCtrl)
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
        composable("orders") {
            OrdersScreen(navCtrl)
        }
    }
}
