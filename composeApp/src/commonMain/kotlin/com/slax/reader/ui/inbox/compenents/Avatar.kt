package com.slax.reader.ui.inbox.compenents

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import com.slax.reader.domain.coordinator.AppSyncState
import com.slax.reader.ui.inbox.InboxListViewModel
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.global_default_avatar
import slax_reader_client.composeapp.generated.resources.ic_inbox_plane

@Composable
fun UserAvatar() {
    println("[watch][UI] recomposition UserAvatar")

    val viewModel = koinInject<InboxListViewModel>()

    val userInfo by viewModel.userInfo.collectAsState()
    val syncStatus by viewModel.syncState.collectAsState()

    val avatarPainter = rememberAsyncImagePainter(
        model = userInfo?.picture,
        error = painterResource(Res.drawable.global_default_avatar),
        placeholder = painterResource(Res.drawable.global_default_avatar)
    )

    LaunchedEffect(syncStatus) {}

    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = avatarPainter,
            contentDescription = "Avatar",
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        when (syncStatus) {
            is AppSyncState.Error -> {
                AvatarError(false)
            }

            // 正在上传 - 实线进度条和上箭头
            is AppSyncState.Uploading -> {
                AvatarUploading()
            }

            // 正在下载 - 实线进度条和下箭头
            is AppSyncState.Downloading -> {
                AvatarDownloading(syncStatus)
            }

            is AppSyncState.Connected -> {}

            is AppSyncState.Connecting -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color(0xFF1DA1F2),
                    strokeWidth = 1.dp,
                    trackColor = Color(0x33333333)
                )
            }

            is AppSyncState.NoNetwork -> {
                AvatarError(true)
            }
        }
    }
}

@Composable
fun BoxScope.AvatarDownloading(syncStatus: AppSyncState) {
    val progress = (syncStatus as AppSyncState.Downloading).progress

    AvatarSyncIndicator(
        progress = progress,
        color = Color(0xFF1DA1F2),
        icon = "↓"
    )
}

@Composable
fun BoxScope.AvatarUploading() {
    AvatarSyncIndicator(
        progress = null,
        color = Color(0xFF1DA1F2),
        icon = "↑"
    )
}

@Composable
fun BoxScope.AvatarError(isNoneNetworkError: Boolean) {
    AvatarSyncIndicator(
        progress = 1f,
        color = if (isNoneNetworkError) Color(0xFF9E9E9E) else Color(0xFFE53935),
        trackColor = Color(0x33E53935),
        badgeColor = if (isNoneNetworkError) Color.Gray else Color.Red,
        icon = if (isNoneNetworkError) null else "x",
        iconImage = if (isNoneNetworkError) Res.drawable.ic_inbox_plane else null
    )
}

@Composable
private fun BoxScope.AvatarSyncIndicator(
    progress: Float?,
    color: Color,
    trackColor: Color = Color(0x33333333),
    badgeColor: Color = Color(0xFFF5F5F3),
    icon: String? = null,
    iconImage: DrawableResource? = null
) {
    if (progress != null) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(24.dp),
            color = color,
            strokeWidth = 1.dp,
            trackColor = trackColor
        )
    } else {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = color,
            strokeWidth = 1.dp,
            trackColor = trackColor
        )
    }

    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .size(8.dp)
            .clip(CircleShape)
            .background(badgeColor),
        contentAlignment = Alignment.Center
    ) {
        when {
            iconImage != null -> {
                Image(
                    painter = painterResource(iconImage),
                    contentDescription = null,
                    modifier = Modifier.size(8.dp)
                )
            }

            icon != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0xFF1DA1F2)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = icon,
                        color = Color.White,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}