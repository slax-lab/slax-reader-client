package com.slax.reader.data.database.dao

import com.powersync.PowerSyncDatabase
import com.powersync.db.getString
import com.powersync.db.getStringOptional
import com.slax.reader.data.database.model.CollectionBookmarkCacheCandidate
import com.slax.reader.data.database.model.CollectionBookmarkItem
import com.slax.reader.data.database.model.SubscribedCollection
import com.slax.reader.data.database.model.mapperToBookmark
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class CollectionDao(
    private val scope: CoroutineScope,
    private val database: PowerSyncDatabase,
) {
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
                COALESCE((
                    SELECT MAX(cb.created_at)
                    FROM sr_collection_bookmark cb
                    WHERE cb.owner_id = c.owner_id
                ), '') AS latest_article_at
            FROM sr_user_collection_subscriber s
            INNER JOIN sr_user_collection c ON c.id = s.collection_id
            ORDER BY s.updated_at DESC
            """.trimIndent()
        ) { cursor ->
            val lastReadAt = cursor.getStringOptional("last_read_at").orEmpty()
            val latestArticleAt = cursor.getStringOptional("latest_article_at").orEmpty()
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
                hasNew = latestArticleAt.isNotBlank() && latestArticleAt > lastReadAt,
            )
        }
            .catch { error -> println("Error watching subscribed collections: ${error.message}") }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
    }

    fun watchSubscribedCollections(): StateFlow<List<SubscribedCollection>> = subscribedCollections

    private val collectionBookmarkCacheCandidates: StateFlow<List<CollectionBookmarkCacheCandidate>> by lazy {
        database.watch(
            """
            SELECT
                cb.id,
                collection.id AS collection_id,
                JSON_EXTRACT(cb.metadata, '$.bookmark.status') AS metadata_status
            FROM sr_user_collection_subscriber AS subscriber
            INNER JOIN sr_user_collection AS collection
                ON collection.id = subscriber.collection_id
            INNER JOIN sr_collection_bookmark AS cb
                ON cb.owner_id = collection.owner_id
            ORDER BY collection.id, COALESCE(NULLIF(cb.starred_at, ''), cb.created_at) DESC
            """.trimIndent()
        ) { cursor ->
            CollectionBookmarkCacheCandidate(
                id = cursor.getString("id"),
                collectionId = cursor.getStringOptional("collection_id").orEmpty(),
                metadataStatus = cursor.getStringOptional("metadata_status").orEmpty(),
            )
        }
            .catch { error -> println("Error watching collection cache candidates: ${error.message}") }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.Eagerly, emptyList())
    }

    fun watchCollectionBookmarkCacheCandidates(): StateFlow<List<CollectionBookmarkCacheCandidate>> =
        collectionBookmarkCacheCandidates

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
    }.catch { error ->
        println("Error watching collection bookmark detail: ${error.message}")
    }.distinctUntilChanged()

    fun watchCollectionBookmarks(ownerId: String) = database.watch(
        """
        SELECT
            cb.id,
            cb.owner_id,
            cb.alias_title,
            cb.created_at,
            JSON_EXTRACT(cb.metadata, '$.bookmark.title') AS title,
            COALESCE(
                JSON_EXTRACT(cb.metadata, '$.bookmark.site_name'),
                JSON_EXTRACT(cb.metadata, '$.bookmark.host_url')
            ) AS source,
            JSON_EXTRACT(cb.metadata, '$.bookmark.content_cover') AS cover,
            COALESCE(stats.comment_count, 0) AS mark_count,
            JSON_EXTRACT(stats.first_comment, '$.content') AS first_mark_content,
            JSON_EXTRACT(stats.first_comment, '$.comment') AS first_mark_comment
        FROM sr_collection_bookmark cb
        LEFT JOIN sr_user_bookmark_stats stats
            ON stats.bookmark_uuid = cb.id
           AND stats.owner_id = cb.owner_id
        WHERE cb.owner_id = ?
        ORDER BY cb.created_at DESC
        """.trimIndent(),
        parameters = listOf(ownerId),
    ) { cursor ->
        CollectionBookmarkItem(
            id = cursor.getString("id"),
            ownerId = cursor.getStringOptional("owner_id").orEmpty(),
            aliasTitle = cursor.getStringOptional("alias_title").orEmpty(),
            title = cursor.getStringOptional("title").orEmpty(),
            source = cursor.getStringOptional("source").orEmpty(),
            cover = cursor.getStringOptional("cover").orEmpty(),
            createdAt = cursor.getStringOptional("created_at").orEmpty(),
            markCount = cursor.getStringOptional("mark_count")?.toIntOrNull() ?: 0,
            firstMarkContent = cursor.getStringOptional("first_mark_content").orEmpty(),
            firstMarkComment = cursor.getStringOptional("first_mark_comment").orEmpty(),
        )
    }.catch { error ->
        println("Error watching collection bookmarks: ${error.message}")
    }.distinctUntilChanged()

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
}
