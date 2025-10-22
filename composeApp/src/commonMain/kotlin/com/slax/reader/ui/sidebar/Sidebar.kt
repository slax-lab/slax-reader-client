package com.slax.reader.ui.sidebar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.rememberAsyncImagePainter
import com.slax.reader.const.AboutRoutes
import com.slax.reader.const.SettingsRoutes
import com.slax.reader.const.SpaceManagerRoutes
import com.slax.reader.ui.AppViewModel
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.default_splash
import slax_reader_client.composeapp.generated.resources.global_default_avatar

@Composable
fun SideBar(
    navController: NavController,
    onLogout: () -> Unit = {}
) {
    val appViewModel: AppViewModel = koinInject()
    val userInfo by appViewModel.userInfo.collectAsState()
    val syncStatus by appViewModel.syncStatusData.collectAsState()

    val avatarPainter = rememberAsyncImagePainter(
        model = userInfo?.picture,
        error = painterResource(Res.drawable.global_default_avatar),
        placeholder = painterResource(Res.drawable.global_default_avatar)
    )

    Surface(
        modifier = Modifier
            .width(200.dp)
            .fillMaxHeight(),
        shadowElevation = 16.dp,
        color = Color(0xFFF5F5F3)
    ) {
        NavigationRail(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color(0xFFF5F5F3),
            header = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = avatarPainter,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = userInfo?.name ?: "",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        ) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color(0xFFE0E0E0)
            )

            if (appViewModel.syncType != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE3F2FD))
                        .padding(12.dp)
                ) {
                    Text(
                        text = appViewModel.syncType!!,
                        fontSize = 11.sp,
                        color = Color(0xFF1976D2),
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
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
                        color = Color(0xFF1976D2),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color(0xFFE0E0E0)
                )
            }

            NavigationRailItem(
                icon = {
                    Image(
                        painter = painterResource(Res.drawable.default_splash),
                        contentDescription = "Article",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                    )
                },
                label = { Text("设置页面", fontSize = 12.sp) },
                selected = false,
                onClick = { navController.navigate(SettingsRoutes) }
            )

            NavigationRailItem(
                icon = {
                    Image(
                        painter = painterResource(Res.drawable.default_splash),
                        contentDescription = "Article",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                    )
                },
                label = { Text("空间管理", fontSize = 12.sp) },
                selected = false,
                onClick = { navController.navigate(SpaceManagerRoutes) }
            )

            NavigationRailItem(
                icon = {
                    Image(
                        painter = painterResource(Res.drawable.default_splash),
                        contentDescription = "Article",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                    )
                },
                label = { Text("关于", fontSize = 12.sp) },
                selected = false,
                onClick = { navController.navigate(AboutRoutes) }
            )

            Spacer(modifier = Modifier.weight(1f))

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color(0xFFE0E0E0)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onLogout)
                    .background(Color(0xFFFFEBEE))
                    .border(1.dp, Color(0xFFEF5350), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.default_splash),
                    contentDescription = "Article",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "退出登录",
                    fontSize = 13.sp,
                    color = Color(0xFFD32F2F),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}