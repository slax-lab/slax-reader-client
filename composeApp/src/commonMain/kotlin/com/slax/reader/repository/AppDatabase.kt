package com.slax.reader.repository

import androidx.room.Database
import androidx.room.RoomDatabase
import com.slax.reader.repository.dao.Item
import com.slax.reader.repository.dao.ItemDao

@Database(
    entities = [Item::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
}

expect fun createAppDatabase(): AppDatabase