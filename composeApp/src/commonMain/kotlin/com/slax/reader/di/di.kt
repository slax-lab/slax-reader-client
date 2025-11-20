package com.slax.reader.di

import app.slax.reader.SlaxConfig
import com.powersync.PowerSyncDatabase
import com.slax.reader.data.database.AppSchema
import com.slax.reader.data.database.dao.BookmarkDao
import com.slax.reader.data.database.dao.LocalBookmarkDao
import com.slax.reader.data.database.dao.PowerSyncDao
import com.slax.reader.data.database.dao.UserDao
import com.slax.reader.data.database.databasePlatformModule
import com.slax.reader.data.file.FileManager
import com.slax.reader.data.network.ApiService
import com.slax.reader.data.preferences.preferencesPlatformModule
import com.slax.reader.domain.auth.AuthDomain
import com.slax.reader.domain.coordinator.CoordinatorDomain
import com.slax.reader.domain.sync.BackgroundDomain
import com.slax.reader.ui.bookmark.BookmarkDetailViewModel
import com.slax.reader.ui.inbox.InboxListViewModel
import com.slax.reader.ui.login.LoginViewModel
import com.slax.reader.ui.sidebar.SidebarViewModel
import com.slax.reader.utils.Connector
import com.slax.reader.utils.getHttpClient
import com.slax.reader.utils.platformFileSystem
import io.kotzilla.sdk.analytics.koin.analytics
import org.koin.core.KoinApplication
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val fileModule = module {
    single { platformFileSystem() }
    single { FileManager(get()) }
}

val networkModule = module {
    single { getHttpClient(get()) }
    single { Connector(get(), get()) }
    single { ApiService(get()) }
}

val powerSyncModule = module {
    single {
        PowerSyncDatabase(get(), schema = AppSchema, dbFilename = "powersync.db")
    }
}

val repositoryModule = module {
    single { BookmarkDao(get()) }
    single { UserDao(get()) }
    single { LocalBookmarkDao(get()) }
    single { PowerSyncDao(get()) }
}

val viewModelModule = module {
    viewModelOf(::InboxListViewModel)
    viewModelOf(::BookmarkDetailViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::SidebarViewModel)
}

val domainModule = module {
    single { AuthDomain(get(), get(), get()) }
    single { BackgroundDomain(get(), get(), get(), get()) }
    single { CoordinatorDomain(get(), get(), get()) }
}

val appModule = module {
    includes(
        fileModule,
        databasePlatformModule,
        preferencesPlatformModule,
        networkModule,
        powerSyncModule,
        domainModule,
        repositoryModule,
        viewModelModule
    )
}

fun KoinApplication.configureKoin() {
    modules(appModule)

    analytics {
        setApiKey(SlaxConfig.KOTZILLA_KEY)
        setVersion("${SlaxConfig.APP_VERSION_NAME}(${SlaxConfig.APP_VERSION_CODE})")
    }
}
