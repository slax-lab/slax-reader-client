package com.slax.reader

import com.slax.reader.data.database.dao.BookmarkRepository
import com.slax.reader.data.database.dao.LocalBookmarkRepository
import com.slax.reader.data.database.dao.SubscriptionRepository
import com.slax.reader.data.database.dao.UserRepository
import com.slax.reader.data.database.model.BookmarkSortType
import com.slax.reader.data.database.model.InboxListBookmarkItem
import com.slax.reader.data.database.model.LocalBookmarkInfo
import com.slax.reader.data.database.model.UserBookmark
import com.slax.reader.data.database.model.UserInfo
import com.slax.reader.data.database.model.UserSubscriptionInfo
import com.slax.reader.data.database.model.UserTag
import com.slax.reader.data.network.AccountApi
import com.slax.reader.data.network.BookmarkAiApi
import com.slax.reader.data.network.MetricsType
import com.slax.reader.data.network.dto.DeleteAccountData
import com.slax.reader.data.network.dto.OverviewResponse
import com.slax.reader.data.network.dto.OutlineResponse
import com.slax.reader.data.network.dto.FeedbackParams
import com.slax.reader.data.preferences.SettingsPreferences
import com.slax.reader.data.preferences.AuthTokenPreferences
import com.slax.reader.utils.IAPCallback
import com.slax.reader.utils.IAPProductOffer
import com.slax.reader.utils.IapGateway
import com.slax.reader.domain.coordinator.AppSyncState
import com.slax.reader.domain.coordinator.NetworkCoordinator
import com.slax.reader.data.network.FeedbackApi
import com.slax.reader.data.network.dto.HttpData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.Json

class FakeUserRepository : UserRepository {
    val user = MutableStateFlow<UserInfo?>(null)
    override fun watchUserInfo(): StateFlow<UserInfo?> = user.asStateFlow()
}

class FakeSubscriptionRepository : SubscriptionRepository {
    val subscription = MutableStateFlow<UserSubscriptionInfo?>(null)
    override fun watchSubscriptionInfo(): StateFlow<UserSubscriptionInfo?> = subscription.asStateFlow()
}

class FakeLocalBookmarkRepository : LocalBookmarkRepository {
    val local = MutableStateFlow<Map<String, LocalBookmarkInfo>>(emptyMap())
    val outline = mutableMapOf<String, String?>()
    val outlineScroll = mutableMapOf<String, Int?>()
    val overview = mutableMapOf<String, Pair<String?, List<String>?>>()
    override fun watchUserLocalBookmarkMap() = local.asStateFlow()
    override suspend fun updateLocalBookmarkOutlineScrollPosition(bookmarkId: String, scrollPosition: Int): Long {
        outlineScroll[bookmarkId] = scrollPosition
        return 1L
    }
    override suspend fun getLocalBookmarkOutlineScrollPosition(bookmarkId: String) = outlineScroll[bookmarkId]
    override suspend fun getLocalBookmarkOutline(bookmarkId: String) = outline[bookmarkId]
    override suspend fun updateLocalBookmarkOutline(bookmarkId: String, outline: String): Long {
        this.outline[bookmarkId] = outline
        return 1L
    }
    override suspend fun getLocalBookmarkOverview(bookmarkId: String) = overview[bookmarkId] ?: (null to null)
    override suspend fun updateLocalBookmarkOverview(bookmarkId: String, overview: String, keyTakeaways: String?): Long {
        this.overview[bookmarkId] = overview to keyTakeaways?.let { Json.decodeFromString<List<String>>(it) }
        return 1L
    }
}

class FakeOfflineFileStore {
    private val files = mutableMapOf<String, ByteArray>()
    var readCount = 0
        private set
    var writeCount = 0
        private set

    fun read(path: String): ByteArray? {
        readCount++
        return files[path]
    }

    fun write(path: String, data: ByteArray) {
        writeCount++
        files[path] = data
    }

    fun clear(path: String) {
        files.remove(path)
    }
}

