package com.slax.reader.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slax.reader.data.database.dao.BookmarkDao
import com.slax.reader.data.database.dao.LocalBookmarkDao
import com.slax.reader.data.database.dao.UserDao
import com.slax.reader.data.database.model.InboxListBookmarkItem
import com.slax.reader.domain.coordinator.CoordinatorDomain
import com.slax.reader.domain.bookmark.BookmarkActionBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InboxListViewModel(
    private val userDao: UserDao,
    private val bookmarkDao: BookmarkDao,
    private val localBookmarkDao: LocalBookmarkDao,
    private val coordinatorDomain: CoordinatorDomain,
    private val bookmarkActionBus: BookmarkActionBus,
) : ViewModel() {
    val userInfo = userDao.watchUserInfo()
    val syncState = coordinatorDomain.syncState

    val bookmarks: StateFlow<List<InboxListBookmarkItem>> = combine(
        bookmarkDao.watchUserBookmarkList(),
        localBookmarkDao.watchUserLocalBookmarkMap()
    ) { bookmarks, localMap ->
        bookmarks.map { bookmark ->
            val local = localMap[bookmark.id]
            if (local != null) {
                bookmark.copy(downloadStatus = local.downloadStatus, isAutoCached = local.isAutoCached)
            } else {
                bookmark
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val hasSynced = bookmarkDao.hasSynced

    private val _scrollToTopEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()

    private val _pendingDeleteId = MutableStateFlow<String?>(null)
    val pendingDeleteId: StateFlow<String?> = _pendingDeleteId.asStateFlow()

    private val _processingUrlEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val processingUrlEvent: SharedFlow<String> = _processingUrlEvent.asSharedFlow()

    // 兜底删除定时器，确保动画被中断时也能执行删除
    private var pendingDeleteJob: Job? = null

    fun scrollToTop() {
        _scrollToTopEvent.tryEmit(Unit)
    }

    /**
     * 当列表页 composable 进入组合树时调用，
     * 从 BookmarkActionBus 消费待删除 ID 并触发动画。
     * 确保动画只在列表页可见时才播放。
     */
    fun activatePendingDelete() {
        val id = bookmarkActionBus.consumePendingDelete() ?: return
        _pendingDeleteId.value = id
        startDeleteFallback(id)
    }

    fun onDeleteAnimationFinished() {
        val id = _pendingDeleteId.value
        _pendingDeleteId.value = null
        pendingDeleteJob?.cancel()
        if (id != null) {
            viewModelScope.launch { commitDelete(id) }
        }
    }

    private suspend fun commitDelete(bookmarkId: String) {
        withContext(Dispatchers.IO) { bookmarkDao.deleteBookmark(bookmarkId) }
    }

    private fun startDeleteFallback(id: String) {
        pendingDeleteJob?.cancel()
        pendingDeleteJob = viewModelScope.launch {
            delay(3000L)
            commitDelete(id)
        }
    }

    /**
     * 列表页直接触发删除（长按菜单），
     * 无需经过 ActionBus 缓冲，直接设置 pendingDeleteId 播放动画。
     */
    fun requestDeleteBookmark(bookmarkId: String) {
        _pendingDeleteId.value = bookmarkId
        startDeleteFallback(bookmarkId)
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

    suspend fun addLinkBookmark(url: String) = withContext(Dispatchers.IO) {
        return@withContext bookmarkDao.createBookmark(url)
    }
}