package com.slax.reader.ui.bookmarks

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.powersync.PowerSyncDatabase
import com.slax.reader.data.database.dao.BookmarkDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json


class BookmarkViewModel(
    private val bookmarkDao: BookmarkDao,
    private val database: PowerSyncDatabase
) : ViewModel() {
    var syncCompleted by mutableStateOf(false)
        private set
    var isInitialized by mutableStateOf(false)
        private set

    var syncStatus by mutableStateOf<SyncStatus>(SyncStatus.Initializing)
        private set

    private val tagNamesCache = mutableStateMapOf<String, String>()

    val bookmarks = bookmarkDao.getUserBookmarkList()
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val tags = bookmarkDao.getUserTags()
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
        launchTry { bookmarkDao.updateBookmarkStar(bookmarkId, if (currentState == 1) 0 else 1) }
    }

    fun toggleArchive(bookmarkId: String, currentState: Int) {
        launchTry { bookmarkDao.updateBookmarkArchive(bookmarkId, if (currentState == 1) 0 else 1) }
    }

    fun updateBookmarkTags(bookmarkId: String, newTagIds: List<String>) {
        launchTry { bookmarkDao.updateMetadataField(bookmarkId, "tags", Json.encodeToString(newTagIds)) }
    }

    fun disableBookmarkShare(bookmarkId: String) {
        launchTry { bookmarkDao.updateMetadataField(bookmarkId, "share.is_enable", Json.encodeToString(false)) }
    }

    fun deleteBookmark(bookmarkId: String) {
        launchTry { bookmarkDao.deleteBookmark(bookmarkId) }
    }

    fun createBookmark(url: String) {
        launchTry { bookmarkDao.createBookmark(url) }
    }

    fun launchTry(func: suspend () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    func()
                } catch (e: Exception) {
                    println("Error in launchTry: ${e.message}")
                }
            }
        }
    }

}