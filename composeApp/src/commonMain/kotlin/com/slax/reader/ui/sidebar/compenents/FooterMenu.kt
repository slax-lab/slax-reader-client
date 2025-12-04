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
import androidx.navigation.NavController
import com.slax.reader.const.AboutRoutes
import com.slax.reader.const.SettingsRoutes
import com.slax.reader.const.SubscriptionManagerRoutes
import com.slax.reader.domain.auth.AuthDomain
import com.slax.reader.ui.subscription.SubscriptionManagerScreen
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_xs_sidebar_about
import slax_reader_client.composeapp.generated.resources.ic_xs_sidebar_config

class FooterMenuConfig(
    val title: String,
    val icon:  @Composable (() -> Unit)? = null,
    val color:  NavigationDrawerItemColors,
    val onClick: () -> Unit
)

@Composable
fun FooterMenu(
    navCtrl: NavController,
    onDismiss: () -> Unit,
) {
    val authDomain: AuthDomain = koinInject()

    println("[watch][UI] FooterMenu recomposed")

    mapOf(
        "setting" to FooterMenuConfig(
            title = "设置",
            icon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_xs_sidebar_config),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp)
                )
            },
            color = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = Color.Transparent
            ),
            onClick = {
                onDismiss()
                navCtrl.navigate(SettingsRoutes)
            }
        ),
        "about" to FooterMenuConfig(
            title = "关于",
            icon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_xs_sidebar_about),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp)
                )
            },
            color = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = Color.Transparent
            ),
            onClick = {
                onDismiss()
                navCtrl.navigate(AboutRoutes)
            }
        ),
        "subscription" to FooterMenuConfig(
            title = "订阅管理",
            icon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_xs_sidebar_about),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp)
                )
            },
            color = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = Color.Transparent
            ),
            onClick = {
                onDismiss()
                navCtrl.navigate(SubscriptionManagerRoutes)
            }
        ),
    ).map {
        NavigationDrawerItem(
            label = {
                Text(
                    text = it.value.title,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFF333333)
                )
            },
            icon = it.value.icon ?: {},
            shape = RoundedCornerShape(12.dp),
            selected = false,
            onClick = it.value.onClick,
            colors = it.value.color
        )
    }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp).padding(top = 60.dp).fillMaxWidth()
    ) {
        Button(
            onClick = {
                authDomain.signOut()
            },
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