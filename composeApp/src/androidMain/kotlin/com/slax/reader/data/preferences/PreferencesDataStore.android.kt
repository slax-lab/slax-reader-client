package com.slax.reader.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val preferencesPlatformModule = module {
    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { androidContext().filesDir.resolve("user.preferences_pb").absolutePath.toPath() }
        )
    }
    single<AppPreferences> { AppPreferences(get()) }
}