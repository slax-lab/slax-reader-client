package com.slax.reader.ui.bookmark.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.slax.reader.data.database.model.UserTag
import com.slax.reader.ui.bookmark.BookmarkDetailViewModel
import com.slax.reader.utils.i18n
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_cell_tag

@Composable
fun TagCreatingScreen(
    detailViewModel: BookmarkDetailViewModel,
    onTagCreated: (UserTag) -> Unit,
    onBack: () -> Unit
) {
    val inputState = rememberTextFieldState("")
    val focusRequester = remember { FocusRequester() }

    val availableTags by detailViewModel.userTagList.collectAsState(emptyList())
    val selectedTags by detailViewModel.selectedTagList.collectAsState()

    val similarTags by remember {
        derivedStateOf {
            val input = inputState.text.toString()
            if (input.isEmpty()) {
                emptyList()
            } else {
                availableTags
                    .filter { it !in selectedTags }
                    .filter { it.tag_name.contains(input, ignoreCase = true) }
                    .sortedBy { tag ->
                        // 完全匹配的排在最前面
                        if (tag.tag_name.equals(input, ignoreCase = true)) 0
                        // 以输入开头的排第二
                        else if (tag.tag_name.startsWith(input, ignoreCase = true)) 1
                        // 其他包含的排最后
                        else 2
                    }
            }
        }
    }

    val hasExactMatch by remember {
        derivedStateOf {
            val input = inputState.text.toString()
            input.isNotBlank() && similarTags.any {
                it.tag_name.equals(input, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    fun createNewTag() {
        val tagName = inputState.text.toString().trim()
        if (tagName.isBlank()) return

        // 检查是否已存在（大小写不敏感）
        val existingTag = availableTags.find {
            it.tag_name.equals(tagName, ignoreCase = true)
        }

        if (existingTag != null) {
            // 如果已存在且未选中，直接添加
            if (existingTag !in selectedTags) {
                onTagCreated(existingTag)
            }
            onBack()
            return
        }

        // 创建新标签
        detailViewModel.viewModelScope.launch {
            val newTag = detailViewModel.createTag(tagName)
            onTagCreated(newTag)
            onBack()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "btn_back".i18n(),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onBack() },
                    style = TextStyle(
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        color = Color(0xFF333333)
                    )
                )

                Text(
                    text = "tags_create_title".i18n(),
                    modifier = Modifier.align(Alignment.Center),
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp,
                        color = Color(0xFF0F1419)
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

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = WindowInsets.ime.asPaddingValues()
            ) {
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }

                item {
                    BasicTextField(
                        state = inputState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
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
                        lineLimits = TextFieldLineLimits.SingleLine,
                        cursorBrush = SolidColor(Color(0xFF16b998)),
                        onKeyboardAction = {
                            createNewTag()
                        },
                        decorator = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (inputState.text.isEmpty()) {
                                    Text(
                                        text = "tags_input_placeholder".i18n(),
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
                }

                if (inputState.text.isNotBlank() && !hasExactMatch) {
                    item {
                        val createInteractionSource = remember { MutableInteractionSource() }
                        val isCreatePressed by createInteractionSource.collectIsPressedAsState()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .padding(horizontal = 24.dp)
                                .alpha(if (isCreatePressed) 0.65f else 1f)
                                .clickable(
                                    interactionSource = createInteractionSource,
                                    indication = null
                                ) { createNewTag() },
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
                                text = "${"tags_create_prefix".i18n()}${inputState.text}",
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    color = Color(0xFFA28D64)
                                ),
                                modifier = Modifier.padding(start = 11.dp)
                            )
                        }
                    }
                }

                if (inputState.text.isNotBlank() && similarTags.isNotEmpty() && !hasExactMatch) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .height(0.5.dp)
                                .background(Color(0x14333333))
                        )
                    }
                }

                items(
                    items = similarTags,
                    key = { it.id }
                ) { tag ->
                    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                        SimilarTagItem(
                            tag = tag,
                            searchText = inputState.text.toString(),
                            onClick = {
                                onTagCreated(tag)
                                onBack()
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}


@Composable
private fun SimilarTagItem(
    tag: UserTag,
    searchText: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .alpha(if (isPressed) 0.65f else 1f)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
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
            text = highlightSearchText(
                text = tag.tag_name,
                searchText = searchText,
                highlightColor = Color(0xFF333333)
            ),
            style = TextStyle(
                fontSize = 15.sp,
                color = Color(0xFF999999),
                lineHeight = 21.sp
            )
        )
    }
}

private fun highlightSearchText(
    text: String,
    searchText: String,
    highlightColor: Color
) = buildAnnotatedString {
    if (searchText.isEmpty()) {
        append(text)
        return@buildAnnotatedString
    }

    val startIndex = text.indexOf(searchText, ignoreCase = true)

    if (startIndex >= 0) {
        // 匹配前的文本
        append(text.take(startIndex))

        // 高亮匹配的文本
        withStyle(style = SpanStyle(color = highlightColor)) {
            append(text.substring(startIndex, startIndex + searchText.length))
        }

        // 匹配后的文本
        append(text.substring(startIndex + searchText.length))
    } else {
        append(text)
    }
}
