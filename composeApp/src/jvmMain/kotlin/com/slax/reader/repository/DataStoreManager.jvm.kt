package com.slax.reader.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import java.io.File

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