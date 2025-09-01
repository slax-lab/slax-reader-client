package com.slax.reader.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.github.aakira.napier.Napier

actual fun onButtonClicked(buttonTitle: String, route: String) {
    Napier.w("user click $buttonTitle, then route to $route")
}

@Composable
actual fun HomeScreens(navController: NavController) {
    HomeScreen(navController)
}