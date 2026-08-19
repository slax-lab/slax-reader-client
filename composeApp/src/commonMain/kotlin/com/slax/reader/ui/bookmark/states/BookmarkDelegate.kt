package com.slax.reader.ui.bookmark.states

import com.slax.reader.data.database.dao.BookmarkDao
import com.slax.reader.data.database.dao.CollectionDao
import com.slax.reader.data.database.model.UserBookmark
import com.slax.reader.data.database.model.UserTag
import com.slax.reader.domain.sync.CollectionBackgroundDomain
import com.slax.reader.utils.bookmarkEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

data class ScrollInfo(val scrollY: Float, val isNearBottom: Boolean)

data class BookmarkDetailBinding(
    val bookmarkId: String,
    val collectionOwnerId: String?,
    val collectionId: String?,
) {
    val isCollection: Boolean
        get() = collectionOwnerId != null || collectionId != null

    val validCollectionOwnerId: String?
        get() = collectionOwnerId?.takeIf { it.isNotBlank() }

    fun resolveCacheKey(): String = validCollectionOwnerId
        ?.let { CollectionBackgroundDomain.cacheKey(it, bookmarkId) }
        ?: bookmarkId
}

data class BookmarkDetailState(
    val isStarred: Boolean = false,
    val isArchived: Boolean = false,
    val displayTitle: String = "",
    val displayTime: String = "",
    val metadataUrl: String? = null,
)

class BookmarkDelegate(
    private val bookmarkDao: BookmarkDao,
    private val collectionDao: CollectionDao,
    private val bindingFlow: StateFlow<BookmarkDetailBinding?>,
    private val scope: CoroutineScope
) {
    private data class BoundBookmarks(
        val binding: BookmarkDetailBinding,
        val bookmarks: List<UserBookmark>,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val boundBookmarks: StateFlow<BoundBookmarks?> = bindingFlow
        .filterNotNull()
        .flatMapLatest { binding ->
            val collectionOwnerId = binding.validCollectionOwnerId
            val bookmarks = when {
                !binding.isCollection -> bookmarkDao.watchBookmarkDetail(binding.bookmarkId)
                collectionOwnerId != null -> collectionDao.watchCollectionBookmarkDetail(
                    binding.bookmarkId,
                    collectionOwnerId,
                )
                else -> flowOf(emptyList())
            }
            bookmarks.map { BoundBookmarks(binding, it) }
        }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), null)

    private val currentBoundBookmarks: Flow<BoundBookmarks?> = combine(
        bindingFlow,
        boundBookmarks,
    ) { currentBinding, bound ->
        bound?.takeIf { it.binding == currentBinding }
    }.distinctUntilChanged()

    private val bookmarkFlow: Flow<List<UserBookmark>> = currentBoundBookmarks
        .map { it?.bookmarks.orEmpty() }
        .distinctUntilChanged()

    val currentBookmark: Flow<UserBookmark> = currentBoundBookmarks
        .filterNotNull()
        .mapNotNull { bound -> bound.bookmarks.firstOrNull { it.id == bound.binding.bookmarkId } }
        .distinctUntilChanged()

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
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedTagList: StateFlow<Set<UserTag>> = bookmarkFlow
        .map { bookmarks -> bookmarks.firstOrNull()?.metadataObj?.tags ?: emptyList() }
        .distinctUntilChanged()
        .mapLatest { tagIds ->
            if (tagIds.isEmpty()) emptySet() else getTagNames(tagIds).toHashSet()
        }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun onToggleStar(isStar: Boolean) {
        if (isCollectionBookmark()) return
        scope.launch {
            runCatching { toggleStar(isStar) }
            bookmarkEvent
                .action("star")
                .param("is_starred", if (isStar) "star" else "unstar")
                .source("detail")
                .send()
        }
    }

    fun onToggleArchive(isArchive: Boolean) {
        if (isCollectionBookmark()) return
        scope.launch {
            runCatching { toggleArchive(isArchive) }
            bookmarkEvent
                .action("archive")
                .param("is_archived", if (isArchive) "archive" else "unarchive")
                .source("detail").send()
        }
    }

    fun onUpdateBookmarkTags(bookmarkId: String, newTagIds: List<String>) {
        if (isCollectionBookmark()) return
        scope.launch {
            runCatching { updateBookmarkTags(bookmarkId, newTagIds) }
        }
    }

    fun onUpdateBookmarkTitle(newTitle: String) {
        if (isCollectionBookmark()) return
        scope.launch {
            runCatching { updateBookmarkTitle(newTitle) }
        }
    }

    suspend fun deleteBookmark(): Unit = withContext(Dispatchers.IO) {
        if (isCollectionBookmark()) return@withContext
        bindingFlow.value?.bookmarkId?.let { id ->
            bookmarkDao.deleteBookmark(id)
        }
    }

    suspend fun getTagNames(uuids: List<String>): List<UserTag> = withContext(Dispatchers.IO) {
        return@withContext bookmarkDao.getTagsByIds(uuids)
    }

    suspend fun createTag(tagName: String): UserTag = withContext(Dispatchers.IO) {
        return@withContext bookmarkDao.createTag(tagName)
    }

    suspend fun toggleStar(isStar: Boolean) = withContext(Dispatchers.IO) {
        if (isCollectionBookmark()) return@withContext
        bindingFlow.value?.bookmarkId?.let { id ->
            return@withContext bookmarkDao.updateBookmarkStar(id, if (isStar) 1 else 0)
        }
    }

    suspend fun toggleArchive(isArchive: Boolean) = withContext(Dispatchers.IO) {
        if (isCollectionBookmark()) return@withContext
        bindingFlow.value?.bookmarkId?.let { id ->
            return@withContext bookmarkDao.updateBookmarkArchive(id, if (isArchive) 1 else 0)
        }
    }

    suspend fun updateBookmarkTags(bookmarkId: String, newTagIds: List<String>) = withContext(Dispatchers.IO) {
        if (isCollectionBookmark()) return@withContext
        return@withContext bookmarkDao.updateMetadataField(bookmarkId, "tags", Json.encodeToString(newTagIds))
    }

    suspend fun updateBookmarkTitle(newTitle: String) = withContext(Dispatchers.IO) {
        if (isCollectionBookmark()) return@withContext
        bindingFlow.value?.bookmarkId?.let { id ->
            return@withContext bookmarkDao.updateBookmarkAliasTitle(id, newTitle)
        }
    }

    private fun isCollectionBookmark(): Boolean = bindingFlow.value?.isCollection == true
}
