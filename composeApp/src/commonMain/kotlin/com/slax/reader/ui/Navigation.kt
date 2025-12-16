package com.slax.reader.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.powersync.ExperimentalPowerSyncAPI
import com.slax.reader.const.*
import com.slax.reader.domain.auth.AuthDomain
import com.slax.reader.domain.auth.AuthState
import com.slax.reader.domain.coordinator.CoordinatorDomain
import com.slax.reader.domain.sync.BackgroundDomain
import com.slax.reader.ui.about.AboutScreen
import com.slax.reader.ui.bookmark.DetailScreen
import com.slax.reader.ui.debug.DebugScreen
import com.slax.reader.ui.inbox.InboxListScreen
import com.slax.reader.ui.login.LoginScreen
import com.slax.reader.ui.me.SubscribeScreen
import com.slax.reader.ui.setting.DeleteAccountScreen
import com.slax.reader.ui.setting.SettingScreen
import com.slax.reader.ui.space.SpaceManager
import com.slax.reader.utils.LifeCycleHelper
import com.slax.reader.utils.NavHostTransitionHelper
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
    val coordinator: CoordinatorDomain = koinInject()
    val authState by authDomain.authState.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(LifeCycleHelper)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(LifeCycleHelper)
        }
    }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                launch(Dispatchers.IO) {
                    authDomain.refreshToken()
                    backgroundDomain.startup()
                    coordinator.startup()
                }
                Firebase.crashlytics.setCrashlyticsCollectionEnabled(true)
                Firebase.analytics.setAnalyticsCollectionEnabled(true)
                Firebase.analytics.setUserId((authState as AuthState.Authenticated).userId)
                Firebase.crashlytics.setUserId((authState as AuthState.Authenticated).userId)
            }

            AuthState.Unauthenticated -> {
                launch(Dispatchers.IO) {
                    backgroundDomain.cleanup()
                    coordinator.cleanup(true)
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
        exitTransition = NavHostTransitionHelper.exitTransition,
        enterTransition = NavHostTransitionHelper.enterTransition,
        popEnterTransition = NavHostTransitionHelper.popEnterTransition,
        popExitTransition = NavHostTransitionHelper.popExitTransition,
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
                onBackClick = {
                    navCtrl.popBackStack()
                }
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
            SettingScreen(
                onBackClick = {
                    navCtrl.popBackStack()
                },
                navController = navCtrl
            )
        }
        composable<AboutRoutes> {
            AboutScreen(onBackClick = {
                navCtrl.popBackStack()
            })
        }
        composable<DeleteAccountRoutes> {
            DeleteAccountScreen(onBackClick = {
                navCtrl.popBackStack()
            })
        }
        composable<SubscribeRoutes> {
            SubscribeScreen(onBackClick = {
                navCtrl.popBackStack()
            })
        }
    }
}

