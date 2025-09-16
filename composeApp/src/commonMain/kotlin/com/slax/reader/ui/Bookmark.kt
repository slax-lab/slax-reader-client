package com.slax.reader.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.powersync.ExperimentalPowerSyncAPI
import com.powersync.PowerSyncDatabase
import com.powersync.connectors.PowerSyncBackendConnector
import com.powersync.connectors.PowerSyncCredentials
import com.powersync.db.crud.CrudEntry
import com.powersync.db.crud.CrudTransaction
import com.powersync.db.crud.SqliteRow
import com.powersync.db.getString
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// ==================== 数据模型 ====================
@Immutable
@Serializable
data class BookmarkMetadata(
    val tags: List<String>,
    val share: ShareSettings?,
    val bookmark: BookmarkDetails
)

@Immutable
@Serializable
data class ShareSettings(
    val uuid: String,
    val is_enable: Boolean,
    val show_line: Boolean,
    val allow_line: Boolean,
    val created_at: String,
    val share_code: String,
    val show_comment: Boolean,
    val allow_comment: Boolean,
    val show_userinfo: Boolean
)

@Immutable
@Serializable
data class BookmarkDetails(
    val uuid: String,
    val title: String,
    val byline: String,
    val status: String,
    val host_url: String,
    val site_name: String,
    val target_url: String,
    val description: String,
    val content_icon: String,
    val published_at: String,
    val content_cover: String,
    val content_word_count: Int
)

@Immutable
@Serializable
data class UserTag(
    val id: String,
    val tag_name: String,
    val display: String,
    val created_at: String
)

@Immutable
data class UserBookmark(
    val id: String,
    val isRead: Int,
    val archiveStatus: Int,
    val isStarred: Int,
    val createdAt: String,
    val updatedAt: String,
    val aliasTitle: String,
    val type: Int,
    val deletedAt: String?,
    val metadata: String?,

    var metadataObj: BookmarkMetadata?,
    var metadataTitle: String?
) {
    val displayTitle: String
        get() = aliasTitle.ifEmpty { displayTitle() }

    fun displayTitle(): String {
        return metadataObj?.bookmark?.title ?: id.take(5)
    }

    fun parsedMetadata(): BookmarkMetadata? {
        return metadataObj ?: getTypedMetadata()
    }

    fun getTypedMetadata(): BookmarkMetadata? {
        return metadata?.let { json ->
            try {
                val obj = Json.decodeFromString<BookmarkMetadata>(json)
                metadataObj = obj
                obj
            } catch (e: Exception) {
                println("Error parsing metadata: ${e.message}")
                null
            }
        }
    }
}

// ==================== 网络层 ====================

@Serializable
data class HttpData<T>(
    val data: T?,
    val message: String,
    val code: Int
)

@Serializable
data class CredentialsData(
    val endpoint: String = "",
    val token: String = ""
)

@Serializable
data class ChangesItem(
    val table: String = "",
    val id: String = "",
    val op: String = "",
    val data: Map<String, String?>? = null,
    val preData: Map<String, String?>? = null
)

// HTTP 客户端单例
private val httpClient = HttpClient {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
}

private const val AUTH_TOKEN = ""

class Connector : PowerSyncBackendConnector() {
    override suspend fun fetchCredentials(): PowerSyncCredentials {
        val response = httpClient.post("https://reader-api.slax.dev/v1/sync/token") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $AUTH_TOKEN")
                append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }
        }

        if (response.status != HttpStatusCode.OK) {
            throw Exception("Error fetching credentials: ${response.status}")
        }

        val data = response.body<HttpData<CredentialsData>>()
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

            val resp = httpClient.post("https://reader-api.slax.dev/v1/sync/changes") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $AUTH_TOKEN")
                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
                setBody(postData)
            }

            if (resp.status != HttpStatusCode.OK) {
                val error = Exception("Error uploading data: ${resp.status}")
                transactions.forEach { it.complete(null) }
                throw error
            }

            if (resp.status != HttpStatusCode.OK) {
                val error = Exception("Error uploading data: ${resp.status}")
                throw error
            }

            transactions.forEach { it.complete(null) }

            println("Successfully uploaded ${transactions.size} transactions with ${batch.size} operations")

        } catch (e: Exception) {
            throw e
        }
    }
}

// ==================== ViewModel ====================

