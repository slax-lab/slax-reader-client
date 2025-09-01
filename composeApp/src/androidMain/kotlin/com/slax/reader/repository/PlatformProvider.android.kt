package com.slax.reader.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

actual fun createAppDatabase(): AppDatabase {
    val context = org.koin.core.context.GlobalContext.get().get<Context>()
    return Room.databaseBuilder<AppDatabase>(
        context = context,
        name = "slax_reader.db"
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}

actual fun createDataStore(): DataStore<Preferences> {
    val context = org.koin.core.context.GlobalContext.get().get<Context>()
    return context.dataStore
}