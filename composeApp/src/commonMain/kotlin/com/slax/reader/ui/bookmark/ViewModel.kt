package com.slax.reader.ui.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.slax.reader.SlaxConfig
import com.powersync.PowerSyncDatabase
import com.slax.reader.data.database.dao.BookmarkCommentDao
import com.slax.reader.data.database.dao.BookmarkDao
import com.slax.reader.data.database.dao.LocalBookmarkDao
import com.slax.reader.data.database.dao.SubscriptionDao
import com.slax.reader.data.database.dao.UserDao
import com.slax.reader.data.network.ApiService
import com.slax.reader.data.preferences.AppPreferences
import com.slax.reader.data.preferences.ContinueReadingBookmark
import com.slax.reader.domain.sync.BackgroundDomain
import com.slax.reader.ui.bookmark.states.BookmarkDelegate
import com.slax.reader.ui.bookmark.states.BookmarkOverlay
import com.slax.reader.ui.bookmark.states.CommentDelegate
import com.slax.reader.ui.bookmark.states.OutlineDelegate
import com.slax.reader.ui.bookmark.states.OverlayDelegate
import com.slax.reader.ui.bookmark.states.OverviewDelegate
import com.slax.reader.utils.parseInstant
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class FeedbackPageParams(
    val title: String? = null,
    val href: String? = null,
    val email: String? = null,
    val bookmarkId: String? = null,
    val entryPoint: String? = null,
    val targetUrl: String? = null,
    val version: String? = null
)

fun FeedbackPageParams.toMap() : Map<String, String> {
    return buildMap {
        title?.let { put("title", it) }
        href?.let { put("href", it) }
        email?.let { put("email", it) }
        bookmarkId?.let { put("bookmarkId", it) }
        entryPoint?.let { put("entryPoint", it) }
        targetUrl?.let { put("targetUrl", it) }
        version?.let { put("version", it)}
        bookmarkId?.let { put("bookmarkId", it) }
    }
}

sealed interface BookmarkDetailEffect {
    data object NavigateBack : BookmarkDetailEffect
    data object NavigateToSubscription : BookmarkDetailEffect
    data class NavigateToFeedback(val params: FeedbackPageParams) : BookmarkDetailEffect

    data class ScrollToAnchor(val anchor: String) : BookmarkDetailEffect
}

data class BookmarkContentState(
    val htmlContent: String? = null,
    val isLoading: Boolean = false,
)

