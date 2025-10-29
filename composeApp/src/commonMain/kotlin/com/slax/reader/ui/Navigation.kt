package com.slax.reader.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.powersync.ExperimentalPowerSyncAPI
import com.powersync.PowerSyncDatabase
import com.slax.reader.const.*
import com.slax.reader.domain.auth.AuthDomain
import com.slax.reader.domain.auth.AuthState
import com.slax.reader.domain.sync.BackgroundDomain
import com.slax.reader.ui.about.AboutScreen
import com.slax.reader.ui.bookmark.DetailScreen
import com.slax.reader.ui.debug.DebugScreen
import com.slax.reader.ui.inbox.InboxListScreen
import com.slax.reader.ui.login.LoginScreen
import com.slax.reader.ui.setting.SettingScreen
import com.slax.reader.ui.space.SpaceManager
import com.slax.reader.utils.*
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics
import dev.gitlive.firebase.crashlytics.crashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalPowerSyncAPI::class)
@Composable
fun SlaxNavigation(
    navCtrl: NavHostController
) {
    val authDomain: AuthDomain = koinInject()
    val backgroundDomain: BackgroundDomain = koinInject()
    val authState by authDomain.authState.collectAsState()

    var database by remember { mutableStateOf<PowerSyncDatabase?>(null) }
    var connector by remember { mutableStateOf<Connector?>(null) }

    if (authState is AuthState.Authenticated && database == null) {
        database = koinInject()
        connector = koinInject()
    }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                launch(Dispatchers.IO) {
                    val state = getNetWorkState()
                    if (state != NetworkState.NONE && state != NetworkState.ACCESS_DENIED) {
                        authDomain.refreshToken()
                    }
                    database!!.connect(
                        connector!!,
                        params = ConnectParams,
                        options = ConnectOptions
                    )
                    backgroundDomain.startup()
                }
                Firebase.crashlytics.setCrashlyticsCollectionEnabled(true)
                Firebase.analytics.setAnalyticsCollectionEnabled(true)
                Firebase.analytics.setUserId((authState as AuthState.Authenticated).userId)
                Firebase.crashlytics.setUserId((authState as AuthState.Authenticated).userId)
            }

            AuthState.Unauthenticated -> {
                launch(Dispatchers.IO) {
                    database?.disconnectAndClear(
                        clearLocal = true,
//                        soft = true
                    )
                    backgroundDomain.cleanup()
                }
            }

            AuthState.Loading -> {
            }
        }
    }

    val startDestination = when (authState) {
        is AuthState.Authenticated -> InboxRoutes
        is AuthState.Unauthenticated -> LoginRoutes
        is AuthState.Loading -> return
    }

    NavHost(
        navController = navCtrl,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize(),
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable<LoginRoutes> {
            LoginScreen(
                navController = navCtrl
            )
        }
        composable<BookmarkRoutes> { backStackEntry ->
            val params = backStackEntry.toRoute<BookmarkRoutes>()
            DetailScreen(
                bookmarkId = params.bookmarkId,
                nav = navCtrl
            )
        }
        composable<InboxRoutes> {
            InboxListScreen(navCtrl)
        }
        composable<DebugRoutes> {
            DebugScreen()
        }
        composable<SpaceManagerRoutes> {
            SpaceManager()
        }
        composable<SettingsRoutes> {
            SettingScreen()
        }
        composable<AboutRoutes> {
            AboutScreen()
        }
    }
}

