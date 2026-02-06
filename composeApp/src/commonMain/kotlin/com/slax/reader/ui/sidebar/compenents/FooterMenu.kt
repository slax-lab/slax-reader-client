package com.slax.reader.ui.sidebar.compenents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.slax.reader.SlaxConfig
import com.slax.reader.const.AboutRoutes
import com.slax.reader.const.RNRoute
import com.slax.reader.const.SettingsRoutes
import com.slax.reader.const.SubscriptionManagerRoutes
import com.slax.reader.data.database.model.checkIsSubscribed
import com.slax.reader.domain.auth.AuthDomain
import com.slax.reader.ui.sidebar.SidebarViewModel
import com.slax.reader.reactnative.navigateToRN
import com.slax.reader.utils.feedbackEvent
import com.slax.reader.utils.i18n
import com.slax.reader.utils.isAndroid
import com.slax.reader.utils.subscriptionEvent
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_xs_sidebar_about
import slax_reader_client.composeapp.generated.resources.ic_xs_sidebar_config
import slax_reader_client.composeapp.generated.resources.ic_xs_sidebar_feedback
import slax_reader_client.composeapp.generated.resources.ic_xs_sidebar_logout
import slax_reader_client.composeapp.generated.resources.ic_xs_sidebar_subscribe
import slax_reader_client.composeapp.generated.resources.ic_xs_sidebar_subscribed
import slax_reader_client.composeapp.generated.resources.ic_xs_sidebar_yellow_arrow

class FooterMenuConfig(
    val title: String,
    val icon: @Composable (() -> Unit)? = null,
    val color: NavigationDrawerItemColors,
    val onClick: () -> Unit
)

@Composable
fun FooterMenu(
    navCtrl: NavController,
    onDismiss: () -> Unit,
) {
    val authDomain: AuthDomain = koinInject()
    val viewModel = koinInject<SidebarViewModel>()

    val userInfo by viewModel.userInfo.collectAsState()
    val subscriptionInfo by viewModel.subscriptionInfo.collectAsState()
    val isSubscribed = subscriptionInfo?.checkIsSubscribed() == true

    if (!isSubscribed && !isAndroid()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Button(
                onClick = {
                    onDismiss()
                    navCtrl.navigate(SubscriptionManagerRoutes)
                    subscriptionEvent.view().source("screen").send()
                },
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
                        "sidebar_subscription".i18n(),
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
    }

    mapOf(
        "subscription" to FooterMenuConfig(
            title = "sidebar_subscription".i18n(),
            icon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_xs_sidebar_subscribe),
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
        "setting" to FooterMenuConfig(
            title = "sidebar_settings".i18n(),
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
        "feedback" to FooterMenuConfig(
            title = "sidebar_feedback".i18n(),
            icon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_xs_sidebar_feedback),
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
                navCtrl.navigateToRN(
                    RNRoute("RNFeedbackPage"), params = mapOf(
                        "email" to userInfo!!.email,
                        "entryPoint" to "inbox",
                        "version" to "${SlaxConfig.APP_VERSION_NAME} (${SlaxConfig.APP_VERSION_CODE})"
                    )
                )
                feedbackEvent.view().source("inbox").send()
            }
        ),
        "about" to FooterMenuConfig(
            title = "sidebar_about".i18n(),
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
        "logout" to FooterMenuConfig(
            title = "sidebar_logout".i18n(),
            icon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_xs_sidebar_logout),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp)
                )
            },
            color = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = Color.Transparent
            ),
            onClick = {
                authDomain.signOut()
            }
        )
    ).map {
        if (it.key == "subscription") {
            if (!isSubscribed) {
                return@map
            }
        }
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
}