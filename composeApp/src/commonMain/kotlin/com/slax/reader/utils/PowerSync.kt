package com.slax.reader.utils

import com.powersync.ExperimentalPowerSyncAPI
import com.powersync.PowerSyncDatabase
import com.powersync.connectors.PowerSyncBackendConnector
import com.powersync.connectors.PowerSyncCredentials
import com.powersync.db.crud.CrudTransaction
import com.powersync.db.crud.SqliteRow
import com.powersync.sync.SyncOptions
import com.powersync.utils.JsonParam
import com.slax.reader.data.database.model.BookmarkMetadata
import com.slax.reader.data.database.model.ShareSettings
import com.slax.reader.data.network.ApiService
import com.slax.reader.data.network.dto.ChangesItem
import com.slax.reader.data.preferences.AppPreferences
import com.slax.reader.data.preferences.PowerSyncAuthInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

val ConnectParams = mapOf("schema_version" to JsonParam.String("1"))

@OptIn(ExperimentalPowerSyncAPI::class)
val ConnectOptions = SyncOptions(newClientImplementation = true)

/** 每次上传请求最多携带的 CRUD 操作数量,小批量多次请求,避免一次性把大量变更打到服务器。 */
private const val UPLOAD_CHUNK_SIZE = 20

/** 相邻两次上传请求之间的间隔(毫秒),给服务器一点喘息时间。 */
private const val UPLOAD_CHUNK_DELAY_MS = 200L

class Connector(
    private val apiService: ApiService,
    private val preferences: AppPreferences
) : PowerSyncBackendConnector() {

    @OptIn(ExperimentalTime::class)
    override suspend fun fetchCredentials(): PowerSyncCredentials {
        val authInfo = preferences.getPowerSyncToken()
        if (authInfo != null) {
            val refreshTime = Instant.fromEpochMilliseconds(authInfo.refreshTime)
            val current = kotlin.time.Clock.System.now()
            if (current - refreshTime < 1.hours) return PowerSyncCredentials(
                endpoint = authInfo.connectUrl,
                token = authInfo.token
            )
        }
        val data = apiService.getSyncToken()
        preferences.setPowerSyncToken(
            PowerSyncAuthInfo(
                connectUrl = data.data?.endpoint ?: throw Exception("No endpoint"),
                token = data.data.token,
                refreshTime = kotlin.time.Clock.System.now().toEpochMilliseconds()
            )
        )
        return PowerSyncCredentials(
            endpoint = data.data.endpoint,
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

        database.getCrudTransactions()
            .take(100)
            .collect { tx ->
                transactions.add(tx)
            }

        if (transactions.isEmpty()) return

        // 把事务按 CRUD 操作数量攒成小批次:单个事务不拆分(保证原子性),
        // 按顺序逐批上传并逐批 complete —— 这样即使某一批失败,已成功的批次也不会在重试时重复上传。
        val pendingBatch = mutableListOf<CrudTransaction>()
        var pendingOps = 0

        suspend fun flush() {
            if (pendingBatch.isEmpty()) return

            val postData = pendingBatch.flatMap { tx -> tx.crud }.map { entry ->
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
            pendingBatch.forEach { it.complete(null) }

            println("Successfully uploaded ${pendingBatch.size} transactions with ${postData.size} operations")

            pendingBatch.clear()
            pendingOps = 0
        }

        var isFirstBatch = true
        for (tx in transactions) {
            // 当前批次已有内容,且再加入这个事务会超过阈值时,先把已攒的批次发出去。
            if (pendingOps > 0 && pendingOps + tx.crud.size > UPLOAD_CHUNK_SIZE) {
                if (!isFirstBatch) delay(UPLOAD_CHUNK_DELAY_MS)
                flush()
                isFirstBatch = false
            }
            pendingBatch.add(tx)
            pendingOps += tx.crud.size
        }

        if (pendingBatch.isNotEmpty()) {
            if (!isFirstBatch) delay(UPLOAD_CHUNK_DELAY_MS)
            flush()
        }
    }
}