class BookmarkViewModel(
    val database: PowerSyncDatabase,
) : ViewModel() {
    var syncCompleted by mutableStateOf(false)
        private set
    var isInitialized by mutableStateOf(false)
        private set

    var syncStatus by mutableStateOf<SyncStatus>(SyncStatus.Initializing)
        private set

    private val tagNamesCache = mutableStateMapOf<String, String>()

    val bookmarks: StateFlow<List<UserBookmark>> =
        getUserBookmarkList(database)
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val tags: StateFlow<Map<String, UserTag>> =
        getUserTags(database)
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyMap()
            )

    init {
        viewModelScope.launch {
            tags.collect { tagsMap ->
                tagNamesCache.clear()
                tagsMap.forEach { (id, tag) ->
                    tagNamesCache[id] = tag.tag_name
                }
            }
        }

        viewModelScope.launch {
            database.currentStatus.asFlow().collect { status ->
                isInitialized = status.connected

                syncStatus = when {
                    !status.connected -> SyncStatus.Disconnected
                    status.downloading -> SyncStatus.Syncing(
                        progress = status.downloadProgress?.let {
                            if (it.totalOperations > 0) {
                                val progress = it.downloadedOperations.toFloat() / it.totalOperations.toFloat()
                                progress.coerceIn(0f, 1f)
                            } else {
                                0f
                            }
                        } ?: 0f
                    )

                    else -> SyncStatus.Synced
                }

                syncCompleted = status.connected && !status.downloading
            }
        }
    }

    fun getTagNames(tagIds: List<String>): List<String> {
        return tagIds.mapNotNull { tagNamesCache[it] }
    }

    fun toggleStar(bookmarkId: String, currentState: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    database.writeTransaction { tx ->
                        tx.execute(
                            "UPDATE sr_user_bookmark SET is_starred = ? WHERE id = ?",
                            listOf(if (currentState == 1) 0 else 1, bookmarkId)
                        )
                    }
                } catch (e: Exception) {
                    println("Error toggling star: ${e.message}")
                }
            }
        }
    }

    fun toggleArchive(bookmarkId: String, currentState: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    database.writeTransaction { tx ->
                        tx.execute(
                            "UPDATE sr_user_bookmark SET archive_status = ? WHERE id = ?",
                            listOf(if (currentState == 1) 0 else 1, bookmarkId)
                        )
                    }
                } catch (e: Exception) {
                    println("Error toggling archive: ${e.message}")
                }
            }
        }
    }

    fun updateBookmarkTags(bookmarkId: String, newTagIds: List<String>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    updateMetadataField(bookmarkId, "tags", Json.encodeToString(newTagIds))
                } catch (e: Exception) {
                    println("Error updating bookmark tags: ${e.message}")
                }
            }
        }
    }

    fun disableBookmarkShare(bookmarkId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    updateMetadataField(bookmarkId, "share.is_enable", Json.encodeToString(false))
                } catch (e: Exception) {
                    println("Error disabling bookmark share: ${e.message}")
                }
            }
        }
    }

    private suspend fun updateMetadataField(bookmarkId: String, fieldPath: String, jsonValue: String) {
        database.writeTransaction { tx ->
            tx.execute(
                "UPDATE sr_user_bookmark SET metadata = JSON_SET(COALESCE(metadata, '{}'), '$.$fieldPath', JSON(?)) WHERE id = ?",
                listOf(jsonValue, bookmarkId)
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    fun deleteBookmark(bookmarkId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val now = Clock.System.now().toString()
                    database.writeTransaction { tx ->
                        tx.execute(
                            "UPDATE sr_user_bookmark SET deleted_at = ? WHERE id = ?",
                            listOf(now, bookmarkId)
                        )
                    }
                } catch (e: Exception) {
                    println("Error deleting bookmark: ${e.message}")
                }
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
    fun createBookmark(url: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val bookmarkId = Uuid.random().toString()
                    val now = Clock.System.now().toString()

                    database.writeTransaction { tx ->
                        tx.execute(
                            """INSERT INTO sr_user_bookmark 
                            (id, is_read, archive_status, is_starred, created_at, updated_at, 
                             alias_title, type, deleted_at, metadata) 
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                            listOf(
                                bookmarkId,
                                0, // is_read
                                0, // archive_status
                                0, // is_starred
                                now, // created_at
                                now, // updated_at
                                "", // alias_title
                                0, // type
                                null, // deleted_at
                                Json.encodeToString(
                                    BookmarkMetadata(
                                        tags = emptyList(),
                                        share = null,
                                        bookmark = BookmarkDetails(
                                            uuid = bookmarkId,
                                            title = "New Bookmark",
                                            byline = "",
                                            status = "pending",
                                            host_url = url,
                                            site_name = "",
                                            target_url = url,
                                            description = "",
                                            content_icon = "",
                                            published_at = now,
                                            content_cover = "",
                                            content_word_count = 0
                                        )
                                    )
                                )
                            )
                        )
                    }
                } catch (e: Exception) {
                    println("Error creating bookmark: ${e.message}")
                }
            }
        }
    }
}

sealed class SyncStatus {
    object Initializing : SyncStatus()
    object Disconnected : SyncStatus()
    data class Syncing(val progress: Float) : SyncStatus()
    object Synced : SyncStatus()
}

// ==================== 数据获取函数 ====================

fun getUserBookmarkList(database: PowerSyncDatabase): Flow<List<UserBookmark>> {
    return database.watch(
        "SELECT is_read, archive_status, is_starred, created_at, updated_at, alias_title, type, deleted_at, metadata, id FROM sr_user_bookmark ORDER BY created_at DESC"
    ) { cursor ->
        UserBookmark(
            id = cursor.getString("id"),
            isRead = cursor.getString("is_read").toIntOrNull() ?: 0,
            archiveStatus = cursor.getString("archive_status").toIntOrNull() ?: 0,
            isStarred = cursor.getString("is_starred").toIntOrNull() ?: 0,
            createdAt = cursor.getString("created_at"),
            updatedAt = cursor.getString("updated_at"),
            aliasTitle = cursor.getString("alias_title"),
            type = cursor.getString("type").toIntOrNull() ?: 0,
            deletedAt = try {
                cursor.getString("deleted_at")
            } catch (_: Exception) {
                null
            },
            metadataObj = null,
            metadataTitle = null,
            metadata = cursor.getString("metadata"),
        )
    }.flowOn(Dispatchers.IO)
        .catch { e ->
            println("Error watching user bookmarks: ${e.message}")
        }
}

fun getUserTags(database: PowerSyncDatabase): Flow<Map<String, UserTag>> {
    return database.watch(
        "SELECT id, user_id, tag_name, display, created_at FROM sr_user_tag"
    ) { cursor ->
        UserTag(
            id = cursor.getString("id"),
            tag_name = cursor.getString("tag_name"),
            display = cursor.getString("display"),
            created_at = cursor.getString("created_at")
        )
    }.flowOn(Dispatchers.IO)
        .catch { e ->
            println("Error watching user tags: ${e.message}")
        }
        .map { tagsList ->
            tagsList.associateBy { it.id }
        }
}
// ==================== UI 组件 ====================

@Composable
@OptIn(ExperimentalPowerSyncAPI::class)
fun UserBookmarksScreen(viewModel: BookmarkViewModel) {
    val bookmarks by viewModel.bookmarks.collectAsState()
    val syncStatus = viewModel.syncStatus
    var showCreateDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            BookmarksHeader()

            Spacer(modifier = Modifier.height(16.dp))

            StatusCard(syncStatus = syncStatus)

            Spacer(modifier = Modifier.height(16.dp))

            BookmarksListHeader(count = bookmarks.size)

            Spacer(modifier = Modifier.height(8.dp))

            BookmarksList(
                bookmarks = bookmarks,
                viewModel = viewModel
            )
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("+", fontSize = 24.sp)
        }
    }

    if (showCreateDialog) {
        CreateBookmarkDialog(
            viewModel = viewModel,
            onDismiss = { showCreateDialog = false }
        )
    }
}

@Composable
private fun BookmarksHeader() {
    Text(
        text = "User Bookmarks",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun StatusCard(syncStatus: SyncStatus) {
    val (backgroundColor, statusText) = when (syncStatus) {
        is SyncStatus.Initializing -> Pair(
            MaterialTheme.colorScheme.surfaceVariant,
            "Initializing..."
        )

        is SyncStatus.Disconnected -> Pair(
            MaterialTheme.colorScheme.errorContainer,
            "Disconnected"
        )

        is SyncStatus.Syncing -> Pair(
            MaterialTheme.colorScheme.primaryContainer,
            "Syncing... ${(syncStatus.progress * 100).toInt()}%"
        )

        is SyncStatus.Synced -> Pair(
            MaterialTheme.colorScheme.primaryContainer,
            "Connected & Synced"
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Status: $statusText",
                fontWeight = FontWeight.Medium
            )

            if (syncStatus is SyncStatus.Syncing) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { syncStatus.progress },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun BookmarksListHeader(count: Int) {
    Text(
        text = "User Bookmarks List: ($count)",
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun BookmarksList(
    bookmarks: List<UserBookmark>,
    viewModel: BookmarkViewModel
) {
    val onToggleStar = remember<(String, Int) -> Unit> {
        { id, state -> viewModel.toggleStar(id, state) }
    }

    val onToggleArchive = remember<(String, Int) -> Unit> {
        { id, state -> viewModel.toggleArchive(id, state) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        items(
            items = bookmarks,
            key = { it.id }
        ) { bookmark ->
            BookmarkCard(
                bookmark = bookmark,
                tagNames = remember(bookmark.metadataObj?.tags) {
                    viewModel.getTagNames(bookmark.metadataObj?.tags ?: emptyList())
                },
                viewModel = viewModel,
                onToggleStar = { onToggleStar(bookmark.id, bookmark.isStarred) },
                onToggleArchive = { onToggleArchive(bookmark.id, bookmark.archiveStatus) }
            )
        }
    }
}

@Composable
private fun BookmarkCard(
    bookmark: UserBookmark,
    tagNames: List<String>,
    viewModel: BookmarkViewModel,
    onToggleStar: () -> Unit,
    onToggleArchive: () -> Unit
) {
    var showTagDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = bookmark.displayTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 2
            )

            if (tagNames.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                TagsRow(tagNames) { showTagDialog = true }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { showTagDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ 添加标签", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            BookmarkStatusRow(bookmark)

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "收藏时间: ${bookmark.createdAt}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            BookmarkActions(
                bookmark = bookmark,
                viewModel = viewModel,
                onToggleStar = onToggleStar,
                onToggleArchive = onToggleArchive
            )
        }
    }

    if (showTagDialog) {
        TagSelectionDialog(
            bookmark = bookmark,
            viewModel = viewModel,
            onDismiss = { showTagDialog = false }
        )
    }
}

@Composable
private fun TagsRow(tagNames: List<String>, onClick: () -> Unit = {}) {
    val tagsText = remember(tagNames) {
        tagNames.joinToString(" • ")
    }

    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = tagsText,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun BookmarkStatusRow(bookmark: UserBookmark) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatusText(
            text = if (bookmark.parsedMetadata()?.share?.is_enable == true) "已分享" else "未分享",
            isPrimary = bookmark.parsedMetadata()?.share?.is_enable == true
        )

        StatusText(
            text = if (bookmark.archiveStatus == 1) "已归档" else "未归档",
            isPrimary = bookmark.archiveStatus == 1
        )

        StatusText(
            text = if (bookmark.isStarred == 1) "★ 星标" else "☆ 未星标",
            isPrimary = bookmark.isStarred == 1
        )
    }
}

@Composable
private fun StatusText(text: String, isPrimary: Boolean) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = if (isPrimary) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    )
}

@Composable
private fun BookmarkActions(
    bookmark: UserBookmark,
    viewModel: BookmarkViewModel,
    onToggleStar: () -> Unit,
    onToggleArchive: () -> Unit
) {
    val isShared = bookmark.parsedMetadata()?.share?.is_enable == true

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(
            onClick = onToggleStar,
            modifier = Modifier.weight(1f)
        ) {
            Text(text = if (bookmark.isStarred == 1) "取消星标" else "添加星标")
        }

        TextButton(
            onClick = onToggleArchive,
            modifier = Modifier.weight(1f)
        ) {
            Text(text = if (bookmark.archiveStatus == 1) "取消归档" else "归档")
        }

        if (isShared) {
            TextButton(
                onClick = { viewModel.disableBookmarkShare(bookmark.id) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("取消分享")
            }
        }

        TextButton(
            onClick = { viewModel.deleteBookmark(bookmark.id) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("删除")
        }
    }
}

@Composable
private fun TagSelectionDialog(
    bookmark: UserBookmark,
    viewModel: BookmarkViewModel,
    onDismiss: () -> Unit
) {
    val allTags by viewModel.tags.collectAsState()
    val currentTagIds = bookmark.parsedMetadata()?.tags ?: emptyList()
    val selectedTagIds = remember {
        mutableStateMapOf<String, Boolean>().apply {
            allTags.keys.forEach { tagId ->
                this[tagId] = tagId in currentTagIds
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑标签") },
        text = {
            LazyColumn(
                modifier = Modifier.height(300.dp)
            ) {
                items(allTags.entries.toList()) { (tagId, tag) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedTagIds[tagId] ?: false,
                            onCheckedChange = { checked ->
                                selectedTagIds[tagId] = checked
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tag.tag_name)
                        Spacer(modifier = Modifier.weight(1f))
                        if (selectedTagIds[tagId] == true) {
                            TextButton(
                                onClick = {
                                    selectedTagIds[tagId] = false
                                }
                            ) {
                                Text("移除", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newTagIds = selectedTagIds.filter { it.value }.keys.toList()
                    viewModel.updateBookmarkTags(bookmark.id, newTagIds)
                    onDismiss()
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun CreateBookmarkDialog(
    viewModel: BookmarkViewModel,
    onDismiss: () -> Unit
) {
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建书签") },
        text = {
            Column {
                Text("输入要收藏的网址:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    placeholder = { Text("https://example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (url.isNotBlank()) {
                        viewModel.createBookmark(url.trim())
                        onDismiss()
                    }
                },
                enabled = url.isNotBlank()
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}