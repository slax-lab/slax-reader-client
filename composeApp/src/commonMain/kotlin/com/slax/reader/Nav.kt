package com.slax.reader

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController


@Composable
fun SlaxNavigation() {
    val navCtrl = rememberNavController()

    NavHost(
        navController = navCtrl,
        startDestination = "home",
        modifier = androidx.compose.ui.Modifier.fillMaxSize()
    ) {
        composable("chrome") {
            ChromeReaderView(navCtrl)
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
    }
}
