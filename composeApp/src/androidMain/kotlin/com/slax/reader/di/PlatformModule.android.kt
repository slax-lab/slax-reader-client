package com.slax.reader.di

import com.powersync.DatabaseDriverFactory
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single<DatabaseDriverFactory> {
        DatabaseDriverFactory(androidContext())
    }
}