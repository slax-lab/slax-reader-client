package com.slax.reader.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

actual fun createDataStore(): DataStore<Preferences> {
    return DataStoreFactory.createDataStore()
}

object DataStoreFactory : KoinComponent {
    private val context: Context by inject()

    fun createDataStore(): DataStore<Preferences> {
        return context.dataStore
    }
}