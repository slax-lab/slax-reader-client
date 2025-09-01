package com.slax.reader.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

class UserPreferences(private val dataStore: DataStore<Preferences>) {
    fun getDataStore(): DataStore<Preferences> = dataStore
}

expect fun createDataStore(): DataStore<Preferences>