package com.slax.reader.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.core.MultiProcessDataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import okhttp3.internal.platform.PlatformRegistry.applicationContext
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.dsl.module

actual val preferencesPlatformModule = module {
    single<AppPreferences> { getPreferences() }
}

actual fun getPreferences(): AppPreferences {
    return AppPreferences(dataStoreInstance)
}

private val dataStoreInstance: DataStore<Preferences> by lazy {
    if (applicationContext == null) throw NullPointerException("Context is null")

    val filePath = applicationContext!!.filesDir
        .resolve("user.preferences_pb")
        .absolutePath
        .toPath()

    MultiProcessDataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = FileSystem.SYSTEM,
            serializer = PreferencesSerializer,
        ) {
            filePath
        },
    )
}