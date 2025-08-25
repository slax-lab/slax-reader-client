package com.slax.reader

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase

expect class DataProvider : IDataProvider

interface IDataProvider {
    fun getDatabase(): RoomDatabase = TODO("Database not implemented for this platform")
    fun getDataStore(): DataStore<Preferences> = TODO("DataStore not implemented for this platform")

    fun getPlatform(): String = "Unknown Platform"
    fun getDatabaseVersion(): String = "Unknown Version"
}
