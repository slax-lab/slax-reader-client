package com.slax.reader.di

import com.powersync.DatabaseDriverFactory
import org.koin.dsl.module

actual val platformModule = module {
    single<DatabaseDriverFactory> {
        DatabaseDriverFactory()
    }
}