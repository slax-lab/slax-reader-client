package com.slax.reader.di

import com.powersync.PowerSyncDatabase
import com.slax.reader.data.database.AppSchema
import com.slax.reader.data.database.dao.BookmarkCommentDao
import com.slax.reader.data.database.dao.BookmarkDao
import com.slax.reader.data.database.dao.BookmarkRepository
import com.slax.reader.data.database.dao.LocalBookmarkDao
import com.slax.reader.data.database.dao.LocalBookmarkRepository
import com.slax.reader.data.database.dao.PowerSyncDao
import com.slax.reader.data.database.dao.SubscriptionDao
import com.slax.reader.data.database.dao.SubscriptionRepository
import com.slax.reader.data.database.dao.UserDao
import com.slax.reader.data.database.dao.UserRepository
import com.slax.reader.data.database.databasePlatformModule
import com.slax.reader.data.file.FileManager
import com.slax.reader.data.network.ApiService
import com.slax.reader.data.network.AccountApi
import com.slax.reader.data.network.PaymentApi
import com.slax.reader.data.network.BookmarkAiApi
import com.slax.reader.data.network.FeedbackApi
import com.slax.reader.data.preferences.preferencesPlatformModule
import com.slax.reader.data.preferences.AppPreferences
import com.slax.reader.data.preferences.SettingsPreferences
import com.slax.reader.data.preferences.AuthTokenPreferences
import com.slax.reader.domain.auth.AuthDomain
import com.slax.reader.domain.auth.AuthGateway
import com.slax.reader.domain.coordinator.CoordinatorDomain
import com.slax.reader.domain.coordinator.NetworkCoordinator
import com.slax.reader.domain.image.ImageDownloadManager
import com.slax.reader.domain.image.ShareImageSelector
import com.slax.reader.domain.sync.BackgroundDomain
import com.slax.reader.ui.bookmark.BookmarkDetailViewModel
import com.slax.reader.ui.inbox.InboxListViewModel
import com.slax.reader.ui.login.LoginViewModel
import com.slax.reader.ui.setting.SettingViewModel
import com.slax.reader.ui.sidebar.SidebarViewModel
import com.slax.reader.ui.subscription.SubscriptionViewModel
import com.slax.reader.utils.Connector
import com.slax.reader.utils.FirstPartyEventReporter
import com.slax.reader.utils.IapGateway
import com.slax.reader.utils.PlatformIapGateway
import com.slax.reader.utils.LifeCycleHelper
import com.slax.reader.utils.getHttpClient
import com.slax.reader.utils.platformFileSystem
import com.slax.reader.ui.AppLifecycle
import com.slax.reader.ui.PlatformWebViewHost
import com.slax.reader.ui.WebViewHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import org.koin.core.KoinApplication
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val fileModule = module {
    single { platformFileSystem() }
    single { FileManager(get()) }
}

val networkModule = module {
    single { getHttpClient(get()) }
    single { Connector(get(), get()) }
    single { ApiService(get()) }
    single { FirstPartyEventReporter(get(), get()) }
    single<AccountApi> { get<ApiService>() }
    single<PaymentApi> { get<ApiService>() }
    single<BookmarkAiApi> { get<ApiService>() }
    single<FeedbackApi> { get<ApiService>() }
}

val powerSyncModule = module {
    single {
        PowerSyncDatabase(get(), schema = AppSchema, dbFilename = "powersync.db")
    }
}

val repositoryModule = module {
    single(named("daoScope")) { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    single { BookmarkDao(get(named("daoScope")), get()) }
    single { UserDao(get(named("daoScope")), get()) }
    single { LocalBookmarkDao(get(named("daoScope")), get()) }
    single { SubscriptionDao(get(named("daoScope")), get()) }
    single<UserRepository> { get<UserDao>() }
    single<BookmarkRepository> { get<BookmarkDao>() }
    single<LocalBookmarkRepository> { get<LocalBookmarkDao>() }
    single<SubscriptionRepository> { get<SubscriptionDao>() }
    single { PowerSyncDao(get()) }
    single { BookmarkCommentDao(get()) }
}

val preferencesContractModule = module {
    single<SettingsPreferences> { get<AppPreferences>() }
    single<AuthTokenPreferences> { get<AppPreferences>() }
}

val viewModelModule = module {
    viewModelOf(::InboxListViewModel)
    viewModelOf(::BookmarkDetailViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::SidebarViewModel)
    viewModelOf(::SettingViewModel)
    viewModelOf(::SubscriptionViewModel)
}

val domainModule = module {
    single { AuthDomain(get(), get(), get()) }
    single<AuthGateway> { get<AuthDomain>() }
    single { BackgroundDomain(get(), get(), get(), get(), get(), get()) }
    single { CoordinatorDomain(get(), get(), get()) }
    single<NetworkCoordinator> { get<CoordinatorDomain>() }
    single { ImageDownloadManager(get(), get()) }
    single { ShareImageSelector(get()) }
}

val paymentModule = module {
    single<IapGateway> { PlatformIapGateway() }
}

val appModule = module {
    includes(
        fileModule,
        databasePlatformModule,
        preferencesPlatformModule,
        preferencesContractModule,
        networkModule,
        paymentModule,
        powerSyncModule,
        domainModule,
        repositoryModule,
        viewModelModule
    )
    single<AppLifecycle> { LifeCycleHelper }
    single<WebViewHost> { PlatformWebViewHost }
}

fun KoinApplication.configureKoin() {
    modules(appModule)
}
