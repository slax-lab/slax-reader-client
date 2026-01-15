package com.slax.reader.ui.bookmark.states

import com.powersync.ExperimentalPowerSyncAPI
import com.powersync.PowerSyncDatabase
import com.powersync.sync.SyncStreamStatus
import com.powersync.sync.SyncStreamSubscription
import com.powersync.utils.JsonParam
import com.slax.reader.data.database.dao.BookmarkCommentDao
import com.slax.reader.data.database.model.BookmarkCommentPO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.hours

data class CommentState(
    val isLoading: Boolean = false,
    val downloadProgress: Int? = null,
    val isDone: Boolean = false,
    val errStr: String? = null,
    val comments: List<BookmarkCommentPO> = emptyList()
)

class CommentDelegate(
    private val database: PowerSyncDatabase,
    private val commentDao: BookmarkCommentDao,
    private val scope: CoroutineScope
) {
    private var sub: SyncStreamSubscription? = null
    private val _status = MutableStateFlow<SyncStreamStatus?>(null)
    private val _bookmarkId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val comments: StateFlow<List<BookmarkCommentPO>> = _bookmarkId
        .flatMapLatest { id ->
            id?.let { commentDao.watchComments(it) } ?: kotlinx.coroutines.flow.flowOf(emptyList())
        }.stateIn(scope, SharingStarted.Lazily, emptyList())

    val commentState: StateFlow<CommentState> = combine(_status, comments) { status, commentList ->
        when {
            // 未开始
            status == null -> CommentState(
                isLoading = true,
                comments = commentList
            )

            // 同步中
            status.progress != null && !status.subscription.hasSynced -> {
                val fraction = status.progress!!.fraction
                CommentState(
                    isLoading = fraction < 1.0,
                    downloadProgress = (fraction * 100).toInt(),
                    isDone = fraction >= 1.0,
                    comments = commentList
                )
            }

            // 同步完成
            status.subscription.hasSynced -> CommentState(
                isLoading = false,
                isDone = true,
                comments = commentList
            )

            else -> CommentState(
                isLoading = false,
                comments = commentList
            )
        }
    }.stateIn(scope, SharingStarted.Lazily, CommentState())


    @OptIn(ExperimentalPowerSyncAPI::class)
    suspend fun bind(bookmarkId: String) {
        val streamParams = mapOf(
            "bookmark_uuid" to JsonParam.String(bookmarkId)
        )
        sub = database.syncStream("bookmark_comment", streamParams).subscribe(ttl = 1.hours)
        _status.value = database.currentStatus.forStream(sub!!)
        _bookmarkId.value = bookmarkId
    }

    fun reset() {
        scope.launch(Dispatchers.IO) {
            sub?.unsubscribe()
        }
        sub = null
    }

}