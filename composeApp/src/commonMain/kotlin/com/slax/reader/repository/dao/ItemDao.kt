package com.slax.reader.repository.dao

import androidx.room.*

@Entity(tableName = "items")
data class Item(
    @PrimaryKey val id: String,
    val name: String
)

@Dao
interface ItemDao {
    @Query("SELECT * FROM items")
    suspend fun getAllItems(): List<Item>
}
