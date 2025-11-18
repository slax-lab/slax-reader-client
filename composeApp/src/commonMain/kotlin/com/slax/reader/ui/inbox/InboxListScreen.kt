package com.slax.reader.ui.inbox

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.slax.reader.const.AboutRoutes
import com.slax.reader.const.BookmarkRoutes
import com.slax.reader.const.SettingsRoutes
import com.slax.reader.const.SpaceManagerRoutes
import com.slax.reader.data.database.model.InboxListBookmarkItem
import com.slax.reader.ui.inbox.compenents.*
import com.slax.reader.ui.sidebar.Sidebar
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_inbox_tab

@Composable
fun InboxListScreen(navCtrl: NavController) {
    val inboxViewModel = koinInject<InboxListViewModel>()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    println("[watch][UI] recomposition InboxListScreen")

    Sidebar(
        drawerState = drawerState,
        onSettingsClick = {
            navCtrl.navigate(SettingsRoutes)
        },
        onAboutClick = {
            navCtrl.navigate(AboutRoutes)
        },
        onSpaceManagerClick = {
            navCtrl.navigate(SpaceManagerRoutes)
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F3))
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.statusBarsPadding()
                    ) {
                        NavigationBar(
                            onAvatarClick = {
                                scope.launch {
                                    drawerState.open()
                                }
                            }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 20.dp)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                inboxViewModel.scrollToTop()
                            }
                    )
                }

                ContentSection(
                    navCtrl = navCtrl,
                    inboxViewModel = inboxViewModel,
                )
            }
        }
    }
}

@Composable
private fun NavigationBar(
    onAvatarClick: () -> Unit = {}
) {
    println("[watch][UI] recomposition NavigationBar")
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_inbox_tab),
                contentDescription = "Menu",
                modifier = Modifier.size(24.dp, 24.dp)
                    .alpha(if (isPressed) 0.5f else 1f)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        onAvatarClick()
                    },
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
private fun ContentSection(
    navCtrl: NavController,
    inboxViewModel: InboxListViewModel,
) {
    println("[watch][UI] recomposition ContentSection")

    var editingBookmark by remember { mutableStateOf<InboxListBookmarkItem?>(null) }

    Box(
        modifier = Modifier.fillMaxSize().padding(top = 8.dp).clipToBounds()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = Color(0xFFFCFCFC),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            InboxTitleRow()
            Spacer(modifier = Modifier.height(4.dp))

            ArticleList(
                navCtrl = navCtrl,
                viewModel = inboxViewModel,
                onEditTitle = { bookmark ->
                    editingBookmark = bookmark
                },
            )
        }

        ContinueReading(
            onClick = { bookmarkId ->
                navCtrl.navigate(BookmarkRoutes(bookmarkId = bookmarkId))
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        editingBookmark?.let { bookmark ->
            TitleEditOverlay(
                bookmark = bookmark,
                viewModel = inboxViewModel,
                onDismiss = {
                    editingBookmark = null
                }
            )
        }
    }
}
