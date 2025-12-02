package com.slax.reader.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath
import org.koin.dsl.module
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL

@OptIn(ExperimentalForeignApi::class)
fun getPreferencesPath(): String {
    val appGroupIdentifier = "group.app.slax.reader"
    val sharedContainerURL: NSURL =
        NSFileManager.defaultManager.containerURLForSecurityApplicationGroupIdentifier(appGroupIdentifier)!!
    return sharedContainerURL.path + "/user.preferences_pb"
}

private val dataStoreInstance: DataStore<Preferences> by lazy {
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { getPreferencesPath().toPath() }
    )
}

actual val preferencesPlatformModule = module {
    single<DataStore<Preferences>> { dataStoreInstance }
    single<AppPreferences> { AppPreferences(get()) }
}

actual fun getPreferences(): AppPreferences {
    return AppPreferences(dataStoreInstance)
}