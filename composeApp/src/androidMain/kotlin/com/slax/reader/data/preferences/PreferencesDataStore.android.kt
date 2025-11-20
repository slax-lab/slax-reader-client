package com.slax.reader.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.MultiProcessDataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import okhttp3.internal.platform.PlatformRegistry.applicationContext
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val preferencesPlatformModule = module {
    single<AppPreferences> { getPreferences(androidContext()) }
}

actual fun getPreferences(): AppPreferences {
    if (applicationContext == null) throw NullPointerException("Context is null")
    return AppPreferences(createDataStore(applicationContext!!))
}

fun getPreferences(context: Context): AppPreferences {
    return AppPreferences(createDataStore(context))
}

private fun createDataStore(context: Context): DataStore<Preferences> {
    val filePath = context.filesDir
        .resolve("user.preferences_pb")
        .absolutePath
        .toPath()

    return MultiProcessDataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = FileSystem.SYSTEM,
            serializer = PreferencesSerializer,
        ) {
            filePath
        },
    )
}
