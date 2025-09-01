package com.slax.reader.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import okio.Path.Companion.toPath
import java.io.File

actual fun createAppDatabase(): AppDatabase {
    val dataDir = getDataDirectory()
    val dbFile = File(dataDir, "slax_reader.db")
    
    return Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}

actual fun createDataStore(): DataStore<Preferences> {
    val dataDir = getDataDirectory()
    val dataStoreFile = File(dataDir, "settings.preferences_pb")
    
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { dataStoreFile.absolutePath.toPath() }
    )
}

private fun getDataDirectory(): File {
    val osName = System.getProperty("os.name").lowercase()
    return when {
        osName.contains("windows") -> {
            val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
            File(appData, "SlaxReader")
        }
        osName.contains("mac") || osName.contains("darwin") -> {
            val userHome = System.getProperty("user.home")
            File(userHome, "Library/Application Support/SlaxReader")
        }
        else -> {
            val userHome = System.getProperty("user.home")
            File(userHome, ".local/share/SlaxReader")
        }
    }.apply {
        if (!exists()) {
            mkdirs()
        }
    }
}