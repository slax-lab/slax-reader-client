package com.slax.reader.ui.bookmark.states

import com.powersync.ExperimentalPowerSyncAPI
import com.powersync.PowerSyncDatabase
import com.powersync.sync.SyncStreamSubscription
import com.powersync.utils.JsonParam
import com.slax.reader.const.AppError
import com.slax.reader.data.database.dao.BookmarkCommentDao
import com.slax.reader.data.database.dao.LocalBookmarkDao
import com.slax.reader.data.database.dao.UserDao
import com.slax.reader.data.database.model.BookmarkCommentPO
import com.slax.reader.data.database.model.CollectionMarkPolicy
import com.slax.reader.data.database.model.canDeleteMark
import com.slax.reader.data.database.model.canCreateMark
import com.slax.reader.data.network.ApiService
import com.slax.reader.data.network.dto.MarkCommentUser
import com.slax.reader.data.network.dto.MarkDetail
import com.slax.reader.data.network.dto.MarkInfo
import com.slax.reader.data.network.dto.MarkPathApprox
import com.slax.reader.data.network.dto.MarkPathItem
import com.slax.reader.data.network.dto.MarkType
import com.slax.reader.data.network.dto.MarkUserInfo
import com.slax.reader.data.network.dto.StrokeCreateSelectContent
import com.slax.reader.utils.BridgeMarkCommentInfo
import com.slax.reader.utils.BridgeMarkReplyInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.days

