package com.slax.reader.ui.home

import androidx.navigation.NavController
import io.github.aakira.napier.Napier

actual fun HomeScreen(): HomeScreen = object : HomeScreen {
    override fun onButtonClicked(buttonTitle: String, route: String, navController: NavController) {
        Napier.i("Android user click $buttonTitle, then route to $route")
        super.onButtonClicked(buttonTitle, route, navController)
    }
}