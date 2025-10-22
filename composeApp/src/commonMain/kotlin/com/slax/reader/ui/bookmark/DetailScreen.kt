package com.slax.reader.ui.bookmark

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.slax.reader.data.database.model.UserTag
import com.slax.reader.ui.bookmark.components.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.koin.compose.viewmodel.koinViewModel
import slax_reader_client.composeapp.generated.resources.*

data class ToolbarIcon(
    val label: String,
    val iconRes: DrawableResource? = null
)

data class OverviewViewBounds(
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f
)

@Composable
fun DetailScreen(nav: NavController, bookmarkId: String) {
    val detailView = koinViewModel<BookmarkDetailViewModel>()

    // println("[watch][UI] recomposition DetailScreen")

    val details by detailView.bookmarkDetail.collectAsState()
    val detail = details.firstOrNull()

    var currentTags: List<UserTag> by remember { mutableStateOf(emptyList()) }
    LaunchedEffect(bookmarkId, detail?.metadataObj?.tags) {
        detailView.setBookmarkId(bookmarkId)
        if (detail?.metadataObj?.tags != null) {
            currentTags = detailView.getTagNames(detail.metadataObj!!.tags)
        }
    }

    var showTagView by remember { mutableStateOf(false) }
    var showOverviewDialog by remember { mutableStateOf(false) }
    var showToolbar by remember { mutableStateOf(false) }
    var overviewBounds by remember { mutableStateOf(OverviewViewBounds()) }

    // FloatingActionBar 的显示和隐藏状态
    val scrollState = rememberScrollState()
    var manuallyVisible by remember { mutableStateOf(true) }

    // 使用 derivedStateOf 优化性能，只在滚动状态变化时重组
    val isFloatingBarVisible by remember {
        derivedStateOf {
            manuallyVisible
        }
    }

    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress && manuallyVisible) {
            // 滚动时隐藏
            manuallyVisible = false
        } else if (!scrollState.isScrollInProgress && scrollState.value == 0) {
            // 滚动停止且在顶部时恢复显示
            manuallyVisible = true
        }
    }

    val toolbarPages = remember {
        listOf(
            listOf(
                ToolbarIcon("Chat", Res.drawable.ic_bottom_panel_chatbot),
                ToolbarIcon("总结全文", Res.drawable.ic_bottom_panel_summary),
                ToolbarIcon("加星", Res.drawable.ic_bottom_panel_star),
                ToolbarIcon("归档", Res.drawable.ic_bottom_panel_archieve),
                ToolbarIcon("划线", Res.drawable.ic_bottom_panel_underline),
                ToolbarIcon("评论", Res.drawable.ic_bottom_panel_comment),
                ToolbarIcon("改标题", Res.drawable.ic_bottom_panel_comment),
                ToolbarIcon("分享", Res.drawable.ic_bottom_panel_share)
            ),
            listOf(
                ToolbarIcon("反馈", Res.drawable.ic_bottom_panel_feedback),
                ToolbarIcon("删除", Res.drawable.ic_bottom_panel_delete),
            )
        )
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFCFCFC))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (!isFloatingBarVisible) {
                            manuallyVisible = true
                        }
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // 页面内容从导航栏下方开始
            NavigatorBarSpacer()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 58.dp)
            ) {
                Text(
                    text = detail?.displayTitle ?: "",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 30.sp,
                        color = Color(0xFF0f1419)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = detail?.displayTime ?: "",
                        style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, color = Color(0xFF999999))
                    )
                    Text(
                        "查看原网页",
                        modifier = Modifier.padding(start = 16.dp).clickable {
                            println("被点击了")
                        },
                        style = TextStyle(color = Color(0xFF5490C2), fontSize = 14.sp, lineHeight = 20.sp)
                    )
                }

                TagsView(
                    modifier = Modifier.padding(top = 16.dp),
                    tags = currentTags,
                    onAddTagClick = { showTagView = true }
                )

                OverviewView(
                    modifier = Modifier.padding(top = 20.dp),
                    onExpand = { showOverviewDialog = true },
                    onBoundsChanged = { bounds -> overviewBounds = bounds }
                )

                BookmarkContentView(
                    bookmarkId = bookmarkId,
                    onWebViewTap = {
                        // 在顶部的时候，不允许隐藏
                        // 非顶部的时候，可以点击进行隐藏、显示的切换
                        manuallyVisible = if (scrollState.value == 0) {
                            true
                        } else !isFloatingBarVisible
                    }
                )
            }
        }

        // 底部悬浮操作栏 - 带有滑动动画
        val floatingBarOffsetY by animateDpAsState(
            targetValue = if (isFloatingBarVisible) 0.dp else 150.dp,
            animationSpec = tween(durationMillis = 300)
        )

        FloatingActionBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 58.dp)
                .offset(y = floatingBarOffsetY),
            onMoreClick = { showToolbar = true }
        )

        // 标签管理界面
        TagsManageBottomSheet(
            visible = showTagView,
            onDismissRequest = { showTagView = false },
            enableDrag = false,
            addedTags = currentTags,
            onConfirm = { selectedTags ->
                currentTags = selectedTags
                detailView.viewModelScope.launch {
                    detailView.updateBookmarkTags(bookmarkId, selectedTags.map { it.id })
                }
            }
        )

        // Overview 弹窗
        if (showOverviewDialog) {
            OverviewDialog(
                visible = showOverviewDialog,
                onDismissRequest = { showOverviewDialog = false },
                sourceBounds = overviewBounds
            )
        }


        // 底部工具栏
        BottomToolbarSheet(
            visible = showToolbar,
            onDismissRequest = { showToolbar = false },
            pages = toolbarPages,
            onIconClick = { pageIndex, iconIndex ->
                println("点击了第 ${pageIndex + 1} 页的第 ${iconIndex + 1} 个图标")
                showToolbar = false
            }
        )

        NavigatorBar(
            navController = nav,
            visible = isFloatingBarVisible
        )
    }
}
