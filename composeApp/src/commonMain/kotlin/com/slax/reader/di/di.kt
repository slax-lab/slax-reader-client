package com.slax.reader.di

import com.powersync.PowerSyncDatabase
import com.slax.reader.data.database.AppSchema
import com.slax.reader.data.database.dao.BookmarkDao
import com.slax.reader.data.database.dao.UserDao
import com.slax.reader.data.database.databasePlatformModule
import com.slax.reader.data.network.ApiService
import com.slax.reader.data.preferences.preferencesPlatformModule
import com.slax.reader.domain.auth.AuthDomain
import com.slax.reader.ui.inbox.InboxListViewModel
import com.slax.reader.utils.Connector
import com.slax.reader.utils.getHttpClient
import io.ktor.client.*
import org.koin.dsl.module

val networkModule = module {
    single<HttpClient> { getHttpClient(get()) }
    single<Connector> { Connector(get()) }
    single<ApiService> { ApiService(get(), get()) }
}

val powerSyncModule = module {
    single<PowerSyncDatabase> {
        PowerSyncDatabase(get(), schema = AppSchema, dbFilename = "powersync.db")
    }
}

val repositoryModule = module {
    single<BookmarkDao> {
        BookmarkDao(get())
    }
    single<UserDao> {
        UserDao(get())
    }
}

val viewModelModule = module {
    single<InboxListViewModel> { InboxListViewModel(get(), get(), get()) }
}

val domainModule = module {
    single<AuthDomain> { AuthDomain(get(), get()) }
}

val appModule = module {
    includes(
        databasePlatformModule,
        preferencesPlatformModule,
        networkModule,
        powerSyncModule,
        domainModule,
        repositoryModule,
        viewModelModule
    )
}
