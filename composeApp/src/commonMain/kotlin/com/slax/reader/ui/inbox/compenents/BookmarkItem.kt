package com.slax.reader.ui.inbox.compenents

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.slax.reader.const.BookmarkRoutes
import com.slax.reader.data.database.model.InboxListBookmarkItem
import com.slax.reader.domain.sync.DownloadStatus
import com.slax.reader.ui.inbox.InboxListViewModel
import com.slax.reader.utils.platformType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.*
import kotlin.math.abs

// 菜单触发源枚举
enum class MenuTriggerSource {
    NONE,           // 未触发
    LONG_PRESS,     // 长按触发
    MORE_ICON       // 点击更多触发
}

@Composable
fun BookmarkItemRow(
    navCtrl: NavController,
    viewModel: InboxListViewModel,
    bookmark: InboxListBookmarkItem,
    iconPainter: Painter,
    onEditTitle: (InboxListBookmarkItem) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var menuTriggerSource by remember { mutableStateOf(MenuTriggerSource.NONE) }
    var lastMenuTriggerSource by remember { mutableStateOf(MenuTriggerSource.NONE) }
    val showMenu = menuTriggerSource != MenuTriggerSource.NONE

    val bookmarkStatusMap by viewModel.bookmarkStatusFlow.collectAsState()

    val bookmarkStatus by remember {
        derivedStateOf { bookmarkStatusMap[bookmark.id] }
    }

    // 闪烁动画
    val flashAlpha = remember { Animatable(0f) }
    var currentTitle by remember { mutableStateOf("") }

    LaunchedEffect(bookmark.displayTitle()) {
        if (currentTitle.isEmpty()) currentTitle = bookmark.displayTitle()
        if (flashAlpha.value == 0f && currentTitle != bookmark.displayTitle()) {
            repeat(3) {
                flashAlpha.animateTo(0.05f, tween(180))
                flashAlpha.animateTo(0f, tween(180))
            }
            currentTitle = bookmark.displayTitle()
        }
    }

    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    var totalDragDistance by remember { mutableStateOf(0f) }
    var dragStartOffset by remember { mutableStateOf(0f) }

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

    val maxSwipeLeft = remember(density) { -with(density) { 130.dp.toPx() } }
    val maxSwipeRight = 0f
    val clickDragTolerancePx = remember(density) { with(density) { 8.dp.toPx() } }

    val offsetXAnimatable = remember { Animatable(0f) }

    LaunchedEffect(maxSwipeLeft, maxSwipeRight) {
        offsetXAnimatable.updateBounds(maxSwipeLeft, maxSwipeRight)
        offsetXAnimatable.snapTo(offsetXAnimatable.value.coerceIn(maxSwipeLeft, maxSwipeRight))
    }

    val swipeProgress by remember {
        derivedStateOf {
            val offset = offsetXAnimatable.value
            if (offset < 0f) {
                (abs(offset) / abs(maxSwipeLeft)).coerceIn(0f, 1f)
            } else 0f
        }
    }

    val menuAlpha by remember {
        derivedStateOf {
            val offset = offsetXAnimatable.value
            if (offset < 0f) {
                (abs(offset) / abs(maxSwipeLeft)).coerceIn(0f, 1f)
            } else 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(53.dp)
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
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = offsetXAnimatable.value
                    scaleX = scale
                    scaleY = scale
                    shadowElevation = elevation
                }
                .pointerInput(maxSwipeLeft, maxSwipeRight) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isDragging = true
                            totalDragDistance = 0f
                            dragStartOffset = offsetXAnimatable.value
                            scope.launch { offsetXAnimatable.stop() }
                        },
                        onDragEnd = {
                            val currentOffset = offsetXAnimatable.value
                            val isDraggingToClose = currentOffset > dragStartOffset // 从左向右，值变大（负数绝对值变小）

                            val threshold = if (isDraggingToClose) {
                                // 关闭方向
                                abs(maxSwipeLeft) * 0.8f
                            } else {
                                // 打开方向
                                abs(maxSwipeLeft) * 0.4f
                            }

                            val shouldOpen = abs(currentOffset) >= threshold
                            scope.launch {
                                offsetXAnimatable.animateTo(
                                    if (shouldOpen) maxSwipeLeft else 0f,
                                    animationSpec = tween(200)
                                )
                                delay(50)
                                isDragging = false
                                totalDragDistance = 0f
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                // 取消拖动时也回弹
                                offsetXAnimatable.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(durationMillis = 200)
                                )
                                delay(50)
                                isDragging = false
                                totalDragDistance = 0f
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            totalDragDistance += abs(dragAmount)
                            val newOffset = (offsetXAnimatable.value + dragAmount).coerceIn(maxSwipeLeft, maxSwipeRight)
                            viewModel.viewModelScope.launch {
                                offsetXAnimatable.snapTo(newOffset)
                            }
                        }
                    )
                }
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = !isDragging,
                    onClick = {
                        // 检查是否真的是点击而非滑动
                        if (totalDragDistance < clickDragTolerancePx) {
                            if (offsetXAnimatable.value != 0f) {
                                // 如果有滑动偏移,点击时关闭
                                scope.launch {
                                    offsetXAnimatable.animateTo(0f, animationSpec = tween(200))
                                }
                            } else {
                                if (platformType == "ios") {
                                    navigateToDetail(bookmark.id, bookmark.displayTitle())
                                } else {
                                    navCtrl.navigate(BookmarkRoutes(bookmarkId = bookmark.id))
                                }
                            }
                        }
                    },
                    onLongClick = {
                        // 只有在未拖动时才触发长按
                        if (!isDragging && totalDragDistance < clickDragTolerancePx) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            isLongPressed = true
                            menuTriggerSource = MenuTriggerSource.LONG_PRESS
                        }
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
                            val isDownloading = bookmarkStatus?.status == DownloadStatus.DOWNLOADING
                            val isCompleted = bookmarkStatus?.status == DownloadStatus.COMPLETED

                            Surface(
                                modifier = Modifier.size(18.dp),
                                shape = RoundedCornerShape(50),
                                color = Color(if (isCompleted) 0xFFC4C4C2 else 0xFFF5F5F3)
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

                        Text(
                            text = bookmark.displayTitle(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                            style = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, color = Color(0xFF0F1419))
                        )
                    }
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

                // 闪烁效果覆盖层（使用长按时的高亮效果）
                if (flashAlpha.value > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = flashAlpha.value))
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
        val boxWidthDp = with(density) { boxSize.width.toDp() }
        val menuWidth = 180.dp
        val menuOffset = DpOffset(
            x = boxWidthDp - menuWidth - 8.dp,
            y = cellHeight - 10.dp
        )

        if (showMenu) {
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
                        menuTriggerSource = MenuTriggerSource.NONE
                        isLongPressed = false
                        onEditTitle(bookmark)
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
}

expect fun navigateToDetail(bookmarkId: String, title: String)