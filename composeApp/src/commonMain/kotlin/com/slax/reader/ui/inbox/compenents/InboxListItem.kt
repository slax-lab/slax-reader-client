package com.slax.reader.ui.inbox.compenents

import androidx.compose.runtime.Immutable
import com.slax.reader.data.database.model.InboxListBookmarkItem

@Immutable
sealed class InboxListItem {
    data class BookmarkItem(val bookmark: InboxListBookmarkItem) : InboxListItem()
    data class GroupSeparator(val label: String) : InboxListItem()
}
