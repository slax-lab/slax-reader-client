package com.slax.reader.data.database.dao

import com.slax.reader.utils.AppLog

import com.powersync.PowerSyncDatabase
import com.powersync.db.getString
import com.powersync.db.getStringOptional
import com.slax.reader.data.database.model.CollectionBookmarkCacheCandidate
import com.slax.reader.data.database.model.CollectionBookmarkItem
import com.slax.reader.data.database.model.CollectionMarkSettings
import com.slax.reader.data.database.model.SubscribedCollection
import com.slax.reader.data.database.model.mapperToBookmark
import com.slax.reader.utils.parseInstantOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

class CollectionDao(
    private val scope: CoroutineScope,
    private val database: PowerSyncDatabase,
) {
    private fun <T> Flow<T>.retryThenLog(label: String): Flow<T> = this
        .retryWhen { cause, attempt ->
            if (cause is CancellationException || attempt >= MAX_WATCH_RETRIES) {
                false
            } else {
                AppLog.d("Error watching $label (attempt ${attempt + 1}), retrying: ${cause.message}")
                delay((WATCH_RETRY_DELAY_MS * (attempt + 1)).milliseconds)
                true
            }
        }
        .catch { error -> AppLog.d("Error watching $label, giving up: ${error.message}") }

    private val subscribedCollections: StateFlow<List<SubscribedCollection>> by lazy {
        database.watch(
            """
            SELECT
                c.id,
                c.collection_code,
                c.display_name,
                c.avatar,
                c.description,
                c.owner_id,
                c.type,
                c.status,
                s.subscription_end_time,
                s.created_at AS subscribed_at,
                s.is_cancelled,
                s.last_read_at,
                COALESCE(latest.latest_article_at, '') AS latest_article_at
            FROM sr_user_collection_subscriber s
            INNER JOIN sr_user_collection c ON c.id = s.collection_id
            LEFT JOIN (
                SELECT
                    cb.owner_id,
                    MAX(COALESCE(NULLIF(cb.starred_at, ''), cb.created_at)) AS latest_article_at
                FROM sr_collection_bookmark cb
                GROUP BY cb.owner_id
            ) AS latest ON latest.owner_id = c.owner_id
            ORDER BY s.created_at ASC, c.id ASC
            """.trimIndent()
        ) { cursor ->
            val lastReadAt = cursor.getStringOptional("last_read_at").orEmpty()
            val latestArticleAt = cursor.getStringOptional("latest_article_at").orEmpty()
            val latestArticleInstant = parseInstantOrNull(latestArticleAt)
            val lastReadInstant = parseInstantOrNull(lastReadAt)
            SubscribedCollection(
                id = cursor.getString("id"),
                code = cursor.getStringOptional("collection_code").orEmpty(),
                name = cursor.getStringOptional("display_name").orEmpty(),
                avatar = cursor.getStringOptional("avatar").orEmpty(),
                description = cursor.getStringOptional("description").orEmpty(),
                ownerId = cursor.getStringOptional("owner_id").orEmpty(),
                type = cursor.getStringOptional("type")?.toIntOrNull() ?: 1,
                status = cursor.getStringOptional("status")?.toIntOrNull() ?: 0,
                subscriptionEndTime = cursor.getStringOptional("subscription_end_time").orEmpty(),
                subscribedAt = cursor.getStringOptional("subscribed_at").orEmpty(),
                isCancelled = cursor.getStringOptional("is_cancelled") == "1",
                lastReadAt = lastReadAt,
                latestArticleAt = latestArticleAt,
                hasNew = latestArticleInstant != null &&
                    (lastReadInstant == null || latestArticleInstant > lastReadInstant),
            )
        }
            .retryThenLog("subscribed collections")
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
    }

    fun watchSubscribedCollections(): StateFlow<List<SubscribedCollection>> = subscribedCollections

    fun watchCollectionMarkSettings(ownerId: String): Flow<CollectionMarkSettings?> = database.watch(
        """
        SELECT allow_highlight, show_highlight, allow_access
        FROM sr_sharer_setting
        WHERE id = ?
        LIMIT 1
        """.trimIndent(),
        parameters = listOf(ownerId),
    ) { cursor ->
        CollectionMarkSettings(
            allow_highlight = cursor.getStringOptional("allow_highlight")?.toIntOrNull() == 1,
            show_highlight = cursor.getStringOptional("show_highlight")?.toIntOrNull() == 1,
            allow_access = cursor.getStringOptional("allow_access")?.toIntOrNull() == 1,
        )
    }
        .retryThenLog("collection mark policy")
        .map { rows -> rows.firstOrNull() }
        .distinctUntilChanged()

    fun watchCollectionBookmarkCacheCandidates() = database.watch(
        """
        SELECT DISTINCT
            cb.id,
            collection.owner_id,
            JSON_EXTRACT(cb.metadata, '$.bookmark.status') AS metadata_status
        FROM sr_user_collection_subscriber AS subscriber
        INNER JOIN sr_user_collection AS collection
            ON collection.id = subscriber.collection_id
        INNER JOIN sr_collection_bookmark AS cb
            ON cb.owner_id = collection.owner_id
        ORDER BY collection.owner_id, COALESCE(NULLIF(cb.starred_at, ''), cb.created_at) DESC
        """.trimIndent()
    ) { cursor ->
        CollectionBookmarkCacheCandidate(
            id = cursor.getString("id"),
            ownerId = cursor.getStringOptional("owner_id").orEmpty(),
            metadataStatus = cursor.getStringOptional("metadata_status").orEmpty(),
        )
    }
        .retryThenLog("collection cache candidates")
        .distinctUntilChanged()

    fun watchCollectionBookmarkDetail(bookmarkId: String, ownerId: String) = database.watch(
        """
        SELECT
            cb.id,
            0 AS is_read,
            0 AS archive_status,
            1 AS is_starred,
            cb.created_at,
            cb.updated_at,
            cb.alias_title,
            0 AS type,
            NULL AS deleted_at,
            cb.metadata,
            JSON_EXTRACT(cb.metadata, '$.bookmark.title') AS metadata_title,
            JSON_EXTRACT(cb.metadata, '$.bookmark.target_url') AS metadata_url
        FROM sr_collection_bookmark AS cb
        WHERE cb.id = ?
          AND cb.owner_id = ?
        """.trimIndent(),
        parameters = listOf(bookmarkId, ownerId),
    ) { cursor ->
        mapperToBookmark(cursor)
    }
        .retryThenLog("collection bookmark detail")
        .distinctUntilChanged()

    fun watchCollectionBookmarks(ownerId: String) = database.watch(
        """
        SELECT
            cb.id,
            cb.owner_id,
            cb.alias_title,
            cb.created_at,
            JSON_EXTRACT(cb.metadata, '$.bookmark.title') AS title
        FROM sr_collection_bookmark cb
        WHERE cb.owner_id = ?
        ORDER BY COALESCE(NULLIF(cb.starred_at, ''), cb.created_at) DESC
        """.trimIndent(),
        parameters = listOf(ownerId),
    ) { cursor ->
        CollectionBookmarkItem(
            id = cursor.getString("id"),
            ownerId = cursor.getStringOptional("owner_id").orEmpty(),
            aliasTitle = cursor.getStringOptional("alias_title").orEmpty(),
            title = cursor.getStringOptional("title").orEmpty(),
            createdAt = cursor.getStringOptional("created_at").orEmpty(),
        )
    }
        .retryThenLog("collection bookmarks")
        .distinctUntilChanged()

    @OptIn(ExperimentalTime::class)
    suspend fun setLastRead(collectionId: String) {
        database.writeTransaction { transaction ->
            transaction.execute(
                """
                UPDATE sr_user_collection_subscriber
                SET last_read_at = ?
                WHERE collection_id = ?
                """.trimIndent(),
                listOf(Clock.System.now().toString(), collectionId),
            )
        }
    }

    private companion object {
        const val MAX_WATCH_RETRIES = 3L
        const val WATCH_RETRY_DELAY_MS = 1_000L
    }
}
