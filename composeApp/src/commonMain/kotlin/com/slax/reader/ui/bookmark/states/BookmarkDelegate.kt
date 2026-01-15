package com.slax.reader.ui.bookmark.states

import com.slax.reader.data.database.dao.BookmarkDao
import com.slax.reader.data.database.model.UserBookmark
import com.slax.reader.data.database.model.UserTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

data class BookmarkDetailState(
    val isStarred: Boolean = false,
    val isArchived: Boolean = false,
    val displayTitle: String = "",
    val displayTime: String = "",
    val metadataUrl: String? = null,
)

class BookmarkDelegate(
    private val bookmarkDao: BookmarkDao,
    private val bookmarkIdFlow: StateFlow<String?>,
    private val scope: CoroutineScope
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val bookmarkFlow: StateFlow<List<UserBookmark>> = bookmarkIdFlow
        .filterNotNull()
        .flatMapLatest { bookmarkDao.watchBookmarkDetail(it) }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkDetailState: StateFlow<BookmarkDetailState> = bookmarkFlow
        .map { list ->
            list.firstOrNull()?.let { b ->
                BookmarkDetailState(
                    isStarred = b.isStarred == 1,
                    isArchived = b.archiveStatus == 1,
                    displayTitle = b.displayTitle,
                    displayTime = b.displayTime,
                    metadataUrl = b.metadataUrl,
                )
            } ?: BookmarkDetailState()
        }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), BookmarkDetailState())

    @OptIn(ExperimentalCoroutinesApi::class)
    val userTagList: StateFlow<List<UserTag>> = bookmarkDao.watchUserTag()
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedTagList: StateFlow<Set<UserTag>> = bookmarkFlow
        .mapLatest { bookmarks ->
            val tagIds = bookmarks.firstOrNull()?.metadataObj?.tags ?: emptyList()
            if (tagIds.isEmpty()) emptySet() else getTagNames(tagIds).toHashSet()
        }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Lazily, emptySet())

    fun onToggleStar(isStar: Boolean) {
        scope.launch {
            runCatching { toggleStar(isStar) }
        }
    }

    fun onToggleArchive(isArchive: Boolean) {
        scope.launch {
            runCatching { toggleArchive(isArchive) }
        }
    }

    fun onUpdateBookmarkTags(bookmarkId: String, newTagIds: List<String>) {
        scope.launch {
            runCatching { updateBookmarkTags(bookmarkId, newTagIds) }
        }
    }

    fun onUpdateBookmarkTitle(newTitle: String) {
        scope.launch {
            runCatching { updateBookmarkTitle(newTitle) }
        }
    }

    suspend fun getTagNames(uuids: List<String>): List<UserTag> = withContext(Dispatchers.IO) {
        return@withContext bookmarkDao.getTagsByIds(uuids)
    }

    suspend fun createTag(tagName: String): UserTag = withContext(Dispatchers.IO) {
        return@withContext bookmarkDao.createTag(tagName)
    }

    suspend fun toggleStar(isStar: Boolean) = withContext(Dispatchers.IO) {
        bookmarkIdFlow.value?.let { id ->
            return@withContext bookmarkDao.updateBookmarkStar(id, if (isStar) 1 else 0)
        }
    }

    suspend fun toggleArchive(isArchive: Boolean) = withContext(Dispatchers.IO) {
        bookmarkIdFlow.value?.let { id ->
            return@withContext bookmarkDao.updateBookmarkArchive(id, if (isArchive) 1 else 0)
        }
    }

    suspend fun updateBookmarkTags(bookmarkId: String, newTagIds: List<String>) = withContext(Dispatchers.IO) {
        return@withContext bookmarkDao.updateMetadataField(bookmarkId, "tags", Json.encodeToString(newTagIds))
    }

    suspend fun updateBookmarkTitle(newTitle: String) = withContext(Dispatchers.IO) {
        bookmarkIdFlow.value?.let { id ->
            return@withContext bookmarkDao.updateBookmarkAliasTitle(id, newTitle)
        }
    }
}