class BookmarkDetailViewModel(
    private val bookmarkDao: BookmarkDao,
    private val subscriptionDao: SubscriptionDao,
    private val localBookmarkDao: LocalBookmarkDao,
    private val commentDao: BookmarkCommentDao,
    private val userDao: UserDao,
    private val backgroundDomain: BackgroundDomain,
    private val apiService: ApiService,
    private val appPreferences: AppPreferences,
    private val database: PowerSyncDatabase,
) : ViewModel() {

    private val _bookmarkId = MutableStateFlow<String?>(null)
    val bookmarkId = _bookmarkId.asStateFlow()

    private val _effects = MutableSharedFlow<BookmarkDetailEffect>(extraBufferCapacity = 8)
    val effects: SharedFlow<BookmarkDetailEffect> = _effects.asSharedFlow()

    private val _contentState = MutableStateFlow(BookmarkContentState(isLoading = false))
    val contentState = _contentState.asStateFlow()

    private var contentJob: Job? = null

    val userInfo = userDao.watchUserInfo()

    val overlayDelegate = OverlayDelegate()
    val commentDelegate = CommentDelegate(database, commentDao, viewModelScope)
    val outlineDelegate = OutlineDelegate(localBookmarkDao, apiService, viewModelScope)
    val overviewDelegate = OverviewDelegate(localBookmarkDao, apiService, viewModelScope)
    val bookmarkDelegate = BookmarkDelegate(bookmarkDao, _bookmarkId, viewModelScope)

    fun bind(bookmarkId: String) {
        if (_bookmarkId.value == bookmarkId) return

        _bookmarkId.value = bookmarkId
        _contentState.value = BookmarkContentState(isLoading = true)

        overlayDelegate.reset()
        outlineDelegate.reset()
        overviewDelegate.reset()
        commentDelegate.reset()

        refreshContent()
    }

    fun refreshContent() {
        val id = _bookmarkId.value ?: return

        contentJob?.cancel()
        _contentState.value = _contentState.value.copy(isLoading = true)

        contentJob = viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { backgroundDomain.getBookmarkContent(id) }
            }.onSuccess { content ->
                _contentState.value = BookmarkContentState(htmlContent = content, isLoading = false)
            }
        }
    }

    fun requestScrollToAnchor(anchor: String) {
        viewModelScope.launch {
            _effects.emit(BookmarkDetailEffect.ScrollToAnchor(anchor))
        }
        outlineDelegate.collapseDialog()
    }

    fun requestNavigateBack() {
        viewModelScope.launch { _effects.emit(BookmarkDetailEffect.NavigateBack) }
    }

    fun requestNavigateToSubscription() {
        viewModelScope.launch { _effects.emit(BookmarkDetailEffect.NavigateToSubscription) }
    }

    fun requestNavigateToFeedback() {
        viewModelScope.launch {
            val bookmarkState = bookmarkDelegate.bookmarkDetailState.value
            val currentBookmarkId = _bookmarkId.value

            val params = FeedbackPageParams(
                title = bookmarkState.displayTitle,
                href = bookmarkState.metadataUrl,
                email = userInfo.value?.email,
                bookmarkId = currentBookmarkId,
                entryPoint = "bookmark_detail",
                version = "${SlaxConfig.APP_VERSION_NAME} (${SlaxConfig.APP_VERSION_CODE})"
            )

            _effects.emit(BookmarkDetailEffect.NavigateToFeedback(params))
        }
    }

    fun onToolbarIconClick(pageId: String) {
        val current = bookmarkDelegate.bookmarkDetailState.value

        when (pageId) {
            "star" -> bookmarkDelegate.onToggleStar(!current.isStarred)
            "archive" -> bookmarkDelegate.onToggleArchive(!current.isArchived)
            "edit_title" -> overlayDelegate.showOverlay(BookmarkOverlay.EditTitle)
            "summary" -> {
                viewModelScope.launch {
                    val isSubscribed = checkUserIsSubscribed()
                    if (!isSubscribed) {
                        overlayDelegate.showOverlay(BookmarkOverlay.SubscriptionRequired)
                        overlayDelegate.dismissOverlay(BookmarkOverlay.Toolbar)
                        return@launch
                    }
                    outlineDelegate.showDialog()
                }
            }

            "feedback" -> overlayDelegate.showOverlay(BookmarkOverlay.FeedbackRequired)
        }

        overlayDelegate.dismissOverlay(BookmarkOverlay.Toolbar)
    }

    fun onStopRecordContinue(scrollY: Int) {
        viewModelScope.launch { runCatching { recordContinueBookmark(scrollY) } }
    }

    fun onResumeClearContinue() {
        viewModelScope.launch { runCatching { clearContinueBookmark() } }
    }

    private suspend fun recordContinueBookmark(scrollY: Int) = withContext(Dispatchers.IO) {
        _bookmarkId.value?.let { id ->
            val state = bookmarkDelegate.bookmarkDetailState.value
            if (state.displayTitle.isEmpty()) return@withContext
            appPreferences.setContinueReadingBookmark(
                ContinueReadingBookmark(
                    bookmarkId = id,
                    title = state.displayTitle,
                    scrollY = scrollY
                )
            )
        }
    }

    private suspend fun clearContinueBookmark() = withContext(Dispatchers.IO) {
        return@withContext appPreferences.clearContinueReadingBookmark()
    }

    fun loadOverview() {
        val id = _bookmarkId.value ?: return
        overviewDelegate.loadOverview(id)
    }

    fun loadOutline() {
        val id = _bookmarkId.value ?: return
        outlineDelegate.loadOutline(id)
    }

    override fun onCleared() {
        super.onCleared()
        contentJob?.cancel()
        contentJob = null
        commentDelegate.reset()
        outlineDelegate.reset()
        overviewDelegate.reset()
        overlayDelegate.reset()
    }

    @OptIn(kotlin.time.ExperimentalTime::class)
    suspend fun checkUserIsSubscribed(): Boolean = withContext(Dispatchers.IO) {
        val info = subscriptionDao.getSubscriptionInfo() ?: return@withContext false

        try {
            val endTime = parseInstant(info.subscription_end_time)
            val now = kotlin.time.Clock.System.now()
            endTime > now
        } catch (e: Exception) {
            println("Error checking subscription: ${e.message}")
            false
        }
    }
}
