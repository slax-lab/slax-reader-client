package com.slax.reader.data.database

import com.powersync.DatabaseDriverFactory
import com.powersync.PersistentConnectionFactory
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

actual val databasePlatformModule = module {
    single { DatabaseDriverFactory(androidContext()) } bind PersistentConnectionFactory::class
}