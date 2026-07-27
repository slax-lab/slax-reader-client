package com.slax.reader.data.database.model

import androidx.compose.runtime.Immutable

@Immutable
data class SubscribedCollection(
    val id: String,
    val code: String,
    val name: String,
    val avatar: String,
    val description: String,
    val ownerId: String,
    val type: Int,
    val status: Int,
    val subscriptionEndTime: String,
    val subscribedAt: String,
    val isCancelled: Boolean,
    val lastReadAt: String,
    val latestArticleAt: String,
    val hasNew: Boolean,
)

@Immutable
data class CollectionBookmarkItem(
    val id: String,
    val ownerId: String,
    val aliasTitle: String,
    val title: String,
    val source: String,
    val cover: String,
    val createdAt: String,
    val markCount: Int,
    val firstMarkContent: String,
    val firstMarkComment: String,
) {
    fun displayTitle(): String = aliasTitle.ifBlank { title }.ifBlank { id.take(8) }
}

@Immutable
data class CollectionBookmarkCacheCandidate(
    val id: String,
    val collectionId: String,
    val metadataStatus: String,
)
