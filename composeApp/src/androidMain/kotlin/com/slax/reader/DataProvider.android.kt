package com.slax.reader

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

actual class DataProvider(private val context: Context) : IDataProvider {

    private var database: AppDatabase? = null

    override fun getDatabase(): RoomDatabase {
        if (database == null) {
            database = Room.databaseBuilder<AppDatabase>(
                context = context.applicationContext,
                name = "slax_reader.db"
            )
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        }
        return database!!
    }

    override fun getDataStore(): DataStore<Preferences> {
        return context.dataStore
    }

    override fun getPlatform(): String = "Android"
}