class FakeBookmarkRepository : BookmarkRepository {
    override val hasSynced = MutableStateFlow(false).asStateFlow()
    val bookmarks = MutableStateFlow<List<InboxListBookmarkItem>?>(emptyList())
    val calls = mutableListOf<String>()
    override fun watchUserBookmarkPaged(sortType: BookmarkSortType) = bookmarks.asStateFlow()
    override fun watchBookmarkDetail(bookmarkId: String): Flow<List<UserBookmark>> = MutableStateFlow(emptyList())
    override fun watchUserTag(): Flow<List<UserTag>> = MutableStateFlow(emptyList())
    override suspend fun getTagsByIds(tagIds: List<String>) = emptyList<UserTag>()
    override suspend fun createTag(tagName: String): UserTag {
        calls += "createTag:$tagName"
        return UserTag(tagName, tagName, tagName, "")
    }
    override suspend fun updateMetadataField(bookmarkId: String, fieldPath: String, jsonValue: String) {
        calls += "metadata:$bookmarkId:$fieldPath:$jsonValue"
    }
    override suspend fun deleteBookmark(bookmarkId: String) {
        calls += "delete:$bookmarkId"
    }
    override suspend fun updateBookmarkArchive(bookmarkId: String, state: Int) {
        calls += "archive:$bookmarkId:$state"
    }
    override suspend fun updateBookmarkStar(bookmarkId: String, state: Int) {
        calls += "star:$bookmarkId:$state"
    }
    override suspend fun updateBookmarkAliasTitle(bookmarkId: String, title: String) {
        calls += "title:$bookmarkId:$title"
    }
    override suspend fun createBookmark(url: String) {
        calls += "bookmark:$url"
    }
}

class FakeNetworkCoordinator : NetworkCoordinator {
    override val syncState = MutableStateFlow<AppSyncState>(AppSyncState.Connected).asStateFlow()
    var networkAvailable = true
    override suspend fun isNetworkAvailable() = networkAvailable
}

class FakeSettingsPreferences : SettingsPreferences, AuthTokenPreferences {
    val cacheCount = MutableStateFlow(50)
    val downloadImages = MutableStateFlow(true)
    override fun getCacheCount() = cacheCount.asStateFlow()
    override suspend fun setCacheCount(count: Int) {
        cacheCount.value = count
    }
    override fun getDownloadImages() = downloadImages.asStateFlow()
    override suspend fun setDownloadImages(enabled: Boolean) {
        downloadImages.value = enabled
    }
    var token: String? = "token"
    override suspend fun getAuthInfoSuspend() = token
}

class FakeAccountApi(
    var response: HttpData<DeleteAccountData> = HttpData(DeleteAccountData(true), "", 200)
) : AccountApi {
    var calls = 0
    var error: Throwable? = null
    override suspend fun deleteAccount(): HttpData<DeleteAccountData> {
        calls++
        error?.let { throw it }
        return response
    }
}

class FakeBookmarkAiApi(
    var outlineResponses: List<OutlineResponse> = emptyList(),
    var overviewResponses: List<OverviewResponse> = emptyList()
) : BookmarkAiApi {
    val metrics = mutableListOf<MetricsType>()
    val outlineRequests = mutableListOf<String>()
    val overviewRequests = mutableListOf<String>()
    override fun getBookmarkOutline(bookmarkId: String): Flow<OutlineResponse> {
        outlineRequests += bookmarkId
        return flowOf(*outlineResponses.toTypedArray())
    }
    override fun getBookmarkOverview(bookmarkId: String): Flow<OverviewResponse> {
        overviewRequests += bookmarkId
        return flowOf(*overviewResponses.toTypedArray())
    }
    override suspend fun sendMetrics(type: MetricsType) {
        metrics += type
    }
}

class FakeFeedbackApi : FeedbackApi {
    var calls = 0
    var lastParam: FeedbackParams? = null
    var error: Throwable? = null

    override suspend fun sendFeedback(param: FeedbackParams): HttpData<String> {
        calls++
        lastParam = param
        error?.let { throw it }
        return HttpData("", "", 200)
    }
}

class FakeIapGateway : IapGateway {
    var registeredCallback: IAPCallback? = null
    val purchases = mutableListOf<String>()
    val offerPurchases = mutableListOf<String>()
    override fun setCallback(callback: IAPCallback?) { registeredCallback = callback }
    override fun loadProducts(productIds: List<String>) {
        registeredCallback?.onProductsLoaded(emptyList())
    }
    override fun purchase(productId: String, orderId: String) {
        purchases += "$productId:$orderId"
    }
    override fun purchaseWithOffer(productId: String, orderId: String, offer: IAPProductOffer) {
        offerPurchases += "$productId:$orderId:${offer.offerId}"
    }
}
