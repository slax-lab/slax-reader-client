package com.slax.reader

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.github.aakira.napier.Napier

actual fun onButtonClicked(buttonTitle: String, route: String) {
    Napier.w("user click $buttonTitle, then route to $route")
}

@Composable
actual fun HomeScreen(navController: NavController) {
    MaterialTheme {
        CommonHomeScreen(navController)
    }
}