package com.slax.reader.ui.sidebar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.DefaultStrokeLineCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
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
            .padding(8.dp)
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

        appViewModel.syncType?.let {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFE8EBED),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val style = TextStyle(
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = Color(0xFF333333)
                        )

                        Text(
                            text = it,
                            style = style
                        )

                        if (!appViewModel.hasError) {
                            Text(
                                text = "${
                                    (when {
                                        syncStatus?.downloading == true -> appViewModel.downloadProgress
                                        syncStatus?.uploading == true -> 0f
                                        else -> 0f
                                    } * 100).toInt()
                                }%",
                                style = style
                            )
                        }
                    }

                    if (!appViewModel.hasError) {
                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            strokeCap = DefaultStrokeLineCap,
                            progress = {
                                when {
                                    syncStatus?.downloading == true -> appViewModel.downloadProgress
                                    syncStatus?.uploading == true -> 0f
                                    else -> 0f
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(2.5.dp)),
                            color = Color(0xFF16B998),
                            trackColor = Color(0x140F1419)
                        )
                    }
                }
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
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = Color.Transparent
                )
            )


            Column(
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 60.dp).fillMaxWidth()
            ) {
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().background(Color.White),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            "退出登录",
                            style = TextStyle(
                                fontSize = 16.sp,
                                lineHeight = 22.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF333333)
                            )
                        )
                    }
                }
            }

        }
    }
}