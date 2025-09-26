package com.slax.reader.data.database

import com.powersync.DatabaseDriverFactory
import org.koin.dsl.module

actual val databasePlatformModule = module {
    single<DatabaseDriverFactory> {
        DatabaseDriverFactory()
    }
}