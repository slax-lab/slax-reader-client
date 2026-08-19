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
import com.slax.reader.domain.sync.CollectionBackgroundDomain
import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InboxListViewModel(
    private val userDao: UserDao,
    private val bookmarkDao: BookmarkDao,
    private val collectionDao: CollectionDao,
    private val localBookmarkDao: LocalBookmarkDao,
    private val coordinatorDomain: CoordinatorDomain,
) : ViewModel() {
    val userInfo = userDao.watchUserInfo()
    val syncState = coordinatorDomain.syncState

    val subscribedCollections: StateFlow<List<SubscribedCollection>> =
        collectionDao.watchSubscribedCollections()

    private val _activeCollectionId = MutableStateFlow<String?>(null)
    val activeCollectionId: StateFlow<String?> = _activeCollectionId.asStateFlow()

    val activeCollection: StateFlow<SubscribedCollection?> = combine(
        subscribedCollections,
        _activeCollectionId,
    ) { collections, collectionId ->
        collections.firstOrNull { it.id == collectionId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val collectionBookmarks: StateFlow<List<CollectionBookmarkItem>> = activeCollection
        .map { it?.ownerId }
        .distinctUntilChanged()
        .flatMapLatest { ownerId ->
            if (ownerId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                combine(
                    collectionDao.watchCollectionBookmarks(ownerId),
                    localBookmarkDao.watchUserLocalBookmarkMap(),
                ) { bookmarks, localMap ->
                    bookmarks.map { bookmark ->
                        val local = localMap[CollectionBackgroundDomain.cacheKey(ownerId, bookmark.id)]
                        if (local != null) {
                            bookmark.copy(
                                downloadStatus = local.downloadStatus,
                                isAutoCached = local.isAutoCached,
                            )
                        } else {
                            bookmark
                        }
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            subscribedCollections.collect { collections ->
                val selectedId = _activeCollectionId.value ?: return@collect
                if (collections.none { it.id == selectedId }) {
                    _activeCollectionId.value = null
                    scrollToTop()
                }
            }
        }
    }

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

    fun selectCollection(collectionId: String?) {
        if (collectionId != null && subscribedCollections.value.none { it.id == collectionId }) return
        if (_activeCollectionId.value == collectionId) return
        _activeCollectionId.value = collectionId
        scrollToTop()
        if (collectionId == null) return

        val hasNew = subscribedCollections.value
            .firstOrNull { it.id == collectionId }
            ?.hasNew == true
        if (!hasNew) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                collectionDao.setLastRead(collectionId)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                println("Failed to update collection last-read time: ${error.message}")
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
