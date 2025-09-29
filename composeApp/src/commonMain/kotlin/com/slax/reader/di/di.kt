package com.slax.reader.di

import com.powersync.PowerSyncDatabase
import com.slax.reader.data.database.AppSchema
import com.slax.reader.data.database.dao.BookmarkDao
import com.slax.reader.data.database.databasePlatformModule
import com.slax.reader.data.network.ApiService
import com.slax.reader.data.preferences.preferencesPlatformModule
import com.slax.reader.ui.bookmarks.BookmarkViewModel
import com.slax.reader.utils.Connector
import io.ktor.client.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.plugins.cache.storage.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val networkModule = module {
    single<HttpClient> {
        HttpClient {
            engine {
               
            }
            install(HttpCache) {
                publicStorage(CacheStorage.Disabled)
            }
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }
    }
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
}

val viewModelModule = module {
    factory<BookmarkViewModel> { BookmarkViewModel(get(), get()) }
}

val appModule = module {
    includes(
        databasePlatformModule,
        preferencesPlatformModule,
        networkModule,
        powerSyncModule,
        repositoryModule,
        viewModelModule
    )
}
