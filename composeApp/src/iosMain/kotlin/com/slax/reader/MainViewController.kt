package com.slax.reader

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeUIViewController
import androidx.navigation.compose.rememberNavController
import com.slax.reader.di.appModule
import com.slax.reader.ui.SlaxNavigation
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize
import org.koin.compose.KoinApplication

@OptIn(ExperimentalComposeUiApi::class)
fun MainViewController() = ComposeUIViewController {
    KoinApplication(
        application = {
            modules(appModule)
        }
    ) {
        Firebase.initialize()
        val navController = rememberNavController()
        SlaxNavigation(navController)
    }
}