package com.slax.reader.data.database.dao

import com.slax.reader.data.database.model.InboxListBookmarkItem
import com.slax.reader.data.database.model.LocalBookmarkInfo
import com.slax.reader.data.database.model.UserBookmark
import com.slax.reader.data.database.model.UserInfo
import com.slax.reader.data.database.model.UserSubscriptionInfo
import com.slax.reader.data.database.model.UserTag
import com.slax.reader.data.database.model.BookmarkSortType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {
    fun watchUserInfo(): StateFlow<UserInfo?>
}

interface SubscriptionRepository {
    fun watchSubscriptionInfo(): StateFlow<UserSubscriptionInfo?>
}

interface LocalBookmarkRepository {
    fun watchUserLocalBookmarkMap(): StateFlow<Map<String, LocalBookmarkInfo>>
    suspend fun updateLocalBookmarkOutlineScrollPosition(bookmarkId: String, scrollPosition: Int): Long
    suspend fun getLocalBookmarkOutlineScrollPosition(bookmarkId: String): Int?
    suspend fun getLocalBookmarkOutline(bookmarkId: String): String?
    suspend fun updateLocalBookmarkOutline(bookmarkId: String, outline: String): Long
    suspend fun getLocalBookmarkOverview(bookmarkId: String): Pair<String?, List<String>?>
    suspend fun updateLocalBookmarkOverview(bookmarkId: String, overview: String, keyTakeaways: String?): Long
}

interface BookmarkRepository {
    val hasSynced: StateFlow<Boolean>
    fun watchUserBookmarkPaged(sortType: BookmarkSortType): StateFlow<List<InboxListBookmarkItem>?>
    fun watchBookmarkDetail(bookmarkId: String): Flow<List<UserBookmark>>
    fun watchUserTag(): Flow<List<UserTag>>
    suspend fun getTagsByIds(tagIds: List<String>): List<UserTag>
    suspend fun createTag(tagName: String): UserTag
    suspend fun updateMetadataField(bookmarkId: String, fieldPath: String, jsonValue: String)
    suspend fun deleteBookmark(bookmarkId: String)
    suspend fun updateBookmarkArchive(bookmarkId: String, state: Int)
    suspend fun updateBookmarkStar(bookmarkId: String, state: Int)
    suspend fun updateBookmarkAliasTitle(bookmarkId: String, title: String)
    suspend fun createBookmark(url: String)
}
