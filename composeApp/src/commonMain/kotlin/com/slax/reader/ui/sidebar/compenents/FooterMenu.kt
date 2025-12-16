package com.slax.reader.ui.sidebar.compenents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_xs_sidebar_about
import slax_reader_client.composeapp.generated.resources.ic_xs_sidebar_config
import slax_reader_client.composeapp.generated.resources.ic_xs_sidebar_logout
import slax_reader_client.composeapp.generated.resources.ic_xs_sidebar_subscribe
import slax_reader_client.composeapp.generated.resources.ic_xs_sidebar_subscribed
import slax_reader_client.composeapp.generated.resources.ic_xs_sidebar_yellow_arrow

@Composable
fun FooterMenu(
    isSubscribed: Boolean = false,
    onSubscribeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onLogout: () -> Unit
) {
    println("[watch][UI] FooterMenu recomposed")

    if (isSubscribed) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Button(
                onClick = onSubscribeClick,
                modifier = Modifier.height(36.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF272220),
                                    Color(0xFF4b4441)
                                )
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_xs_sidebar_subscribed),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.padding(start = 12.dp).size(16.dp, 14.5.dp)
                    )

                    Text(
                        "会员订阅",
                        modifier = Modifier.padding(start = 10.dp),
                        style = TextStyle(
                            fontSize = 15.sp,
                            lineHeight = 21.sp,
                            color = Color(0xFFFFDCC1)
                        )
                    )

                    Icon(
                        painter = painterResource(Res.drawable.ic_xs_sidebar_yellow_arrow),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.padding(horizontal = 8.dp).size(20.dp)
                    )
                }
            }
        }
    } else {
        // 会员管理菜单项
        NavigationDrawerItem(
            label = {
                Text(
                    text = "会员管理",
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFF333333)
                )
            },
            icon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_xs_sidebar_subscribe),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp)
                )
            },
            shape = RoundedCornerShape(12.dp),
            selected = false,
            onClick = onSubscribeClick,
            colors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = Color.Transparent
            )
        )
    }

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
        shape = RoundedCornerShape(12.dp),
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
        shape = RoundedCornerShape(12.dp),
        selected = false,
        onClick = onAboutClick,
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent
        )
    )


    // 退出登录菜单项
    NavigationDrawerItem(
        label = {
            Text(
                text = "退出登录",
                fontSize = 16.sp,
                lineHeight = 20.sp,
                color = Color(0xFF333333)
            )
        },
        icon = {
            Icon(
                painter = painterResource(Res.drawable.ic_xs_sidebar_logout),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp)
            )
        },
        shape = RoundedCornerShape(12.dp),
        selected = false,
        onClick = onLogout,
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent
        )
    )

//    Column(
//        modifier = Modifier.padding(horizontal = 16.dp).padding(top = 60.dp).fillMaxWidth()
//    ) {
//        Button(
//            onClick = onLogout,
//            modifier = Modifier.fillMaxWidth().height(50.dp),
//            shape = RoundedCornerShape(12.dp),
//            contentPadding = PaddingValues(),
//            colors = ButtonDefaults.outlinedButtonColors(
//                containerColor = Color.White
//            )
//        ) {
//            Row(
//                modifier = Modifier.fillMaxSize().background(Color.White),
//                horizontalArrangement = Arrangement.Center,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//
//                Text(
//                    "退出登录",
//                    style = TextStyle(
//                        fontSize = 16.sp,
//                        lineHeight = 22.5.sp,
//                        fontWeight = FontWeight.Medium,
//                        color = Color(0xFF333333)
//                    )
//                )
//            }
//        }
//    }

}