class CommentDelegate(
    private val database: PowerSyncDatabase,
    private val commentDao: BookmarkCommentDao,
    private val localBookmarkDao: LocalBookmarkDao,
    private val userDao: UserDao,
    private val apiService: ApiService,
    private val scope: CoroutineScope
) {
    companion object {
        private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
        private val lineTypes = setOf(MarkType.LINE.value, MarkType.ORIGIN_LINE.value)
        private val rootCommentTypes = setOf(MarkType.COMMENT.value, MarkType.ORIGIN_COMMENT.value)
        private val commentTypes = rootCommentTypes + MarkType.REPLY.value
    }

    private var sub: SyncStreamSubscription? = null
    private var subStreamName: String? = null
    private var subOwnerId: String? = null
    private var markUsersJob: Job? = null
    private val _bookmarkId = MutableStateFlow<String?>(null)
    private val _marksVisible = MutableStateFlow(true)
    private val _markUsers = MutableStateFlow<Map<String, MarkCommentUser>>(emptyMap())
    private val _markPolicy = MutableStateFlow(CollectionMarkPolicy.Personal)
    private val userInfo = userDao.watchUserInfo()
    private val markPolicy: CollectionMarkPolicy
        get() = _markPolicy.value

    val currentUserId: String? get() = userInfo.value?.id
    val currentUserIdLong: Long get() = currentUserId.toStableId()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val comments: StateFlow<List<BookmarkCommentPO>> = combine(_bookmarkId, _marksVisible) { id, visible ->
        id to visible
    }.flatMapLatest { (id, visible) ->
        if (id != null && visible) commentDao.watchComments(id) else flowOf(emptyList())
    }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private data class Processed(
        val markDetail: MarkDetail = MarkDetail(),
        val commentsBySource: Map<String, List<BookmarkCommentPO>> = emptyMap(),
        val repliesByRootId: Map<String, List<BookmarkCommentPO>> = emptyMap(),
        val raw: List<BookmarkCommentPO> = emptyList(),
        val markUsers: Map<String, MarkCommentUser> = emptyMap(),
        val showProfile: Boolean = true,
    )

    private val processed: StateFlow<Processed> = comments
        .onEach { refreshMarkUsersIfNeeded(it) }
        .combine(_markUsers) { list, markUsers -> list to markUsers }
        .combine(_markPolicy) { (list, markUsers), policy ->
            if (policy.showMarks) buildProcessed(list, markUsers, policy.showProfile)
            else Processed(showProfile = policy.showProfile)
        }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, Processed())

    val markDetailFlow: StateFlow<MarkDetail> = processed
        .map { it.markDetail }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), MarkDetail())

    private val _selectedSourceJson = MutableStateFlow<String?>(null)

    val panelCommentsFlow: StateFlow<List<BridgeMarkCommentInfo>> = combine(
        processed, _selectedSourceJson
    ) { p, sourceJson ->
        if (sourceJson == null) emptyList() else buildPanelComments(p, sourceJson)
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedMark(source: List<MarkPathItem>?) {
        _selectedSourceJson.value = source?.let { json.encodeToString(it) }
    }

    @OptIn(ExperimentalPowerSyncAPI::class)
    suspend fun bind(bookmarkId: String, policy: CollectionMarkPolicy, collectionOwnerId: String? = null) {
        val validCollectionOwner = collectionOwnerId?.takeIf { it.isNotBlank() }
        val effectivePolicy = if (policy.isCollection && validCollectionOwner == null) {
            CollectionMarkPolicy.CollectionPending
        } else {
            policy
        }
        val bookmarkChanged = _bookmarkId.value != bookmarkId
        val streamName = if (effectivePolicy.isCollection) "collection_bookmark_comment" else "bookmark_comment"
        val ownerChanged = subOwnerId != validCollectionOwner.takeIf { effectivePolicy.isCollection }
        val streamChanged = subStreamName != null && subStreamName != streamName
        val currentSub = sub.takeIf {
            !effectivePolicy.showMarks || bookmarkChanged || streamChanged || ownerChanged
        }
        if (currentSub != null) {
            sub = null
            subStreamName = null
            subOwnerId = null
        }

        _markPolicy.value = effectivePolicy
        _bookmarkId.value = bookmarkId
        _marksVisible.value = effectivePolicy.showMarks

        if (!effectivePolicy.showMarks || !effectivePolicy.showProfile) {
            markUsersJob?.cancel()
            markUsersJob = null
            _markUsers.value = emptyMap()
        } else {
            refreshMarkUsersIfNeeded(comments.value)
        }

        currentSub?.unsubscribe()

        if (effectivePolicy.showMarks && sub == null) {
            val streamParams = if (effectivePolicy.isCollection) {
                mapOf(
                    "bookmark_uuid" to JsonParam.String(bookmarkId),
                    "owner_id" to JsonParam.String(validCollectionOwner.orEmpty())
                )
            } else {
                mapOf("bookmark_uuid" to JsonParam.String(bookmarkId))
            }
            sub = database.syncStream(streamName, streamParams).subscribe(ttl = 5.days)
            subStreamName = streamName
            subOwnerId = validCollectionOwner.takeIf { effectivePolicy.isCollection }
        }
    }

    fun reset() {
        val currentSub = sub
        sub = null
        subStreamName = null
        subOwnerId = null
        markUsersJob?.cancel()
        markUsersJob = null
        scope.launch(Dispatchers.IO) { currentSub?.unsubscribe() }
        _markUsers.value = emptyMap()
        _bookmarkId.value = null
        _marksVisible.value = false
        _selectedSourceJson.value = null
        _markPolicy.value = CollectionMarkPolicy.Personal
    }

    suspend fun addMark(
        type: MarkType,
        source: List<MarkPathItem> = emptyList(),
        approxSource: MarkPathApprox? = null,
        selectContent: List<StrokeCreateSelectContent> = emptyList(),
        comment: String = "",
        rootId: String? = null,
        parentId: String? = null,
    ): String {
        val bookmarkId = _bookmarkId.value ?: error("CommentDelegate not bound")
        val userId = currentUserId ?: throw AppError.CommentException.MarkNotAllowed

        if (!markPolicy.canCreateMark(type)) {
            throw AppError.CommentException.MarkNotAllowed
        }

        if (comment.isNotEmpty() && comment.length > 1500) throw AppError.CommentException.TooLong
        if (type == MarkType.LINE && comment.isNotBlank()) throw AppError.CommentException.EmptyComment
        if ((type == MarkType.COMMENT || type == MarkType.REPLY) && comment.isBlank()) throw AppError.CommentException.EmptyComment

        return commentDao.addMark(
            bookmarkId = bookmarkId,
            userId = userId,
            type = type,
            source = source,
            approxSource = approxSource,
            selectContent = selectContent,
            comment = comment,
            rootId = rootId,
            parentId = parentId,
            markPolicy = markPolicy,
        )
    }

    suspend fun deleteComment(commentId: String) {
        if (!canDeleteComment(commentId)) throw AppError.CommentException.MarkNotAllowed
        commentDao.deleteComment(commentId)
    }

    fun canDeleteComment(commentId: String): Boolean {
        val currentUserId = currentUserId
        val bookmarkId = _bookmarkId.value ?: return false
        val comment = processed.value.raw.firstOrNull { item ->
            item.id == commentId && item.userBookmarkUuid == bookmarkId
        } ?: return false

        return markPolicy.canDeleteMark(currentUserId, comment.metadataObj?.user_id)
    }

    fun findCommentId(predicate: (BookmarkCommentPO) -> Boolean): String? =
        processed.value.raw.find(predicate)?.id

    fun findComment(predicate: (BookmarkCommentPO) -> Boolean): BookmarkCommentPO? =
        processed.value.raw.find(predicate)

    private fun refreshMarkUsersIfNeeded(comments: List<BookmarkCommentPO>) {
        if (!markPolicy.showMarks || !markPolicy.showProfile) return
        val bookmarkId = _bookmarkId.value ?: return
        val userIds = comments.mapNotNullTo(mutableSetOf()) { it.metadataObj?.user_id }
        if (userIds.isEmpty()) return
        val unknownIds = userIds - _markUsers.value.keys - setOfNotNull(userInfo.value?.id)
        if (unknownIds.isEmpty()) return
        if (markUsersJob?.isActive == true) return

        markUsersJob = scope.launch(Dispatchers.IO) {
            try {
                val cached = localBookmarkDao.getMarkUsers(bookmarkId)
                if (!cached.isNullOrBlank()) {
                    val users = json.decodeFromString<List<MarkCommentUser>>(cached).associateBy { it.uuid }
                    if ((unknownIds - users.keys).isEmpty()) {
                        _markUsers.value = users
                        return@launch
                    }
                }
                val users = apiService.getMarkUsers(bookmarkId).data ?: return@launch
                localBookmarkDao.updateMarkUsers(bookmarkId, json.encodeToString(users))
                _markUsers.value = users.associateBy { it.uuid }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                println("[CommentDelegate] loadMarkUsers failed: ${error.message}")
            }
        }
    }

    private fun buildProcessed(
        list: List<BookmarkCommentPO>,
        markUsers: Map<String, MarkCommentUser>,
        showProfile: Boolean,
    ): Processed {
        if (list.isEmpty()) return Processed(showProfile = showProfile)

        val visibleCommentIds = buildVisibleCommentIds(list)
        val userMap = mutableMapOf<String, MarkUserInfo>()
        val markList = ArrayList<MarkInfo>(list.size)
        val bySource = HashMap<String, MutableList<BookmarkCommentPO>>()
        val byRootId = HashMap<String, MutableList<BookmarkCommentPO>>()

        for (po in list) {
            val isVisible = when (po.type) {
                in lineTypes -> po.is_deleted == 0
                in commentTypes -> po.id in visibleCommentIds
                else -> po.is_deleted == 0
            }
            if (!isVisible) continue

            val poUserId = po.metadataObj?.user_id ?: ""
            collectUser(poUserId, into = userMap, markUsers = markUsers, showProfile = showProfile)

            markList += MarkInfo(
                id = po.id.toStableId(),
                user_id = poUserId.toStableId(),
                type = MarkType.entries.firstOrNull { it.value == po.type } ?: MarkType.LINE,
                source = po.source.decodeOrDefault(emptyList()),
                approx_source = po.approx_source.decodeOrDefault(null),
                parent_id = po.metadataObj?.parent_id.toStableId(),
                root_id = po.metadataObj?.root_id.toStableId(),
                comment = po.comment,
                created_at = po.created_at,
                is_deleted = po.is_deleted != 0
            )

            when (po.type) {
                in rootCommentTypes ->
                    bySource.getOrPut(po.source) { mutableListOf() }.add(po)
                MarkType.REPLY.value ->
                    po.metadataObj?.root_id?.let { rootId ->
                        byRootId.getOrPut(rootId) { mutableListOf() }.add(po)
                    }
            }
        }

        return Processed(
            markDetail = MarkDetail(mark_list = markList, user_list = userMap),
            commentsBySource = bySource,
            repliesByRootId = byRootId,
            raw = list,
            markUsers = markUsers,
            showProfile = showProfile,
        )
    }

    private fun buildVisibleCommentIds(list: List<BookmarkCommentPO>): Set<String> {
        val comments = list.filter { it.type in commentTypes }
        if (comments.isEmpty()) return emptySet()

        val byParentId = comments
            .filter { it.type == MarkType.REPLY.value }
            .groupBy { it.metadataObj?.parent_id.orEmpty() }
        val memo = HashMap<String, Boolean>()

        fun isVisible(po: BookmarkCommentPO, visiting: Set<String> = emptySet()): Boolean {
            memo[po.id]?.let { return it }
            if (po.id in visiting) return po.is_deleted == 0

            val visible = po.is_deleted == 0 ||
                    byParentId[po.id].orEmpty().any { child ->
                        isVisible(child, visiting + po.id)
                    }
            memo[po.id] = visible
            return visible
        }

        return comments
            .filter { isVisible(it) }
            .mapTo(mutableSetOf()) { it.id }
    }

    private fun buildPanelComments(p: Processed, sourceJson: String): List<BridgeMarkCommentInfo> {
        val rootPOs = p.commentsBySource[sourceJson] ?: return emptyList()

        val rootMap = LinkedHashMap<Long, BridgeMarkCommentInfo>(rootPOs.size)
        val commentMap = LinkedHashMap<Long, BridgeMarkCommentInfo>()
        for (po in rootPOs) {
            val comment = po.toBridgeComment(p.markUsers, p.showProfile)
            rootMap[comment.markId] = comment
            commentMap[comment.markId] = comment
        }

        for (po in rootPOs) {
            p.repliesByRootId[po.id].orEmpty().forEach { replyPO ->
                val reply = replyPO.toBridgeComment(p.markUsers, p.showProfile)
                commentMap[reply.markId] = reply
            }
        }

        for (po in rootPOs) {
            val replies = p.repliesByRootId[po.id] ?: continue
            val rootId = po.id.toStableId()
            val root = rootMap[rootId] ?: continue

            val children = replies.map { replyPO ->
                val reply = commentMap[replyPO.id.toStableId()]
                    ?: replyPO.toBridgeComment(p.markUsers, p.showProfile)
                val parentId = replyPO.metadataObj?.parent_id.toStableId()
                val parent = commentMap[parentId]
                if (parent != null) {
                    reply.copy(reply = BridgeMarkReplyInfo(
                        id = parent.markId, username = parent.username,
                        userId = parent.userId, avatar = parent.avatar,
                    ))
                } else reply
            }

            rootMap[rootId] = root.copy(children = children)
        }

        return rootMap.values
            .filter { !it.isDeleted || it.children.isNotEmpty() }
    }

    private fun BookmarkCommentPO.toBridgeComment(
        markUsers: Map<String, MarkCommentUser>,
        showProfile: Boolean,
    ): BridgeMarkCommentInfo {
        val poUserId = metadataObj?.user_id ?: ""
        val (name, avatar) = resolveUserInfo(poUserId, markUsers, showProfile)
        return BridgeMarkCommentInfo(
            markId = id.toStableId(),
            comment = comment,
            userId = poUserId.toStableId(),
            username = name,
            avatar = avatar,
            isDeleted = is_deleted != 0,
            createdAt = created_at,
            rootId = metadataObj?.root_id.toStableId().takeIf { it != 0L },
        )
    }

    private fun resolveUserInfo(
        userId: String,
        markUsers: Map<String, MarkCommentUser>,
        showProfile: Boolean,
    ): Pair<String, String> {
        if (!showProfile) return "" to ""
        val user = userInfo.value
        if (userId == user?.id) return (user.name) to (user.picture)
        val cached = markUsers[userId]
        return (cached?.nick_name ?: "") to (cached?.avatar ?: "")
    }

    private fun collectUser(
        userId: String,
        into: MutableMap<String, MarkUserInfo>,
        markUsers: Map<String, MarkCommentUser>,
        showProfile: Boolean,
    ) {
        if (userId.isBlank() || userId in into) return
        val numId = userId.toStableId()
        val (name, avatar) = resolveUserInfo(userId, markUsers, showProfile)
        into[numId.toString()] = MarkUserInfo(id = numId, username = name, avatar = avatar)
    }
}

private val parseJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

internal fun String?.toStableId(): Long =
    if (this.isNullOrBlank()) 0L else (toLongOrNull() ?: hashCode().toLong())

private inline fun <reified T> String.decodeOrDefault(default: T): T =
    if (isBlank()) default
    else try { parseJson.decodeFromString(this) } catch (_: Exception) { default }
