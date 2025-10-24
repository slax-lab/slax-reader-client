package com.slax.reader.ui.inbox

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.slax.reader.const.AboutRoutes
import com.slax.reader.const.SettingsRoutes
import com.slax.reader.const.SpaceManagerRoutes
import com.slax.reader.domain.auth.AuthDomain
import com.slax.reader.ui.AppViewModel
import com.slax.reader.ui.inbox.compenents.ArticleList
import com.slax.reader.ui.inbox.compenents.InboxTitleRow
import com.slax.reader.ui.inbox.compenents.UserAvatar
import com.slax.reader.ui.sidebar.Sidebar
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.inbox_tab

@Composable
fun InboxListScreen(navCtrl: NavController) {
    val authDomain: AuthDomain = koinInject()
    val viewModel = koinInject<AppViewModel>()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(viewModel)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(viewModel)
        }
    }
// MARK:    navController.navigate(SpaceManagerRoutes)
    Sidebar(
        drawerState = drawerState,
        onSettingsClick = {
            navCtrl.navigate(SettingsRoutes)
        },
        onAboutClick = {
            navCtrl.navigate(AboutRoutes)
        },
        onLogout = {
            authDomain.signOut()
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F3))
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                NavigationBar(onAvatarClick = {
                    scope.launch {
                        drawerState.open()
                    }
                })
                Spacer(modifier = Modifier.height(8.dp))
                ContentSection(navCtrl)
            }
        }
    }
}

@Composable
private fun NavigationBar(onAvatarClick: () -> Unit = {}) {
    // println("[watch][UI] recomposition NavigationBar")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .width(70.dp)
                .align(Alignment.CenterStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(Res.drawable.inbox_tab),
                contentDescription = "Menu",
                modifier = Modifier.size(20.dp, 20.dp).clickable(onClick = onAvatarClick),
                contentScale = ContentScale.Fit
            )
            UserAvatar()
        }

        Text(
            text = "Slax Reader",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF0F1419),
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun ContentSection(navCtrl: NavController) {
    // println("[watch][UI] recomposition ContentSection")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = Color(0xFFFCFCFC),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(17.dp))
        InboxTitleRow()
        Spacer(modifier = Modifier.height(16.dp))
        ArticleList(navCtrl)
    }
}
