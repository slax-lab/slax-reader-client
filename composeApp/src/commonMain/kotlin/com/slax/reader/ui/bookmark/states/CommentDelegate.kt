package com.slax.reader.ui.bookmark.states

import com.powersync.ExperimentalPowerSyncAPI
import com.powersync.PowerSyncDatabase
import com.powersync.sync.SyncStreamStatus
import com.powersync.sync.SyncStreamSubscription
import com.powersync.utils.JsonParam
import com.slax.reader.const.AppError
import com.slax.reader.data.database.dao.BookmarkCommentDao
import com.slax.reader.data.database.dao.LocalBookmarkDao
import com.slax.reader.data.database.dao.UserDao
import com.slax.reader.data.database.model.BookmarkCommentPO
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
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
    }

    private var sub: SyncStreamSubscription? = null
    private val _status = MutableStateFlow<SyncStreamStatus?>(null)
    private val _bookmarkId = MutableStateFlow<String?>(null)
    private val _markUsers = MutableStateFlow<Map<String, MarkCommentUser>>(emptyMap())
    private val userInfo = userDao.watchUserInfo()

    val currentUserId: String? get() = userInfo.value?.id
    val currentUserIdLong: Long get() = currentUserId.toStableId()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val comments: StateFlow<List<BookmarkCommentPO>> = _bookmarkId
        .flatMapLatest { id -> id?.let { commentDao.watchComments(it) } ?: flowOf(emptyList()) }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private data class Processed(
        val markDetail: MarkDetail = MarkDetail(),
        val commentsBySource: Map<String, List<BookmarkCommentPO>> = emptyMap(),
        val repliesByRootId: Map<String, List<BookmarkCommentPO>> = emptyMap(),
        val raw: List<BookmarkCommentPO> = emptyList(),
    )

    private val processed: StateFlow<Processed> = comments
        .onEach { refreshMarkUsersIfNeeded(it) }
        .combine(_markUsers) { list, _ -> buildProcessed(list) }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, Processed())

    val markDetailFlow: StateFlow<MarkDetail> = processed
        .map { it.markDetail }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), MarkDetail())

    private val _selectedSourceJson = MutableStateFlow<String?>(null)

    val panelCommentsFlow: StateFlow<List<BridgeMarkCommentInfo>> = combine(
        processed, _selectedSourceJson, _markUsers
    ) { p, sourceJson, _ ->
        if (sourceJson == null) emptyList() else buildPanelComments(p, sourceJson)
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedMark(source: List<MarkPathItem>?) {
        _selectedSourceJson.value = source?.let { json.encodeToString(it) }
    }

    @OptIn(ExperimentalPowerSyncAPI::class)
    suspend fun bind(bookmarkId: String) {
        val streamParams = mapOf(
            "bookmark_uuid" to JsonParam.String(bookmarkId)
        )
        sub = database.syncStream("bookmark_comment", streamParams).subscribe(ttl = 5.days)
        _status.value = database.currentStatus.forStream(sub!!)
        _bookmarkId.value = bookmarkId
    }

    fun reset() {
        scope.launch(Dispatchers.IO) { sub?.unsubscribe() }
        sub = null
        _markUsers.value = emptyMap()
        _bookmarkId.value = null
        _selectedSourceJson.value = null
        _status.value = null
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

        if (comment.isNotEmpty() && comment.length > 1500) throw AppError.CommentException.TooLong
        if (type == MarkType.LINE && comment.isNotBlank()) throw AppError.CommentException.EmptyComment
        if ((type == MarkType.COMMENT || type == MarkType.REPLY) && comment.isBlank()) throw AppError.CommentException.EmptyComment

        return commentDao.addMark(
            bookmarkId = bookmarkId,
            userId = userInfo.value?.id ?: "",
            type = type,
            source = source,
            approxSource = approxSource,
            selectContent = selectContent,
            comment = comment,
            rootId = rootId,
            parentId = parentId,
        )
    }

    suspend fun deleteComment(commentId: String) = commentDao.deleteComment(commentId)

    fun findCommentId(predicate: (BookmarkCommentPO) -> Boolean): String? =
        processed.value.raw.find(predicate)?.id

    fun findComment(predicate: (BookmarkCommentPO) -> Boolean): BookmarkCommentPO? =
        processed.value.raw.find(predicate)

    private fun refreshMarkUsersIfNeeded(comments: List<BookmarkCommentPO>) {
        val bookmarkId = _bookmarkId.value ?: return
        val userIds = comments.mapNotNullTo(mutableSetOf()) { it.metadataObj?.user_id }
        if (userIds.isEmpty()) return
        val unknownIds = userIds - _markUsers.value.keys - setOfNotNull(userInfo.value?.id)
        if (unknownIds.isEmpty()) return

        scope.launch(Dispatchers.IO) {
            runCatching {
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
            }.onFailure { println("[CommentDelegate] loadMarkUsers failed: ${it.message}") }
        }
    }

    private fun buildProcessed(list: List<BookmarkCommentPO>): Processed {
        if (list.isEmpty()) return Processed()

        val userMap = mutableMapOf<String, MarkUserInfo>()
        val markList = ArrayList<MarkInfo>(list.size)
        val bySource = HashMap<String, MutableList<BookmarkCommentPO>>()
        val byRootId = HashMap<String, MutableList<BookmarkCommentPO>>()

        for (po in list) {
            val poUserId = po.metadataObj?.user_id ?: ""
            collectUser(poUserId, userMap)

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
                MarkType.COMMENT.value ->
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
        )
    }

    private fun buildPanelComments(p: Processed, sourceJson: String): List<BridgeMarkCommentInfo> {
        val rootPOs = p.commentsBySource[sourceJson] ?: return emptyList()

        // 先把所有 root COMMENT 转为 BridgeMarkCommentInfo
        val rootMap = LinkedHashMap<Long, BridgeMarkCommentInfo>(rootPOs.size)
        for (po in rootPOs) {
            rootMap[po.id.toStableId()] = po.toBridgeComment()
        }

        // 找到属于这些 root 的 REPLY
        for (po in rootPOs) {
            val replies = p.repliesByRootId[po.id] ?: continue
            val rootId = po.id.toStableId()
            val root = rootMap[rootId] ?: continue

            val children = replies.map { replyPO ->
                val reply = replyPO.toBridgeComment()
                // 被回复人
                val parentId = replyPO.metadataObj?.parent_id.toStableId()
                val parent = rootMap[parentId]
                if (parent != null) {
                    reply.copy(reply = BridgeMarkReplyInfo(
                        id = parent.markId, username = parent.username,
                        userId = parent.userId, avatar = parent.avatar,
                    ))
                } else reply
            }

            rootMap[rootId] = root.copy(children = children)
        }

        // 过滤：已删除且无子回复的不显示；子回复中已删除的直接移除
        return rootMap.values
            .map { it.copy(children = it.children.filter { c -> !c.isDeleted }) }
            .filter { !it.isDeleted || it.children.isNotEmpty() }
    }

    private fun BookmarkCommentPO.toBridgeComment(): BridgeMarkCommentInfo {
        val poUserId = metadataObj?.user_id ?: ""
        val (name, avatar) = resolveUserInfo(poUserId)
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

    private fun resolveUserInfo(userId: String): Pair<String, String> {
        val user = userInfo.value
        if (userId == user?.id) return (user.name) to (user.picture)
        val cached = _markUsers.value[userId]
        return (cached?.nick_name ?: "") to (cached?.avatar ?: "")
    }

    private fun collectUser(userId: String, into: MutableMap<String, MarkUserInfo>) {
        if (userId.isBlank() || userId in into) return
        val numId = userId.toStableId()
        val (name, avatar) = resolveUserInfo(userId)
        into[numId.toString()] = MarkUserInfo(id = numId, username = name, avatar = avatar)
    }
}

private val parseJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

internal fun String?.toStableId(): Long =
    if (this.isNullOrBlank()) 0L else (toLongOrNull() ?: hashCode().toLong())

private inline fun <reified T> String.decodeOrDefault(default: T): T =
    if (isBlank()) default
    else try { parseJson.decodeFromString(this) } catch (_: Exception) { default }
