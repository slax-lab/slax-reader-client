package com.slax.reader.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.FrameRateCategory
import androidx.compose.ui.Modifier
import androidx.compose.ui.preferredFrameRate
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
import com.slax.reader.domain.sync.CollectionBackgroundDomain
import com.slax.reader.ui.about.AboutScreen
import com.slax.reader.ui.bookmark.DetailScreen
import com.slax.reader.ui.debug.DebugScreen
import com.slax.reader.ui.bookmark.DetailScreenEvent
import com.slax.reader.ui.feedback.FeedbackScreen
import com.slax.reader.ui.inbox.InboxListScreen
import com.slax.reader.ui.login.LoginScreen
import com.slax.reader.ui.setting.DeleteAccountScreen
import com.slax.reader.ui.setting.SettingScreen
import com.slax.reader.ui.subscription.SubscriptionManagerScreen
import com.slax.reader.utils.FirebaseHelper
import com.slax.reader.utils.LifeCycleHelper
import com.slax.reader.utils.NavHostTransitionHelper
import com.slax.reader.utils.aboutEvent
import com.slax.reader.utils.bookmarkEvent
import com.slax.reader.utils.bookmarkListEvent
import com.slax.reader.utils.feedbackEvent
import com.slax.reader.utils.settingEvent
import com.slax.reader.utils.subscriptionEvent
import com.slax.reader.utils.userEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@OptIn(ExperimentalPowerSyncAPI::class)
@Composable
fun SlaxNavigation(
    navCtrl: NavHostController
) {
    val authDomain: AuthDomain = koinInject()
    val backgroundDomain: BackgroundDomain = koinInject()
    val collectionBackgroundDomain: CollectionBackgroundDomain = koinInject()
    val coordinator: CoordinatorDomain = koinInject()
    val authState by authDomain.authState.collectAsState()
    var readyUserId by remember { mutableStateOf<String?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(LifeCycleHelper)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(LifeCycleHelper)
        }
    }

    // 只按 userId 作为 key：token 刷新会产生新的 Authenticated 实例，
    // 若用整个 authState 当 key，刷新会把正在执行的本效果取消再重跑一遍。
    val authKey = when (val state = authState) {
        is AuthState.Authenticated -> state.userId
        AuthState.Unauthenticated -> "unauthenticated"
        AuthState.Loading -> "loading"
    }

    LaunchedEffect(authKey) {
        when (val state = authState) {
            is AuthState.Authenticated -> {
                val previousReadyUserId = readyUserId
                val isUserSwitch = previousReadyUserId != null && previousReadyUserId != state.userId

                // 换用户时必须先把上一个用户的本地数据清干净再渲染，这一步同步等待。
                // 它是纯本地操作，不含网络请求。
                if (isUserSwitch) {
                    withContext(NonCancellable + Dispatchers.IO) {
                        backgroundDomain.cleanup()
                        collectionBackgroundDomain.cleanup()
                        coordinator.cleanup(true)
                    }
                }

                // 首屏不等网络：先放行渲染，token 刷新与各 domain 启动都在后台进行。
                readyUserId = state.userId
                FirebaseHelper.setUserId(state.userId)
                FirebaseHelper.setCrashlyticsUserId(state.userId)

                launch(Dispatchers.IO) {
                    authDomain.refreshToken()
                    coordinator.startup()
                    backgroundDomain.startup()
                    collectionBackgroundDomain.startup()
                }
            }

            AuthState.Unauthenticated -> {
                readyUserId = null
                withContext(NonCancellable + Dispatchers.IO) {
                    backgroundDomain.cleanup()
                    collectionBackgroundDomain.cleanup()
                    coordinator.cleanup(true)
                }
            }

            AuthState.Loading -> {
            }
        }
    }

    val authenticatedUserId = (authState as? AuthState.Authenticated)?.userId
    if (authenticatedUserId != null && readyUserId != authenticatedUserId) return

    val startDestination = when (authState) {
        is AuthState.Authenticated -> InboxRoutes
        is AuthState.Unauthenticated -> LoginRoutes
        is AuthState.Loading -> return
    }

    NavHost(
        navController = navCtrl,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize().preferredFrameRate(FrameRateCategory.High),
        exitTransition = NavHostTransitionHelper.exitTransition,
        enterTransition = NavHostTransitionHelper.enterTransition,
        popEnterTransition = NavHostTransitionHelper.popEnterTransition,
        popExitTransition = NavHostTransitionHelper.popExitTransition,
    ) {
        composable<LoginRoutes> {
            LoginScreen(
                navController = navCtrl
            )
            LaunchedEffect(Unit) { userEvent.view("login").send() }
        }
        composable<BookmarkRoutes> { backStackEntry ->
            val params = backStackEntry.toRoute<BookmarkRoutes>()
            DetailScreen(
                bookmarkId = params.bookmarkId,
                collectionOwnerId = params.collectionOwnerId,
                collectionId = params.collectionId,
                onEvent = { event ->
                    when (event) {
                        DetailScreenEvent.BackClick -> {
                            navCtrl.popBackStack()
                        }

                        DetailScreenEvent.NavigateToSubscription -> {
                            navCtrl.navigate(SubscriptionManagerRoutes)
                            subscriptionEvent.view().source("dialog").send()
                        }

                        is DetailScreenEvent.NavigateToFeedback -> {
                            val p = event.params
                            navCtrl.navigate(
                                FeedbackRoutes(
                                    title = p.title,
                                    href = p.href,
                                    email = p.email,
                                    bookmarkId = p.bookmarkId,
                                    entryPoint = p.entryPoint,
                                    version = p.version,
                                )
                            )
                        }
                    }
                }
            )
            LaunchedEffect(Unit) {
                bookmarkEvent
                    .view()
                    .bookmarkUUID(params.bookmarkId)
                    .mode("snapshot")
                    .send()
            }
        }
        composable<InboxRoutes> {
            InboxListScreen(navCtrl)
            LaunchedEffect(Unit) { bookmarkListEvent.view().send() }
        }
        composable<SettingsRoutes> {
            SettingScreen(
                onBackClick = {
                    navCtrl.popBackStack()
                },
                navController = navCtrl
            )
            LaunchedEffect(Unit) { settingEvent.view().send() }
        }
        composable<AboutRoutes> {
            AboutScreen(
                onBackClick = {
                    navCtrl.popBackStack()
                },
                onDebugClick = {
                    navCtrl.navigate(DebugRoutes)
                }
            )
            LaunchedEffect(Unit) { aboutEvent.view().send() }
        }
        composable<DebugRoutes> {
            DebugScreen(onBackClick = {
                navCtrl.popBackStack()
            })
        }
        composable<DeleteAccountRoutes> {
            DeleteAccountScreen(onBackClick = {
                navCtrl.popBackStack()
            })
            LaunchedEffect(Unit) { userEvent.view("delete_account").send() }
        }
        composable<SubscriptionManagerRoutes> {
            SubscriptionManagerScreen(onBackClick = {
                navCtrl.popBackStack()
            })
            LaunchedEffect(Unit) { subscriptionEvent.view().send() }
        }
        composable<FeedbackRoutes> { backStackEntry ->
            val params = backStackEntry.toRoute<FeedbackRoutes>()
            FeedbackScreen(
                title = params.title,
                href = params.href,
                email = params.email,
                bookmarkId = params.bookmarkId,
                entryPoint = params.entryPoint,
                version = params.version,
                onBackClick = { navCtrl.popBackStack() }
            )
            LaunchedEffect(Unit) { feedbackEvent.view().send() }
        }
    }
}
