package com.slax.reader.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slax.reader.data.database.dao.BookmarkDao
import com.slax.reader.data.database.dao.CollectionDao
import com.slax.reader.data.database.dao.LocalBookmarkDao
import com.slax.reader.data.database.dao.UserDao
import com.slax.reader.data.database.model.BookmarkSortType
import com.slax.reader.data.database.model.CollectionBookmarkItem
import com.slax.reader.data.database.model.InboxListBookmarkItem
import com.slax.reader.data.database.model.SubscribedCollection
import com.slax.reader.domain.coordinator.CoordinatorDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InboxListViewModel(
    private val userDao: UserDao,
    private val bookmarkDao: BookmarkDao,
    private val collectionDao: CollectionDao,
    private val localBookmarkDao: LocalBookmarkDao,
    private val coordinatorDomain: CoordinatorDomain
) : ViewModel() {
    val userInfo = userDao.watchUserInfo()
    val syncState = coordinatorDomain.syncState

    val subscribedCollections: StateFlow<List<SubscribedCollection>> =
        collectionDao.watchSubscribedCollections()

    private val _activeCollectionOwnerId = MutableStateFlow<String?>(null)
    val activeCollectionOwnerId: StateFlow<String?> = _activeCollectionOwnerId.asStateFlow()

    val activeCollection: StateFlow<SubscribedCollection?> = combine(
        subscribedCollections,
        _activeCollectionOwnerId,
    ) { collections, ownerId ->
        collections.firstOrNull { it.ownerId == ownerId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val collectionBookmarks: StateFlow<List<CollectionBookmarkItem>> = _activeCollectionOwnerId
        .flatMapLatest { ownerId ->
            if (ownerId.isNullOrBlank()) flowOf(emptyList())
            else collectionDao.watchCollectionBookmarks(ownerId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _sortType = MutableStateFlow(BookmarkSortType.UPDATED)
    val sortType: StateFlow<BookmarkSortType> = _sortType.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val bookmarks: StateFlow<List<InboxListBookmarkItem>> = _sortType
        .flatMapLatest { type ->
            combine(
                bookmarkDao.watchUserBookmarkPaged(type),
                localBookmarkDao.watchUserLocalBookmarkMap()
            ) { bookmarks, localMap ->
                bookmarks?.map { bookmark ->
                    val local = localMap[bookmark.id]
                    if (local != null) {
                        bookmark.copy(downloadStatus = local.downloadStatus, isAutoCached = local.isAutoCached)
                    } else {
                        bookmark
                    }
                }
            }
        }
        .scan(emptyList<InboxListBookmarkItem>()) { prev, new ->
            if (new == null && prev.isNotEmpty()) prev else new ?: emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSortType(type: BookmarkSortType) {
        _sortType.value = type
    }

    fun selectCollection(ownerId: String?) {
        if (_activeCollectionOwnerId.value == ownerId) return
        _activeCollectionOwnerId.value = ownerId
        scrollToTop()
        if (ownerId != null) {
            viewModelScope.launch(Dispatchers.IO) {
                val collectionId = subscribedCollections.value
                    .firstOrNull { it.ownerId == ownerId }
                    ?.id
                    ?: return@launch
                runCatching { collectionDao.setLastRead(collectionId) }
                    .onFailure { error -> println("Failed to update collection last-read time: ${error.message}") }
            }
        }
    }
    val hasSynced = bookmarkDao.hasSynced

    private val _scrollToTopEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()

    private val _processingUrlEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val processingUrlEvent: SharedFlow<String> = _processingUrlEvent.asSharedFlow()

    fun scrollToTop() {
        _scrollToTopEvent.tryEmit(Unit)
    }

    fun emitProcessingUrl(url: String) {
        _processingUrlEvent.tryEmit(url)
    }

    suspend fun confirmEditTitle(bookmarkId: String, newTitle: String) {
        withContext(Dispatchers.IO) {
            bookmarkDao.updateBookmarkAliasTitle(bookmarkId, newTitle)
        }
    }

    suspend fun toggleStar(bookmarkId: String, isStar: Boolean) = withContext(Dispatchers.IO) {
        bookmarkDao.updateBookmarkStar(bookmarkId, if (isStar) 1 else 0)
    }

    suspend fun toggleArchive(bookmarkId: String, isArchive: Boolean) = withContext(Dispatchers.IO) {
        bookmarkDao.updateBookmarkArchive(bookmarkId, if (isArchive) 1 else 0)
    }

    suspend fun deleteBookmark(bookmarkId: String) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteBookmark(bookmarkId)
    }

    suspend fun addLinkBookmark(url: String) = withContext(Dispatchers.IO) {
        return@withContext bookmarkDao.createBookmark(url)
    }
}
