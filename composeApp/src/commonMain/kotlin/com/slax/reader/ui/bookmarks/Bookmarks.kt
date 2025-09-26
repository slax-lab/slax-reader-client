package com.slax.reader.ui.bookmarks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powersync.ExperimentalPowerSyncAPI
import com.slax.reader.data.database.model.UserBookmark

sealed class SyncStatus {
    object Initializing : SyncStatus()
    object Disconnected : SyncStatus()
    data class Syncing(val progress: Float) : SyncStatus()
    object Synced : SyncStatus()
}

@Composable
@OptIn(ExperimentalPowerSyncAPI::class)
fun UserBookmarksScreen(viewModel: BookmarkViewModel) {
    val bookmarks by viewModel.bookmarks.collectAsState()
    val syncStatus = viewModel.syncStatus
    var showCreateDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            BookmarksHeader()

            Spacer(modifier = Modifier.height(16.dp))

            StatusCard(syncStatus = syncStatus)

            Spacer(modifier = Modifier.height(16.dp))

            BookmarksListHeader(count = bookmarks.size)

            Spacer(modifier = Modifier.height(8.dp))

            BookmarksList(
                bookmarks = bookmarks,
                viewModel = viewModel
            )
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("+", fontSize = 24.sp)
        }
    }

    if (showCreateDialog) {
        CreateBookmarkDialog(
            viewModel = viewModel,
            onDismiss = { showCreateDialog = false }
        )
    }
}

