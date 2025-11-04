package com.slax.reader.ui.bookmark.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slax.reader.data.database.model.UserTag
import com.slax.reader.ui.bookmark.BookmarkDetailViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_cell_tag
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

    val coroutineScope = rememberCoroutineScope()
    var isCreatingMode by remember { mutableStateOf(false) }

    var inputText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    var addedTags by remember { mutableStateOf<List<UserTag>>(emptyList()) }
    val availableTags by detailViewModel.userTagList.collectAsState(emptyList())
    var currentSelectedTags by remember(addedTags) { mutableStateOf(addedTags) }
    val unselectedTags by remember(availableTags, currentSelectedTags) {
        derivedStateOf {
            availableTags.filter { tag -> currentSelectedTags.none { it.id == tag.id } }
        }
    }
    val similarTags by remember(inputText, availableTags, currentSelectedTags) {
        derivedStateOf {
            if (inputText.isEmpty()) emptyList()
            else availableTags
                .filter { it !in currentSelectedTags }
                .filter { it.tag_name.contains(inputText, ignoreCase = true) }
                .take(5)
        }
    }

    val density = LocalDensity.current
    var offsetY by remember { mutableFloatStateOf(0f) }


    LaunchedEffect(currentTags) {
        currentTags?.let { tagIds ->
            addedTags = detailViewModel.getTagNames(tagIds)
        }
    }

    LaunchedEffect(addedTags) {
        currentSelectedTags = addedTags
    }

    LaunchedEffect(isCreatingMode) {
        if (isCreatingMode) {
            focusRequester.requestFocus()
        }
    }

    fun exitCreatingMode() {
        isCreatingMode = false
        inputText = ""
    }

    fun createAndAddTag() {
        if (inputText.isNotBlank()) {
            coroutineScope.launch {
                val newTag = detailViewModel.createTag(inputText.trim())
                currentSelectedTags = currentSelectedTags + newTag
                exitCreatingMode()
            }
        }
    }

    fun addExistingTag(tag: UserTag) {
        currentSelectedTags = currentSelectedTags + tag
        exitCreatingMode()
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
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {

        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .heightIn(max = with(density) { (density.density * 800).dp })
                .padding(0.dp)
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
                        text = if (isCreatingMode) "返回" else "取消",
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (isCreatingMode) {
                                    exitCreatingMode()
                                } else {
                                    onDismissRequest()
                                }
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

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isCreatingMode,
                        enter = fadeIn(animationSpec = tween(300)),
                        exit = fadeOut(animationSpec = tween(300))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
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
                                                onClick = {},
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

                            Text(
                                text = "可添加",
                                modifier = Modifier.padding(top = 30.dp, bottom = 12.dp),
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = Color(0xFF999999)
                                )
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .border(
                                        width = 0.5.dp,
                                        color = Color(0x140F1419),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .background(
                                        color = Color(0xF2F5F5F3),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        isCreatingMode = true
                                    }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "创建新标签",
                                    style = TextStyle(
                                        fontSize = 15.sp,
                                        color = Color(0x99A28D64),
                                        lineHeight = 21.sp
                                    )
                                )
                            }

                            if (unselectedTags.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))

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
                            } else {
                                Spacer(modifier = Modifier.height(28.dp))
                            }
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = isCreatingMode,
                        enter = fadeIn(animationSpec = tween(300)),
                        exit = fadeOut(animationSpec = tween(300))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(
                                    top = 20.dp,
                                    bottom = maxOf(
                                        WindowInsets.ime.asPaddingValues().calculateBottomPadding(),
                                        330.dp
                                    )
                                )
                        ) {
                            BasicTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .border(
                                        width = 0.5.dp,
                                        color = Color(0x140F1419),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .background(
                                        color = Color(0xF2F5F5F3),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .focusRequester(focusRequester),
                                textStyle = TextStyle(
                                    fontSize = 15.sp,
                                    color = Color(0xFF0F1419),
                                    fontWeight = FontWeight.Bold
                                ),
                                cursorBrush = SolidColor(Color(0xFF16b998)),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (inputText.isEmpty()) {
                                            Text(
                                                text = "创建新标签",
                                                style = TextStyle(
                                                    fontSize = 15.sp,
                                                    color = Color(0x99A28D64),
                                                    lineHeight = 21.sp
                                                )
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                color = Color.Transparent
                            ) {
                                if (!inputText.isNotBlank()) {
                                    return@Surface
                                }

                                val createInteractionSource = remember { MutableInteractionSource() }
                                val isCreatePressed by createInteractionSource.collectIsPressedAsState()

                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(if (isCreatePressed) 0.65f else 1f)
                                        .clickable(
                                            interactionSource = createInteractionSource,
                                            indication = null
                                        ) {
                                            createAndAddTag()
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_cell_tag),
                                        contentDescription = null,
                                        tint = Color.Unspecified,
                                        modifier = Modifier.padding(start = 11.dp).size(14.dp)
                                    )

                                    Text(
                                        text = "创建：$inputText",
                                        style = TextStyle(
                                            fontSize = 15.sp,
                                            color = Color(0xFFA28D64)
                                        ),
                                        modifier = Modifier.padding(start = 11.dp)
                                    )
                                }
                            }


                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp),
                                color = Color.Transparent
                            ) {
                                if (!(inputText.isNotBlank() && similarTags.isNotEmpty())) {
                                    return@Surface
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0x14333333))
                                )
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp),
                                color = Color.Transparent
                            ) {
                                if (!similarTags.isNotEmpty()) {
                                    return@Surface
                                }

                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                ) {
                                    items(similarTags) { tag ->
                                        val itemInteractionSource = remember { MutableInteractionSource() }
                                        val isItemPressed by itemInteractionSource.collectIsPressedAsState()

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp)
                                                .alpha(if (isItemPressed) 0.65f else 1f)
                                                .clickable(
                                                    interactionSource = itemInteractionSource,
                                                    indication = null
                                                ) {
                                                    addExistingTag(tag)
                                                },
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Start
                                        ) {
                                            Icon(
                                                painter = painterResource(Res.drawable.ic_cell_tag),
                                                contentDescription = null,
                                                tint = Color.Unspecified,
                                                modifier = Modifier.padding(start = 11.dp).size(14.dp)
                                            )

                                            Text(
                                                modifier = Modifier.padding(start = 11.dp),
                                                text = buildAnnotatedString {
                                                    val tagName = tag.tag_name
                                                    val searchText = inputText
                                                    val startIndex = tagName.indexOf(searchText, ignoreCase = true)

                                                    if (startIndex >= 0) {
                                                        append(tagName.take(startIndex))
                                                        withStyle(
                                                            style = SpanStyle(
                                                                color = Color(0xFF333333),
                                                            )
                                                        ) {
                                                            append(
                                                                tagName.substring(
                                                                    startIndex,
                                                                    startIndex + searchText.length
                                                                )
                                                            )
                                                        }
                                                        append(tagName.substring(startIndex + searchText.length))
                                                    } else {
                                                        append(tagName)
                                                    }
                                                },
                                                style = TextStyle(
                                                    fontSize = 15.sp,
                                                    color = Color(0xFF999999),
                                                    lineHeight = 21.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}