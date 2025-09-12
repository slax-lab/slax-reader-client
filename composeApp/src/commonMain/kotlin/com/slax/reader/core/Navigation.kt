package com.slax.reader.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.multiplatform.webview.web.rememberWebViewState
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import com.powersync.DatabaseDriverFactory
import com.powersync.ExperimentalPowerSyncAPI
import com.powersync.PowerSyncDatabase
import com.powersync.sync.SyncOptions
import com.slax.reader.repository.AppSchema
import com.slax.reader.ui.*
import org.koin.compose.koinInject


@OptIn(ExperimentalPowerSyncAPI::class)
@Composable
fun SlaxNavigation() {
    val navCtrl = rememberNavController()
    var db by remember { mutableStateOf<PowerSyncDatabase?>(null) }
    val factory = koinInject<DatabaseDriverFactory>()
    val connector = remember { Connector() }

    val viewModel = remember(db) {
        if (db != null) BookmarkViewModel(db!!) else null
    }

    LaunchedEffect(key1 = "db_create") {
        db?.let {
            try {
                it.disconnect()
                println("Cleaned up old database instance")
            } catch (e: Exception) {
                println("Error cleaning up old instance: ${e.message}")
            }
        }

        val newDb = PowerSyncDatabase(factory, AppSchema)
        db = newDb
        println("Created new database instance")

        try {
            println("Connecting PowerSync...")
            newDb.connect(connector, options = SyncOptions(newClientImplementation = true))
            println("PowerSync connected successfully")
        } catch (e: Exception) {
            println("Error connecting PowerSync: ${e.message}")
        }
    }

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
            viewModel?.let { vm ->
                UserBookmarksScreen(vm)
            } ?: run {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
