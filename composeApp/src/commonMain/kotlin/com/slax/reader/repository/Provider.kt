package com.slax.reader.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.koin.dsl.module

val dataModule = module {
    includes(repositoryModule)
}

val repositoryModule = module {
    single<AppDatabase> { createAppDatabase() }
    single<DataStore<Preferences>> { createDataStore() }
    single<UserPreferences> { UserPreferences(get<DataStore<Preferences>>()) }
}