@Composable
private fun BookmarksHeader() {
    Text(
        text = "User Bookmarks",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun StatusCard(syncStatus: SyncStatus) {
    val (backgroundColor, statusText) = when (syncStatus) {
        is SyncStatus.Initializing -> Pair(
            MaterialTheme.colorScheme.surfaceVariant,
            "Initializing..."
        )

        is SyncStatus.Disconnected -> Pair(
            MaterialTheme.colorScheme.errorContainer,
            "Disconnected"
        )

        is SyncStatus.Syncing -> Pair(
            MaterialTheme.colorScheme.primaryContainer,
            "Syncing... ${(syncStatus.progress * 100).toInt()}%"
        )

        is SyncStatus.Synced -> Pair(
            MaterialTheme.colorScheme.primaryContainer,
            "Connected & Synced"
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Status: $statusText",
                fontWeight = FontWeight.Medium
            )

            if (syncStatus is SyncStatus.Syncing) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { syncStatus.progress },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun BookmarksListHeader(count: Int) {
    Text(
        text = "User Bookmarks List: ($count)",
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun BookmarksList(
    bookmarks: List<UserBookmark>,
    viewModel: BookmarkViewModel
) {
    val onToggleStar = remember<(String, Int) -> Unit> {
        { id, state -> viewModel.toggleStar(id, state) }
    }

    val onToggleArchive = remember<(String, Int) -> Unit> {
        { id, state -> viewModel.toggleArchive(id, state) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        items(
            items = bookmarks,
            key = { it.id }
        ) { bookmark ->
            BookmarkCard(
                bookmark = bookmark,
                tagNames = remember(bookmark.metadataObj?.tags) {
                    viewModel.getTagNames(bookmark.metadataObj?.tags ?: emptyList())
                },
                viewModel = viewModel,
                onToggleStar = { onToggleStar(bookmark.id, bookmark.isStarred) },
                onToggleArchive = { onToggleArchive(bookmark.id, bookmark.archiveStatus) }
            )
        }
    }
}

@Composable
private fun BookmarkCard(
    bookmark: UserBookmark,
    tagNames: List<String>,
    viewModel: BookmarkViewModel,
    onToggleStar: () -> Unit,
    onToggleArchive: () -> Unit
) {
    var showTagDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = bookmark.displayTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 2
            )

            if (tagNames.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                TagsRow(tagNames) { showTagDialog = true }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { showTagDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ 添加标签", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            BookmarkStatusRow(bookmark)

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "收藏时间: ${bookmark.createdAt}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            BookmarkActions(
                bookmark = bookmark,
                viewModel = viewModel,
                onToggleStar = onToggleStar,
                onToggleArchive = onToggleArchive
            )
        }
    }

    if (showTagDialog) {
        TagSelectionDialog(
            bookmark = bookmark,
            viewModel = viewModel,
            onDismiss = { showTagDialog = false }
        )
    }
}

@Composable
private fun TagsRow(tagNames: List<String>, onClick: () -> Unit = {}) {
    val tagsText = remember(tagNames) {
        tagNames.joinToString(" • ")
    }

    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = tagsText,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun BookmarkStatusRow(bookmark: UserBookmark) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatusText(
            text = if (bookmark.parsedMetadata()?.share?.is_enable == true) "已分享" else "未分享",
            isPrimary = bookmark.parsedMetadata()?.share?.is_enable == true
        )

        StatusText(
            text = if (bookmark.archiveStatus == 1) "已归档" else "未归档",
            isPrimary = bookmark.archiveStatus == 1
        )

        StatusText(
            text = if (bookmark.isStarred == 1) "★ 星标" else "☆ 未星标",
            isPrimary = bookmark.isStarred == 1
        )
    }
}

@Composable
private fun StatusText(text: String, isPrimary: Boolean) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = if (isPrimary) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    )
}

@Composable
private fun BookmarkActions(
    bookmark: UserBookmark,
    viewModel: BookmarkViewModel,
    onToggleStar: () -> Unit,
    onToggleArchive: () -> Unit
) {
    val isShared = bookmark.parsedMetadata()?.share?.is_enable == true

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(
            onClick = onToggleStar,
            modifier = Modifier.weight(1f)
        ) {
            Text(text = if (bookmark.isStarred == 1) "取消星标" else "添加星标")
        }

        TextButton(
            onClick = onToggleArchive,
            modifier = Modifier.weight(1f)
        ) {
            Text(text = if (bookmark.archiveStatus == 1) "取消归档" else "归档")
        }

        if (isShared) {
            TextButton(
                onClick = { viewModel.disableBookmarkShare(bookmark.id) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("取消分享")
            }
        }

        TextButton(
            onClick = { viewModel.deleteBookmark(bookmark.id) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("删除")
        }
    }
}

@Composable
private fun TagSelectionDialog(
    bookmark: UserBookmark,
    viewModel: BookmarkViewModel,
    onDismiss: () -> Unit
) {
    val allTags by viewModel.tags.collectAsState()
    val currentTagIds = bookmark.parsedMetadata()?.tags ?: emptyList()
    val selectedTagIds = remember {
        mutableStateMapOf<String, Boolean>().apply {
            allTags.keys.forEach { tagId ->
                this[tagId] = tagId in currentTagIds
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑标签") },
        text = {
            LazyColumn(
                modifier = Modifier.height(300.dp)
            ) {
                items(allTags.entries.toList()) { (tagId, tag) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedTagIds[tagId] ?: false,
                            onCheckedChange = { checked ->
                                selectedTagIds[tagId] = checked
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tag.tag_name)
                        Spacer(modifier = Modifier.weight(1f))
                        if (selectedTagIds[tagId] == true) {
                            TextButton(
                                onClick = {
                                    selectedTagIds[tagId] = false
                                }
                            ) {
                                Text("移除", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newTagIds = selectedTagIds.filter { it.value }.keys.toList()
                    viewModel.updateBookmarkTags(bookmark.id, newTagIds)
                    onDismiss()
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun CreateBookmarkDialog(
    viewModel: BookmarkViewModel,
    onDismiss: () -> Unit
) {
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建书签") },
        text = {
            Column {
                Text("输入要收藏的网址:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    placeholder = { Text("https://example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (url.isNotBlank()) {
                        viewModel.createBookmark(url.trim())
                        onDismiss()
                    }
                },
                enabled = url.isNotBlank()
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}