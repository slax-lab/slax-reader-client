package com.slax.reader.ui.sidebar

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import com.slax.reader.ui.AppViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import slax_reader_client.composeapp.generated.resources.*

@Composable
fun Sidebar(
    drawerState: DrawerState,
    onSettingsClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
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
                drawerContainerColor = Color.White
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
    val appViewModel: AppViewModel = koinInject()
    val userInfo by appViewModel.userInfo.collectAsState()
    val syncStatus by appViewModel.syncStatusData.collectAsState()

    val avatarPainter = rememberAsyncImagePainter(
        model = userInfo?.picture,
        error = painterResource(Res.drawable.global_default_avatar),
        placeholder = painterResource(Res.drawable.global_default_avatar)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(12.dp)
    ) {
        // 关闭按钮
        Box(
            modifier = Modifier
                .padding(start = 12.dp)
                .size(24.dp)
                .clip(CircleShape)
                .clickable(onClick = onDismiss),
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

        if (appViewModel.syncType != null) {
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = appViewModel.syncType!!,
                    fontSize = 11.sp,
                    color = Color(0xFF0F1419),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = {
                        when {
                            syncStatus?.downloading == true -> appViewModel.downloadProgress
                            syncStatus?.uploading == true -> 0f
                            else -> 0f
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF1976D2),
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${
                        (when {
                            syncStatus?.downloading == true -> appViewModel.downloadProgress
                            syncStatus?.uploading == true -> 0f
                            else -> 0f
                        } * 100).toInt()
                    }%",
                    fontSize = 10.sp,
                    color = Color(0xFF0F1419),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }


        Spacer(modifier = Modifier.weight(1f))

        // 底部菜单项
        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 设置菜单项
            NavigationDrawerItem(
                label = {
                    Text(
                        text = "设置",
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        color = Color(0xFF333333)
                    )
                },
                icon = {
                    Icon(
                        painter = painterResource(Res.drawable.ic_xs_sidebar_config),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
                },
                selected = false,
                onClick = onSettingsClick,
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = Color.Transparent
                )
            )

            // 关于菜单项
            NavigationDrawerItem(
                label = {
                    Text(
                        text = "关于",
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        color = Color(0xFF333333)
                    )
                },
                icon = {
                    Icon(
                        painter = painterResource(Res.drawable.ic_xs_sidebar_about),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
                },
                selected = false,
                onClick = onAboutClick,
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = Color.Transparent
                )
            )
        }
    }
}