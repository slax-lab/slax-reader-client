package com.slax.reader.ui.sidebar

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import com.slax.reader.domain.coordinator.AppSyncState
import com.slax.reader.ui.sidebar.compenents.FooterMenu
import com.slax.reader.ui.sidebar.compenents.SyncStatusBar
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.global_default_avatar
import slax_reader_client.composeapp.generated.resources.ic_xs_sidebar_close

@Composable
fun Sidebar(
    drawerState: DrawerState,
    onSettingsClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onSpaceManagerClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 320.dp),
                drawerContainerColor = Color(0xFFF5F5F3)
            ) {
                DrawerContent(
                    onDismiss = {
                        scope.launch {
                            drawerState.close()
                        }
                    },
                    onSettingsClick = {
                        scope.launch {
                            drawerState.close()
                        }
                        onSettingsClick()
                    },
                    onAboutClick = {
                        scope.launch {
                            drawerState.close()
                        }
                        onAboutClick()
                    },
                    onLogout = onLogout
                )
            }
        },
        content = content
    )
}

@Composable
private fun DrawerContent(
    onDismiss: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onLogout: () -> Unit
) {
    val viewModel = koinInject<SidebarViewModel>()

    val userInfo by viewModel.userInfo.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    val avatarPainter = rememberAsyncImagePainter(
        model = userInfo?.picture,
        error = painterResource(Res.drawable.global_default_avatar),
        placeholder = painterResource(Res.drawable.global_default_avatar)
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(8.dp)
    ) {
        // 关闭按钮
        Box(
            modifier = Modifier
                .padding(start = 12.dp)
                .size(24.dp)
                .clip(CircleShape)
                .alpha(if (isPressed) 0.5f else 1f)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    onDismiss()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_xs_sidebar_close),
                contentDescription = "关闭",
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 用户信息
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = avatarPainter,
                contentDescription = "用户头像",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Text(
                text = userInfo?.name ?: "",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 20.sp,
                color = Color(0xFF0F1419)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))


        if (syncStatus != AppSyncState.Connected) {
            SyncStatusBar(syncStatus)
        }

        Spacer(modifier = Modifier.weight(1f))

        // 底部菜单项
        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            FooterMenu(
                onSettingsClick = onSettingsClick,
                onAboutClick = onAboutClick,
                onLogout = onLogout
            )
        }
    }
}