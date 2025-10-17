package com.slax.reader.ui.inbox

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.rememberAsyncImagePainter
import com.slax.reader.const.BookmarkRoutes
import com.slax.reader.data.database.model.InboxListBookmarkItem
import com.slax.reader.domain.auth.AuthDomain
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import slax_reader_client.composeapp.generated.resources.*

@Composable
fun InboxListScreen(navCtrl: NavController) {
    var isSidebarVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F3))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            NavigationBar(onAvatarClick = { isSidebarVisible = true })
            Spacer(modifier = Modifier.height(8.dp))
            ContentSection(navCtrl)
        }
    }

    Sidebar(
        isVisible = isSidebarVisible,
        onDismiss = { isSidebarVisible = false },
        onSettingsClick = {
        },
        onAboutClick = {
        }
    )
}

@Composable
private fun UserAvatar() {
    val viewModel = koinInject<InboxListViewModel>()
    val authDomain: AuthDomain = koinInject()

    val userInfo by viewModel.userInfo.collectAsState()
    val syncStatus by viewModel.syncStatusData.collectAsState()

    val avatarPainter = rememberAsyncImagePainter(
        model = userInfo?.picture,
        error = painterResource(Res.drawable.global_default_avatar),
        placeholder = painterResource(Res.drawable.global_default_avatar)
    )

    // release 后可以删掉这个日志了
    LaunchedEffect(syncStatus) {
        println("==== Sync Status ====")
        println("==== connecting: ${syncStatus?.connecting}")
        println("==== connected: ${syncStatus?.connected}")
        println("==== uploading: ${syncStatus?.uploading}")
        println("==== downloading: ${syncStatus?.downloading}")
        println("==== anyError: ${syncStatus?.anyError}")
        println("==== Sync Status ====")
    }

    Box(
        modifier = Modifier.size(32.dp).clickable(onClick = {
            authDomain.signOut()
        }),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = avatarPainter,
            contentDescription = "Avatar",
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        when {
            viewModel.hasError -> {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(36.dp),
                    color = Color(0xFFE53935),
                    strokeWidth = 2.dp,
                    trackColor = Color(0x33E53935)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFFE53935)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✕",
                            color = Color.White,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 正在上传 - 实线进度条和上箭头
            viewModel.isUploading -> {
                CircularProgressIndicator(
                    progress = { viewModel.downloadProgress },
                    modifier = Modifier.size(36.dp),
                    color = Color(0xFF1DA1F2),
                    strokeWidth = 2.dp,
                    trackColor = Color(0x33333333)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFF1DA1F2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "↑",
                            color = Color.White,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 正在下载 - 实线进度条和下箭头
            viewModel.isDownloading -> {
                val progress = viewModel.downloadProgress
                val hasProgress = syncStatus?.downloadProgress?.let { it.totalOperations > 0 } ?: false

                if (hasProgress) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(36.dp),
                        color = Color(0xFF1DA1F2),
                        strokeWidth = 2.dp,
                        trackColor = Color(0x33333333)
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = Color(0xFF1DA1F2),
                        strokeWidth = 2.dp,
                        trackColor = Color(0x33333333)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFF1DA1F2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "↓",
                            color = Color.White,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            viewModel.connected -> {}

            // 正在连接 - 虚线进度环
            viewModel.isConnecting && !viewModel.connected -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = Color(0xFF1DA1F2),
                    strokeWidth = 2.dp,
                    trackColor = Color(0x33333333)
                )
            }
        }
    }
}

@Composable
private fun NavigationBar(onAvatarClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .width(70.dp)
                .align(Alignment.CenterStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(Res.drawable.inbox_tab),
                contentDescription = "Menu",
                modifier = Modifier.size(20.dp, 20.dp).clickable(onClick = onAvatarClick),
                contentScale = ContentScale.Fit
            )
            UserAvatar()
        }

        Text(
            text = "Slax Reader",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF0F1419),
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun ContentSection(navCtrl: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = Color(0xFFFCFCFC),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(17.dp))
        InboxTitleRow()
        Spacer(modifier = Modifier.height(16.dp))
        ArticleList(navCtrl)
    }
}

@Composable
private fun InboxTitleRow() {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Inbox",
                fontSize = 16.sp,
                color = Color(0xFF999999)
            )

            Image(
                painter = painterResource(Res.drawable.inbox_more),
                contentDescription = "Content Type",
                modifier = Modifier
                    .size(12.dp)
                    .height(8.dp),
                contentScale = ContentScale.Fit
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Inbox") },
                onClick = {
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun ArticleList(navCtrl: NavController) {
    val viewModel: InboxListViewModel = koinInject()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val iconResource = painterResource(Res.drawable.inbox_internet)
    val moreResource = painterResource(Res.drawable.inbox_list_more)
    val iconPainter = remember { iconResource }
    val morePainter = remember { moreResource }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            },
        verticalArrangement = Arrangement.spacedBy(0.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        itemsIndexed(
            items = bookmarks,
            key = { _, bookmark -> bookmark.id },
            contentType = { _, _ -> "bookmark" }
        ) { index, bookmark ->
            BookmarkItemRow(navCtrl, bookmark, iconPainter, morePainter)

            if (index < bookmarks.lastIndex) {
                DividerLine()
            }
        }
    }
}

@Composable
private fun BookmarkItemRow(
    navCtrl: NavController,
    bookmark: InboxListBookmarkItem,
    iconPainter: Painter,
    morePainter: Painter,
) {
    val haptics = LocalHapticFeedback.current
    val viewModel: InboxListViewModel = koinInject()

    val bookmarkStatusMap by viewModel.bookmarkStatusFlow.collectAsState()
    val downloadStatus = bookmarkStatusMap[bookmark.id]

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .combinedClickable(
                onClick = {
                    navCtrl.navigate(BookmarkRoutes(bookmarkId = bookmark.id))
                },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
//                    contextMenuPhotoId = photo.id
                },
//                onLongClickLabel = stringResource(R.string.open_context_menu)
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(24.dp)) {
                Image(
                    painter = iconPainter,
                    contentDescription = "Article",
                    modifier = Modifier.size(24.dp),
                    contentScale = ContentScale.Fit
                )

                if (downloadStatus?.status == com.slax.reader.domain.sync.DownloadStatus.DOWNLOADING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF1DA1F2),
                        strokeWidth = 2.dp,
                        trackColor = Color.Transparent
                    )
                }
            }

            Text(
                text = bookmark.displayTitle(),
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Image(
            painter = morePainter,
            contentDescription = "More",
            modifier = Modifier.size(16.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun DividerLine() {
    Spacer(modifier = Modifier.height(8.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(Color(0x14333333))
    )
    Spacer(modifier = Modifier.height(8.dp))
}