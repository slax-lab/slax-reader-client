package com.slax.reader.ui.inbox.compenents

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.slax.reader.const.BookmarkRoutes
import com.slax.reader.data.database.model.InboxListBookmarkItem
import com.slax.reader.ui.inbox.InboxListViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_cell_action_archieve
import slax_reader_client.composeapp.generated.resources.ic_cell_action_star
import slax_reader_client.composeapp.generated.resources.ic_cell_more
import slax_reader_client.composeapp.generated.resources.ic_cell_more_archieve
import slax_reader_client.composeapp.generated.resources.ic_cell_more_delete
import slax_reader_client.composeapp.generated.resources.ic_cell_more_edittitle
import slax_reader_client.composeapp.generated.resources.ic_cell_more_highlighted
import slax_reader_client.composeapp.generated.resources.ic_cell_more_star
import slax_reader_client.composeapp.generated.resources.ic_floating_panel_archieved
import slax_reader_client.composeapp.generated.resources.ic_floating_panel_starred
import com.slax.reader.domain.sync.DownloadStatus

// 菜单触发源枚举
enum class MenuTriggerSource {
    NONE,           // 未触发
    LONG_PRESS,     // 长按触发
    MORE_ICON       // 点击更多触发
}

@Composable
fun BookmarkItemRow(
    navCtrl: NavController,
    bookmark: InboxListBookmarkItem,
    iconPainter: Painter,
    morePainter: Painter,
    downloadStatus: DownloadStatus?,
) {
    val haptics = LocalHapticFeedback.current
    val viewModel: InboxListViewModel = koinInject()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var menuTriggerSource by remember { mutableStateOf(MenuTriggerSource.NONE) }
    var lastMenuTriggerSource by remember { mutableStateOf(MenuTriggerSource.NONE) }
    val showMenu = menuTriggerSource != MenuTriggerSource.NONE

    var isEditingTitle by remember { mutableStateOf(false) }
    var editTitleText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isEditingTitle) {
        if (isEditingTitle) {
            focusRequester.requestFocus()
        }
    }

    val offsetXAnimatable = remember { Animatable(0f) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    val actualMorePainter = if (menuTriggerSource == MenuTriggerSource.MORE_ICON) {
        painterResource(Res.drawable.ic_cell_more_highlighted)
    } else {
        painterResource(Res.drawable.ic_cell_more)
    }

    // 点击交互状态
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var isLongPressed by remember { mutableStateOf(false) }

    // 动画效果
    val elevation by animateFloatAsState(
        targetValue = if (isLongPressed) 16f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    val scale by animateFloatAsState(
        targetValue = if (isLongPressed) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    // 滑动阈值
    val maxSwipeLeft = -130f
    val maxSwipeRight = 0f

    // 计算滑动进度（0-1），用于背景透明度渐变
    val swipeProgress = remember(offsetXAnimatable.value) {
        val leftProgress = if (offsetXAnimatable.value < 0f) {
            kotlin.math.abs(offsetXAnimatable.value) / kotlin.math.abs(maxSwipeLeft)
        } else 0f
        val rightProgress = if (offsetXAnimatable.value > 0f) {
            offsetXAnimatable.value / maxSwipeRight
        } else 0f
        kotlin.math.max(leftProgress, rightProgress).coerceIn(0f, 1f)
    }

    // 计算菜单透明度（0-1），根据滑动进度渐变
    val menuAlpha = remember(offsetXAnimatable.value) {
        if (offsetXAnimatable.value < 0f) {
            (kotlin.math.abs(offsetXAnimatable.value) / kotlin.math.abs(maxSwipeLeft)).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    val draggableState = rememberDraggableState { delta ->
        scope.launch {
            val newOffset = (offsetXAnimatable.value + delta).coerceIn(maxSwipeLeft, maxSwipeRight)
            offsetXAnimatable.snapTo(newOffset)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .zIndex(if (isLongPressed) 10f else 0f)
            .onSizeChanged { size ->
                boxSize = size
            }
    ) {
        if (offsetXAnimatable.value < 0f) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 18.dp)
                    .fillMaxHeight()
                    .graphicsLayer { alpha = menuAlpha },
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 归档按钮
                Surface(
                    modifier = Modifier
                        .size(40.dp, 30.dp)
                        .fillMaxHeight(),
                    color = Color(0xFF333333),
                    shape = RoundedCornerShape(15.dp),
                    onClick = {
                        scope.launch {
                            println("归档: ${bookmark.displayTitle()}")
                            offsetXAnimatable.animateTo(0f, animationSpec = tween(200))

                            viewModel.toggleArchive(bookmark.id, bookmark.archiveStatus != 1)
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(if (bookmark.archiveStatus == 1) Res.drawable.ic_floating_panel_archieved else Res.drawable.ic_cell_action_archieve),
                            contentDescription = "Archive",
                            modifier = Modifier.size(20.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                // 加星按钮
                Surface(
                    modifier = Modifier
                        .size(40.dp, 30.dp)
                        .fillMaxHeight(),
                    color = Color(0xFFFFB648),
                    shape = RoundedCornerShape(15.dp),
                    onClick = {
                        scope.launch {
                            println("加星: ${bookmark.displayTitle()}")
                            offsetXAnimatable.animateTo(0f, animationSpec = tween(200))

                            viewModel.toggleStar(bookmark.id, bookmark.isStarred != 1)
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(if (bookmark.isStarred == 1) Res.drawable.ic_floating_panel_starred else Res.drawable.ic_cell_action_star),
                            contentDescription = "Star",
                            modifier = Modifier.size(20.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }

        if (offsetXAnimatable.value > 0f) {
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(80.dp)
                    .fillMaxHeight(),
                color = Color(0xFFFFC107),
                onClick = {
                    scope.launch {
                        offsetXAnimatable.animateTo(0f, animationSpec = tween(200))
                    }
                }
            ) {

            }
        }

        // 主内容卡片
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = offsetXAnimatable.value.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(elevation.dp)
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        scope.launch {
                            // 自动回弹动画
                            val targetOffset = when {
                                offsetXAnimatable.value < -40f -> maxSwipeLeft
                                offsetXAnimatable.value > 40f -> maxSwipeRight
                                else -> 0f
                            }
                            offsetXAnimatable.animateTo(
                                targetValue = targetOffset,
                                animationSpec = tween(durationMillis = 200)
                            )
                        }
                    }
                )
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        if (offsetXAnimatable.value != 0f) {
                            // 如果有滑动偏移，点击时关闭
                            scope.launch {
                                offsetXAnimatable.animateTo(0f, animationSpec = tween(200))
                            }
                        } else {
                            navCtrl.navigate(BookmarkRoutes(bookmarkId = bookmark.id))
                        }
                    },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        isLongPressed = true
                        menuTriggerSource = MenuTriggerSource.LONG_PRESS
                    }
                ),
            color = Color(0xFFFCFCFC)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 内容
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 24.dp, end = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            val isDownloading = downloadStatus == DownloadStatus.DOWNLOADING
                            val isCompleted = downloadStatus == DownloadStatus.COMPLETED

                            Surface(
                                modifier = Modifier.size(18.dp),
                                shape = RoundedCornerShape(50),
                                color = Color(if (isCompleted) 0x0F0F1419 else 0xFFF5F5F3)
                            ) {}

                            Image(
                                painter = iconPainter,
                                contentDescription = "Article",
                                modifier = Modifier.size(12.dp),
                                contentScale = ContentScale.Fit
                            )

                            if (isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color(0xFF1DA1F2),
                                    strokeWidth = 2.dp,
                                    trackColor = Color.Transparent
                                )
                            }
                        }

                        if (isEditingTitle) {
                            BasicTextField(
                                value = editTitleText,
                                onValueChange = { editTitleText = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color.Transparent)
                                    .focusRequester(focusRequester),
                                textStyle = TextStyle(fontSize = 15.sp, lineHeight = 24.sp, color = Color(0xFF0F1419)),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        val trimmedTitle = editTitleText.trim()
                                        if (trimmedTitle.isEmpty() || editTitleText == bookmark.displayTitle()) {
                                            isEditingTitle = false
                                            editTitleText = ""
                                        } else {
                                            scope.launch {
                                                viewModel.editTitle(bookmark.id, trimmedTitle)
                                                isEditingTitle = false
                                                editTitleText = ""
                                            }
                                        }
                                    }
                                )
                            )
                        } else {
                            Text(
                                text = bookmark.displayTitle(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                                style = TextStyle(fontSize = 15.sp, lineHeight = 24.sp, color = Color(0xFF0F1419))
                            )
                        }
                    }

                    Image(
                        painter = actualMorePainter,
                        contentDescription = "More",
                        modifier = Modifier
                            .size(14.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                menuTriggerSource = MenuTriggerSource.MORE_ICON
                            },
                        contentScale = ContentScale.Fit
                    )
                }

                val overlayAlpha = if (isPressed) {
                    0.05f
                } else {
                    swipeProgress * 0.05f
                }

                if (overlayAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = overlayAlpha))
                    )
                }
            }
        }

        // 当菜单打开时，记录触发源
        if (menuTriggerSource != MenuTriggerSource.NONE && menuTriggerSource != lastMenuTriggerSource) {
            lastMenuTriggerSource = menuTriggerSource
        }

        val cellHeight = 48.dp

        // 计算菜单位置偏移 - 使用 lastMenuTriggerSource 保持菜单关闭时位置不变
        val menuOffset = when (lastMenuTriggerSource) {
            MenuTriggerSource.LONG_PRESS -> {
                DpOffset(x = 8.dp, y = cellHeight + 10.dp)
            }
            MenuTriggerSource.MORE_ICON -> {
                val boxWidthDp = with(density) { boxSize.width.toDp() }
                val menuWidth = 180.dp
                DpOffset(x = boxWidthDp - menuWidth - 8.dp, y = cellHeight - 10.dp)  // 48dp 是单元格高度
            }
            MenuTriggerSource.NONE -> DpOffset(x = 8.dp, y = cellHeight + 10.dp)  // 默认位置
        }

        Menu(
            expanded = showMenu,
            onDismissRequest = {
                menuTriggerSource = MenuTriggerSource.NONE
                isLongPressed = false
            },
            offset = menuOffset,
            modifier = Modifier.width(180.dp)
        ) {
            // 加星
            MenuItem(
                icon = painterResource(if (bookmark.isStarred == 1) Res.drawable.ic_floating_panel_starred else Res.drawable.ic_cell_more_star),
                text = "加星",
                onClick = {
                    scope.launch {
                        menuTriggerSource = MenuTriggerSource.NONE
                        isLongPressed = false
                        viewModel.toggleStar(bookmark.id, bookmark.isStarred != 1)
                    }
                }
            )

            // 归档
            MenuItem(
                icon = painterResource(if (bookmark.archiveStatus == 1) Res.drawable.ic_floating_panel_archieved else Res.drawable.ic_cell_more_archieve),
                text = "归档",
                onClick = {
                    scope.launch {
                        menuTriggerSource = MenuTriggerSource.NONE
                        isLongPressed = false
                        viewModel.toggleArchive(bookmark.id, bookmark.archiveStatus != 1)
                    }
                }
            )

            // 修改标题
            MenuItem(
                icon = painterResource(Res.drawable.ic_cell_more_edittitle),
                text = "修改标题",
                onClick = {
                    scope.launch {
                        menuTriggerSource = MenuTriggerSource.NONE
                        isLongPressed = false
                        editTitleText = bookmark.displayTitle()
                        isEditingTitle = true
                    }
                }
            )

            // 删除
            MenuItem(
                icon = painterResource(Res.drawable.ic_cell_more_delete),
                text = "删除",
                color = Color(0xFFF45454),
                onClick = {
                    scope.launch {
                        menuTriggerSource = MenuTriggerSource.NONE
                        isLongPressed = false
                        viewModel.deleteBookmark(bookmark.id)
                    }
                }
            )
        }
    }
}
