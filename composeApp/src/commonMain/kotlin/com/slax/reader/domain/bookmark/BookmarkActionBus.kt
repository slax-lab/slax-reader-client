package com.slax.reader.domain.bookmark

/**
 * 跨页面书签操作事件总线（单例）。
 * 详情页发出删除事件后，由列表页在可见时消费并播放动画。
 */
class BookmarkActionBus {
    // 待消费的删除 ID，保证即使列表页 composable 不在组合树中也不会丢失事件
    var pendingDeleteId: String? = null
        private set

    fun emitDelete(bookmarkId: String) {
        pendingDeleteId = bookmarkId
    }

    fun consumePendingDelete(): String? {
        val id = pendingDeleteId
        pendingDeleteId = null
        return id
    }
}
