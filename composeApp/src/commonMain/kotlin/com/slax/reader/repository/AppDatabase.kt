package com.slax.reader.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.slax.reader.repository.dao.Item
import com.slax.reader.repository.dao.ItemDao

@Database(
    entities = [Item::class],
    version = 1,
    exportSchema = false
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
}

expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

expect fun createAppDatabase(): AppDatabase
expect fun createDataStore(): DataStore<Preferences>