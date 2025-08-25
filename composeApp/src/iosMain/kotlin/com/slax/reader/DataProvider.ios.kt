package com.slax.reader

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
actual class DataProvider : IDataProvider {

    private var database: AppDatabase? = null
    private var dataStore: DataStore<Preferences>? = null

    override fun getDatabase(): RoomDatabase {
        if (database == null) {
            val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = false,
                error = null
            )
            val dbPath = documentDirectory!!.path + "/slax_reader.db"

            database = Room.databaseBuilder<AppDatabase>(name = dbPath)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.Default)
                .build()
        }
        return database!!
    }

    override fun getDataStore(): DataStore<Preferences> {
        if (dataStore == null) {
            val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = false,
                error = null
            )
            val dataStoreFile = documentDirectory!!.path + "/settings.preferences_pb"

            dataStore = PreferenceDataStoreFactory.createWithPath(
                produceFile = { dataStoreFile.toPath() }
            )
        }
        return dataStore!!
    }

    override fun getPlatform(): String = "iOS"
}