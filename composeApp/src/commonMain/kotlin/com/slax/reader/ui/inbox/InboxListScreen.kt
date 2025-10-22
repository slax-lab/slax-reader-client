package com.slax.reader.ui.inbox

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.slax.reader.domain.auth.AuthDomain
import com.slax.reader.ui.inbox.compenents.ArticleList
import com.slax.reader.ui.inbox.compenents.InboxTitleRow
import com.slax.reader.ui.inbox.compenents.UserAvatar
import com.slax.reader.ui.sidebar.SideBar
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.inbox_tab

@Composable
fun InboxListScreen(navCtrl: NavController) {
    var showSidebar by remember { mutableStateOf(false) }
    val authDomain: AuthDomain = koinInject()

    // println("[watch][UI] recomposition InboxListScreen")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F3))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            NavigationBar(onAvatarClick = { showSidebar = true })
            Spacer(modifier = Modifier.height(8.dp))
            ContentSection(navCtrl)
        }

        AnimatedVisibility(
            visible = showSidebar,
            enter = EnterTransition.None,
            exit = ExitTransition.None,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        showSidebar = false
                    }
            )
        }

        AnimatedVisibility(
            visible = showSidebar,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it })
        ) {
            SideBar(
                navController = navCtrl,
                onLogout = {
                    authDomain.signOut()
                }
            )
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
