package com.slax.reader.ui.sidebar.compenents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_xs_sidebar_about
import slax_reader_client.composeapp.generated.resources.ic_xs_sidebar_config

@Composable
fun FooterMenu(
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onLogout: () -> Unit
) {
    println("[watch][UI] FooterMenu recomposed")
    // 设置菜单项
//    NavigationDrawerItem(
//        label = {
//            Text(
//                text = "设置",
//                fontSize = 16.sp,
//                lineHeight = 20.sp,
//                color = Color(0xFF333333)
//            )
//        },
//        icon = {
//            Icon(
//                painter = painterResource(Res.drawable.ic_xs_sidebar_config),
//                contentDescription = null,
//                tint = Color.Unspecified,
//                modifier = Modifier.size(20.dp)
//            )
//        },
//        shape = RoundedCornerShape(12.dp),
//        selected = false,
//        onClick = onSettingsClick,
//        colors = NavigationDrawerItemDefaults.colors(
//            unselectedContainerColor = Color.Transparent
//        )
//    )

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