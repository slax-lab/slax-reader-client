package com.slax.reader.data.database.model

import androidx.compose.runtime.Immutable
import com.slax.reader.data.network.dto.MarkType
import com.slax.reader.utils.parseInstantOrNull
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Immutable
data class CollectionMarkPolicy(
    val isCollection: Boolean = false,
    val showMarks: Boolean = true,
    val allowLine: Boolean = true,
    val allowComment: Boolean = true,
    val showProfile: Boolean = true,
) {
    companion object {
        val Personal = CollectionMarkPolicy()
        val CollectionPending = CollectionMarkPolicy(
            isCollection = true,
            showMarks = false,
            allowLine = false,
            allowComment = false,
            showProfile = false,
        )
    }
}

@Immutable
@Serializable
data class CollectionMarkSettings(
    val allow_highlight: Boolean = false,
    val show_highlight: Boolean = true,
    val allow_access: Boolean = true,
) {
    companion object {
        val Default = CollectionMarkSettings()
    }
}

internal fun collectionMarkPolicy(
    share: ShareSettings?,
    collectionSettings: CollectionMarkSettings? = null,
): CollectionMarkPolicy {
    val settings = collectionSettings ?: CollectionMarkSettings.Default
    val isEnabled = share?.is_enable ?: settings.allow_access
    val showLine = share?.show_line ?: settings.show_highlight
    val allowLine = share?.allow_line ?: settings.allow_highlight
    val showComment = share?.show_comment ?: settings.show_highlight
    val allowComment = share?.allow_comment ?: settings.allow_highlight
    val showUserInfo = share?.show_userinfo ?: true

    return CollectionMarkPolicy(
        isCollection = true,
        showMarks = isEnabled && showLine && showComment,
        allowLine = isEnabled && allowLine,
        allowComment = isEnabled && allowComment,
        showProfile = isEnabled && showUserInfo,
    )
}

internal fun CollectionMarkPolicy.canCreateMark(type: MarkType): Boolean {
    if (!isCollection) return true
    return when (type) {
        MarkType.LINE -> allowLine
        MarkType.COMMENT, MarkType.REPLY -> allowComment
        else -> false
    }
}

internal fun CollectionMarkPolicy.canDeleteMark(
    currentUserId: String?,
    markOwnerId: String?,
): Boolean {
    val userId = currentUserId?.takeIf { it.isNotBlank() } ?: return false
    return !isCollection || markOwnerId == userId
}

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

val SubscribedCollection.isClosed: Boolean
    get() = status != 1

@OptIn(ExperimentalTime::class)
fun SubscribedCollection.isExpired(now: Instant = Clock.System.now()): Boolean {
    val endTime = parseInstantOrNull(subscriptionEndTime) ?: return true
    return endTime <= now
}

@Immutable
data class CollectionBookmarkItem(
    override val id: String,
    val ownerId: String,
    val aliasTitle: String,
    val title: String,
    val createdAt: String,
    override val downloadStatus: Int = 0,
    val isAutoCached: Boolean = false,
) : ListRowBookmark {
    override fun displayTitle(): String = aliasTitle.ifBlank { title }.ifBlank { id.take(8) }
}

@Immutable
data class CollectionBookmarkCacheCandidate(
    val id: String,
    val ownerId: String,
    val metadataStatus: String,
)
