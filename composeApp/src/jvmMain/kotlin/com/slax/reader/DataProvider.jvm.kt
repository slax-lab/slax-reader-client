package com.slax.reader

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import okio.Path.Companion.toPath
import java.io.File

actual class DataProvider : IDataProvider {
    private var database: AppDatabase? = null
    private var dataStore: DataStore<Preferences>? = null

    private fun getDataDirectory(): File {
        val osName = System.getProperty("os.name").lowercase()
        return when {
            osName.contains("windows") -> {
                // Windows: %APPDATA%/SlaxReader
                val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
                File(appData, "SlaxReader")
            }

            osName.contains("mac") || osName.contains("darwin") -> {
                // macOS: ~/Library/Application Support/SlaxReader
                val userHome = System.getProperty("user.home")
                File(userHome, "Library/Application Support/SlaxReader")
            }

            else -> {
                // Linux and others: ~/.local/share/SlaxReader
                val userHome = System.getProperty("user.home")
                File(userHome, ".local/share/SlaxReader")
            }
        }.apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    override fun getDatabase(): RoomDatabase {
        if (database == null) {
            val dataDir = getDataDirectory()
            val dbFile = File(dataDir, "slax_reader.db")

            database = Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        }
        return database!!
    }

    override fun getDataStore(): DataStore<Preferences> {
        if (dataStore == null) {
            val dataDir = getDataDirectory()
            val dataStoreFile = File(dataDir, "settings.preferences_pb")

            dataStore = PreferenceDataStoreFactory.createWithPath(
                produceFile = { dataStoreFile.absolutePath.toPath() }
            )
        }
        return dataStore!!
    }

    override fun getPlatform(): String {
        val osName = System.getProperty("os.name")
        val osArch = System.getProperty("os.arch")
        return "JVM ($osName $osArch)"
    }

    override fun getDatabaseVersion(): String {
        return "1.0.0"
    }
}