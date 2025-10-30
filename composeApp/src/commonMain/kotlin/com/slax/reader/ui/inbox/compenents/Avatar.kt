package com.slax.reader.ui.inbox.compenents

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import com.slax.reader.ui.AppViewModel
import com.slax.reader.utils.NetworkState
import com.slax.reader.utils.getNetWorkState
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_inbox_plane
import slax_reader_client.composeapp.generated.resources.global_default_avatar

@Composable
fun UserAvatar() {
    val viewModel = koinInject<AppViewModel>()

    val userInfo by viewModel.userInfo.collectAsState()
    val syncStatus by viewModel.syncStatusData.collectAsState()

    val avatarPainter = rememberAsyncImagePainter(
        model = userInfo?.picture,
        error = painterResource(Res.drawable.global_default_avatar),
        placeholder = painterResource(Res.drawable.global_default_avatar)
    )

    LaunchedEffect(syncStatus) {
        println("network : ${getNetWorkState()}, error: ${syncStatus?.anyError}")
    }

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

        when {
            viewModel.hasError -> {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(24.dp),
                    color = Color(0xFFE53935),
                    strokeWidth = 1.dp,
                    trackColor = Color(0x33E53935)
                )
                var showText = "x"
                var showColor = Color.Red
                var showPlane = false
                val state = getNetWorkState()
                if (state == NetworkState.NONE || state == NetworkState.ACCESS_DENIED) {
                    showPlane = true
                    showColor = Color.Gray
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .size(8.dp)
                        .background(showColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (showPlane) {
                        Image(
                            painter = painterResource(Res.drawable.ic_inbox_plane),
                            contentDescription = "Plane",
                            modifier = Modifier.size(8.dp)
                        )
                    } else {
                        Text(
                            text = showText,
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
                    modifier = Modifier.size(24.dp),
                    color = Color(0xFF1DA1F2),
                    strokeWidth = 1.dp,
                    trackColor = Color(0x33333333)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF5F5F3))
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
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF1DA1F2),
                        strokeWidth = 1.dp,
                        trackColor = Color(0x33333333)
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF1DA1F2),
                        strokeWidth = 1.dp,
                        trackColor = Color(0x33333333)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF5F5F3))
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
                    modifier = Modifier.size(24.dp),
                    color = Color(0xFF1DA1F2),
                    strokeWidth = 1.dp,
                    trackColor = Color(0x33333333)
                )
            }
        }
    }
}
