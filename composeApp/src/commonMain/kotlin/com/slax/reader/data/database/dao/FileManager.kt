package com.slax.reader.data.database.dao

import com.powersync.PowerSyncDatabase

class FileManagerDao(
    private val db: PowerSyncDatabase
) {

    suspend fun addBookmark(id: String, name: String, path: String, size: Int) {
        db.writeTransaction { transaction ->
            transaction.execute(
                "INSERT INTO local_file_manager (id, name, path, size, source_id) VALUES (?, ?, ?, ?, ?)",
                listOf(id, name, path, size, "bookmark")
            )
        }
    }

    suspend fun addBookmarkImage(sourceId: String, size: Int) {}

    suspend fun deleteBookmark() {}

    suspend fun bookmarkFileSize() {}
}