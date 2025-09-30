package com.slax.reader.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.multiplatform.webview.web.rememberWebViewState
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import com.powersync.ExperimentalPowerSyncAPI
import com.powersync.PowerSyncDatabase
import com.powersync.sync.SyncOptions
import com.slax.reader.data.preferences.AppPreferences
import com.slax.reader.ui.bookmark.ChromeReaderView
import com.slax.reader.ui.bookmark.optimizedHtml
import com.slax.reader.ui.bookmarks.BookmarkViewModel
import com.slax.reader.ui.bookmarks.UserBookmarksScreen
import com.slax.reader.ui.debug.DebugScreen
import com.slax.reader.ui.home.HomeScreen
import com.slax.reader.utils.Connector
import org.koin.compose.koinInject

@OptIn(ExperimentalPowerSyncAPI::class)
@Composable
fun SlaxNavigation(
    navCtrl: NavHostController
) {

    val database: PowerSyncDatabase = koinInject<PowerSyncDatabase>()
    val connector = koinInject<Connector>()
    val preferences = koinInject<AppPreferences>()
    LaunchedEffect("App Start") {
        preferences.setAuthToken("eyJhbGciOiJIUzI1NiJ9.eyJpZCI6IjE0NDEyIiwibGFuZyI6ImVuIiwiZW1haWwiOiJkYWd1YW5nODMwQGdtYWlsLmNvbSIsImV4cCI6MTc2MDE2MzUxMCwiaWF0IjoxNzU4ODY3NTEwLCJpc3MiOiJzbGF4LXJlYWRlci1wcm9kIn0.jIv4z6eAthvEyHFwa-yrijo9JNloP-CfUl4TlxT60YU")
        database.connect(connector, options = SyncOptions(newClientImplementation = true))
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
        composable("raw_webview") {
            val webState = rememberWebViewState(url = "https://r.slax.com/s/P1A0aa4387")
            ChromeReaderView(navCtrl, webState)
        }
        composable("bookmark/{bookmark_id}") { backStackEntry ->
            backStackEntry.let { entry ->
                entry.arguments?.toString()?.let {
                    val webState = rememberWebViewState(url = "https://r.slax.com/s/$it")
                    ChromeReaderView(navCtrl, webState)
                }
            }
        }
        composable("orders") {
            val viewModel: BookmarkViewModel = koinInject()
            UserBookmarksScreen(viewModel)
        }
        composable("debug") {
            DebugScreen()
        }
    }
}
