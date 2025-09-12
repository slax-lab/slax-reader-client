package com.slax.reader.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

// ==================== 数据模型 ====================

@Immutable
@Serializable
data class BookmarkMetadata<T>(
    val tags: List<String>,
    val share: ShareSettings?,
    val bookmark: T
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

    var metadataObj: BookmarkMetadata<BookmarkDetails>?,
    var metadataTitle: String?
) {
    val displayTitle: String
        get() = aliasTitle.ifEmpty { displayTitle() }

    fun displayTitle(): String {
        return metadataObj?.bookmark?.title ?: id.take(5)
    }

    fun parsedMetadata(): BookmarkMetadata<BookmarkDetails>? {
        return metadataObj ?: getTypedMetadata()
    }

    fun getTypedMetadata(): BookmarkMetadata<BookmarkDetails>? {
        return metadata?.let { json ->
            try {
                val obj = Json.decodeFromString<BookmarkMetadata<BookmarkDetails>>(json)
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
    val data: String? = null
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

    override suspend fun uploadData(database: PowerSyncDatabase) {
        val batch = mutableListOf<CrudEntry>()
        var lastTx: CrudTransaction? = null

        database.getCrudTransactions().takeWhile { batch.size < 100 }.collect {
            batch.addAll(it.crud)
            lastTx = it
        }

        if (batch.isEmpty()) return

        val postData = batch.map { entry ->
            ChangesItem(
                table = entry.table,
                id = entry.id,
                op = entry.op.toString(),
                data = entry.opData?.toString()
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
            throw Exception("Error uploading data: ${resp.status}")
        }

        val responseData = resp.body<HttpData<String>>()
        if (responseData.code != 200 || responseData.message != "ok") {
            throw Exception("Server error: ${responseData.code}")
        }

        lastTx?.complete(null)
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
                            it.downloadedOperations.toFloat() / it.totalOperations.toFloat()
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
    onToggleStar: () -> Unit,
    onToggleArchive: () -> Unit
) {
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
                TagsRow(tagNames)
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
                onToggleStar = onToggleStar,
                onToggleArchive = onToggleArchive
            )
        }
    }
}

@Composable
private fun TagsRow(tagNames: List<String>) {
    val tagsText = remember(tagNames) {
        tagNames.joinToString(" • ")
    }

    Text(
        text = tagsText,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.primary
    )
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
    onToggleStar: () -> Unit,
    onToggleArchive: () -> Unit
) {
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
    }
}