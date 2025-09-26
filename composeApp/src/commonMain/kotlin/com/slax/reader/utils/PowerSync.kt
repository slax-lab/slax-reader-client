package com.slax.reader.utils

import com.powersync.PowerSyncDatabase
import com.powersync.connectors.PowerSyncBackendConnector
import com.powersync.connectors.PowerSyncCredentials
import com.powersync.db.crud.CrudEntry
import com.powersync.db.crud.CrudTransaction
import com.powersync.db.crud.SqliteRow
import com.slax.reader.data.database.model.BookmarkMetadata
import com.slax.reader.data.database.model.ShareSettings
import com.slax.reader.data.network.ApiService
import com.slax.reader.data.network.dto.ChangesItem
import kotlinx.coroutines.flow.take
import kotlinx.serialization.json.Json

class Connector(
    private val apiService: ApiService
) : PowerSyncBackendConnector() {

    override suspend fun fetchCredentials(): PowerSyncCredentials {
        val data = apiService.getSyncToken()
        return PowerSyncCredentials(
            endpoint = data.data?.endpoint ?: throw Exception("No endpoint"),
            token = data.data.token
        )
    }

    fun diffChanges(data: SqliteRow?, preData: SqliteRow?): Pair<Map<String, String?>?, Map<String, String?>?> {
        if (data == null) return Pair(null, null)

        val dataMap = data.toMap()
        val preDataMap = preData?.toMap() ?: return Pair(dataMap, null)

        val changes = mutableMapOf<String, String?>()
        val preChanges = mutableMapOf<String, String?>()

        for ((key, value) in dataMap) {
            val oldValue = preDataMap[key]
            if (value != oldValue) {
                if (key == "metadata") {
                    try {
                        val newMetadataObj = Json.decodeFromString<BookmarkMetadata>(value ?: "")
                        val oldMetadataObj = Json.decodeFromString<BookmarkMetadata>(oldValue ?: "")

                        if (newMetadataObj.tags != oldMetadataObj.tags) {
                            changes["metadata.tags"] = Json.encodeToString(newMetadataObj.tags)
                            preChanges["metadata.tags"] = Json.encodeToString(oldMetadataObj.tags)
                        }

                        val newShare = newMetadataObj.share
                        val oldShare = oldMetadataObj.share

                        if (newShare != null && oldShare != null) {
                            compareShareSettings(newShare, oldShare, changes, preChanges)
                        } else if (newShare != oldShare) {
                            changes["metadata.share"] = Json.encodeToString(newShare)
                            preChanges["metadata.share"] = Json.encodeToString(oldShare)
                        }

                    } catch (e: Exception) {
                        println("Error comparing metadata JSON: ${e.message}")
                        changes["metadata"] = value
                        preChanges["metadata"] = oldValue
                    }
                } else {
                    changes[key] = value
                    preChanges[key] = oldValue
                }
            }
        }

        return Pair(
            changes.ifEmpty { null },
            preChanges.ifEmpty { null }
        )
    }

    private fun compareShareSettings(
        newShare: ShareSettings,
        oldShare: ShareSettings,
        changes: MutableMap<String, String?>,
        preChanges: MutableMap<String, String?>
    ) {
        if (newShare.is_enable != oldShare.is_enable) {
            changes["metadata.share.is_enable"] = newShare.is_enable.toString()
            preChanges["metadata.share.is_enable"] = oldShare.is_enable.toString()
        }
        if (newShare.show_line != oldShare.show_line) {
            changes["metadata.share.show_line"] = newShare.show_line.toString()
            preChanges["metadata.share.show_line"] = oldShare.show_line.toString()
        }
        if (newShare.allow_line != oldShare.allow_line) {
            changes["metadata.share.allow_line"] = newShare.allow_line.toString()
            preChanges["metadata.share.allow_line"] = oldShare.allow_line.toString()
        }
        if (newShare.show_comment != oldShare.show_comment) {
            changes["metadata.share.show_comment"] = newShare.show_comment.toString()
            preChanges["metadata.share.show_comment"] = oldShare.show_comment.toString()
        }
        if (newShare.allow_comment != oldShare.allow_comment) {
            changes["metadata.share.allow_comment"] = newShare.allow_comment.toString()
            preChanges["metadata.share.allow_comment"] = oldShare.allow_comment.toString()
        }
        if (newShare.show_userinfo != oldShare.show_userinfo) {
            changes["metadata.share.show_userinfo"] = newShare.show_userinfo.toString()
            preChanges["metadata.share.show_userinfo"] = oldShare.show_userinfo.toString()
        }
        if (newShare.share_code != oldShare.share_code) {
            changes["metadata.share.share_code"] = newShare.share_code
            preChanges["metadata.share.share_code"] = oldShare.share_code
        }
        if (newShare.created_at != oldShare.created_at) {
            changes["metadata.share.created_at"] = newShare.created_at
            preChanges["metadata.share.created_at"] = oldShare.created_at
        }
    }

    override suspend fun uploadData(database: PowerSyncDatabase) {
        val transactions = mutableListOf<CrudTransaction>()
        val batch = mutableListOf<CrudEntry>()

        database.getCrudTransactions()
            .take(100)
            .collect { tx ->
                batch.addAll(tx.crud)
                transactions.add(tx)
            }

        if (batch.isEmpty()) return

        try {
            val postData = batch.map { entry ->
                val (changes, preChanges) = diffChanges(entry.opData, entry.previousValues)
                ChangesItem(
                    table = entry.table,
                    id = entry.id,
                    op = entry.op.toString(),
                    data = changes,
                    preData = preChanges
                )
            }

            apiService.uploadChanges(changes = postData)
            
            transactions.forEach { it.complete(null) }

            println("Successfully uploaded ${transactions.size} transactions with ${batch.size} operations")

        } catch (e: Exception) {
            throw e
        }
    }
}
