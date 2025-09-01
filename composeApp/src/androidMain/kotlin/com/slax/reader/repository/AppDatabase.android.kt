package com.slax.reader.repository

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual fun createAppDatabase(): AppDatabase {
    return DatabaseFactory.createDatabase()
}

object DatabaseFactory : KoinComponent {
    private val context: Context by inject()

    fun createDatabase(): AppDatabase {
        return Room.databaseBuilder<AppDatabase>(
            context = context,
            name = "slax_reader.db"
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
}