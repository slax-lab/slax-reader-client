package com.slax.reader.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.powersync.ExperimentalPowerSyncAPI
import com.powersync.PowerSyncDatabase
import com.powersync.sync.SyncOptions
import com.slax.reader.domain.auth.AuthDomain
import com.slax.reader.domain.auth.AuthState
import com.slax.reader.ui.debug.DebugScreen
import com.slax.reader.ui.inbox.InboxListScreen
import com.slax.reader.ui.login.LoginScreen
import com.slax.reader.utils.Connector
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics
import dev.gitlive.firebase.crashlytics.crashlytics
import org.koin.compose.koinInject

@OptIn(ExperimentalPowerSyncAPI::class)
@Composable
fun SlaxNavigation(
    navCtrl: NavHostController
) {
    val authDomain: AuthDomain = koinInject()
    val authState by authDomain.authState.collectAsState()

    val database: PowerSyncDatabase = koinInject()
    val connector = koinInject<Connector>()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                database.connect(connector, options = SyncOptions(newClientImplementation = true))
                Firebase.crashlytics.setCrashlyticsCollectionEnabled(true)
                Firebase.analytics.setAnalyticsCollectionEnabled(true)
                Firebase.analytics.setUserId((authState as AuthState.Authenticated).userId)
                Firebase.crashlytics.setUserId((authState as AuthState.Authenticated).userId)
                navCtrl.navigate("inbox") {
                    popUpTo(0) { inclusive = true }
                }
            }

            AuthState.Unauthenticated -> {
                database.disconnectAndClear()
                navCtrl.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }

            AuthState.Loading -> {
            }
        }
    }

    NavHost(
        navController = navCtrl,
        startDestination = "inbox",
        modifier = Modifier.fillMaxSize(),
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navCtrl.navigate("inbox") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("inbox") {
            InboxListScreen()
        }
        composable("debug") {
            DebugScreen()
        }
    }
}

