package com.slax.reader.ui.bookmark.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slax.reader.data.database.model.UserTag
import com.slax.reader.ui.bookmark.BookmarkDetailViewModel
import kotlin.math.roundToInt

@Composable
fun TagsManageBottomSheet(
    detailViewModel: BookmarkDetailViewModel,
    currentTags: List<String>?,
    onDismissRequest: () -> Unit,
    enableDrag: Boolean = false,
    onConfirm: (List<UserTag>) -> Unit = {}
) {
    println("[watch][UI] recomposition TagsManageBottomSheet")

    // 获取当前已添加的标签
    var addedTags by remember { mutableStateOf<List<UserTag>>(emptyList()) }
    LaunchedEffect(currentTags) {
        currentTags?.let { tagIds ->
            addedTags = detailViewModel.getTagNames(tagIds)
        }
    }

    // 获取所有可用标签
    val availableTags by detailViewModel.userTagList.collectAsState(emptyList())

    val density = LocalDensity.current
    var offsetY by remember { mutableFloatStateOf(0f) }
    var currentSelectedTags by remember(addedTags) { mutableStateOf(addedTags) }

    // 监听 addedTags 变化，更新 currentSelectedTags
    LaunchedEffect(addedTags) {
        currentSelectedTags = addedTags
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onDismissRequest()
            }
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {

        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, offsetY.roundToInt()) }
                .then(
                    if (enableDrag) {
                        Modifier
                            .draggable(
                                orientation = Orientation.Vertical,
                                state = rememberDraggableState { delta ->
                                    offsetY = (offsetY + delta).coerceAtLeast(0f)
                                },
                                onDragStopped = {
                                    if (offsetY > with(density) { 100.dp.toPx() }) {
                                        onDismissRequest()
                                    } else {
                                        offsetY = 0f
                                    }
                                }
                            )
                    } else {
                        Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {}
                    }
                ),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = "取消",
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onDismissRequest()
                            },
                        style = TextStyle(
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            color = Color(0xFF333333)
                        )
                    )

                    Text(
                        text = "标签",
                        modifier = Modifier.align(Alignment.Center),
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 20.sp,
                            color = Color(0xFF0F1419)
                        )
                    )

                    Text(
                        text = "确定",
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onConfirm(currentSelectedTags)
                                onDismissRequest()
                            },
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 20.sp,
                            color = Color(0xFF16b998)
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(0.5.dp)
                        .background(Color(0x14333333))
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 28.dp)
                ) {
                    if (currentSelectedTags.isNotEmpty()) {
                        Text(
                            text = "已添加",
                            modifier = Modifier.padding(top = 20.dp, bottom = 12.dp),
                            style = TextStyle(
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = Color(0xFF999999)
                            )
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            currentSelectedTags.forEach { tag ->
                                key(tag.id) {
                                    TagItem(
                                        tag = tag.tag_name,
                                        onClick = { /* 不需要 */ },
                                        showDeleteButton = true,
                                        onDelete = {
                                            currentSelectedTags = currentSelectedTags - tag
                                        },
                                        isLargeStyle = true
                                    )
                                }
                            }
                        }
                    }

                    val unselectedTags = availableTags.filter { it !in currentSelectedTags }
                    if (unselectedTags.isNotEmpty()) {
                        Text(
                            text = "可添加",
                            modifier = Modifier.padding(
                                top = if (currentSelectedTags.isNotEmpty()) 30.dp else 30.dp,
                                bottom = 12.dp
                            ),
                            style = TextStyle(
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = Color(0xFF999999)
                            )
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(bottom = 28.dp)
                        ) {
                            unselectedTags.forEach { tag ->
                                TagItem(
                                    tag = tag.tag_name,
                                    onClick = {
                                        currentSelectedTags = currentSelectedTags + tag
                                    },
                                    isLargeStyle = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

