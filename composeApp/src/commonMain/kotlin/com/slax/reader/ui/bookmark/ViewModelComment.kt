package com.slax.reader.ui.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.powersync.ExperimentalPowerSyncAPI
import com.powersync.PowerSyncDatabase
import com.powersync.sync.SyncStreamStatus
import com.powersync.sync.SyncStreamSubscription
import com.powersync.utils.JsonParam
import com.slax.reader.data.database.dao.BookmarkCommentDao
import com.slax.reader.data.database.model.BookmarkCommentPO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.hours

class CommentViewModel(
    private val database: PowerSyncDatabase,
    private val commentDao: BookmarkCommentDao
) : ViewModel() {
    private var sub: SyncStreamSubscription? = null
    private val _status = MutableStateFlow<SyncStreamStatus?>(null)

    val status: StateFlow<SyncStreamStatus?> = _status.asStateFlow()

    var _bookmarkId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    var comments: StateFlow<List<BookmarkCommentPO>> = _bookmarkId
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { id ->
            commentDao.watchComments(id)
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalPowerSyncAPI::class)
    suspend fun setBookmarkId(bookmarkId: String) {
        val streamParams = mapOf(
            "bookmark_uuid" to JsonParam.String(bookmarkId)
        )
        sub = database.syncStream("bookmark_comment", streamParams).subscribe(ttl = 1.hours)
        _status.value = database.currentStatus.forStream(sub!!)
        _bookmarkId.value = bookmarkId
    }

    override fun onCleared() {
        if (sub != null) {
           viewModelScope.launch {
               sub!!.unsubscribe()
           }
        }
        super.onCleared()
    }
}