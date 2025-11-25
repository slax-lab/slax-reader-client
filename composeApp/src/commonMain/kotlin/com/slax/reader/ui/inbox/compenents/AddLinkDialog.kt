package com.slax.reader.ui.inbox.compenents

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.slax.reader.ui.inbox.InboxListViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_xs_dialog_close

@Composable
fun AddLinkDialog(
    inboxView: InboxListViewModel,
    visible: Boolean,
    onDismissRequest: () -> Unit,
    onViewOriginalArticle: (bookmarkId: String) -> Unit
) {
    val inputState = remember { TextFieldState("") }
    val focusRequester = remember { FocusRequester() }

    var internalVisible by remember { mutableStateOf(false) }
    var addedBookmarkId by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(visible) {
        internalVisible = visible
        if (visible) {
            delay(100)
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(internalVisible) {
        if (!internalVisible) {
            delay(300)
            onDismissRequest()
        }
    }

    // 监听输入变化，清除错误提示
    LaunchedEffect(inputState.text) {
        if (errorMessage != null) {
            errorMessage = null
        }
    }

    fun isValidUrl(url: String): Boolean {
        // URL 正则表达式，支持 http、https 协议
        val urlPattern = "^(https?://)?(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)$".toRegex()
        return url.matches(urlPattern)
    }

    fun onConfirm() {
        val text = inputState.text.toString().trim()
        when {
            text.isEmpty() -> {
                errorMessage = "请输入链接"
            }
            !isValidUrl(text) -> {
                errorMessage = "请输入有效的网址链接"
            }
            else -> {
                inboxView.viewModelScope.launch {
                    addedBookmarkId = inboxView.addLinkBookmark(text)
                    delay(100)
                    inboxView.scrollToTop()
                }
            }
        }
    }

    AnimatedVisibility(
        visible = internalVisible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        internalVisible = false
                    }
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .imePadding(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(25.dp)
                    ) {

                        if (addedBookmarkId.isEmpty()) {
                            Text(
                                text = "Add link",
                                modifier = Modifier.align(Alignment.CenterStart),
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    lineHeight = 25.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF0F1419)
                                )
                            )
                        }

                        Image(
                            painter = painterResource(Res.drawable.ic_xs_dialog_close),
                            contentDescription = "关闭",
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(24.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    internalVisible = false
                                }
                        )
                    }

                    if (addedBookmarkId.isEmpty()) {
                        Spacer(modifier = Modifier.height(25.dp))
                        BasicTextField(
                            state = inputState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .border(
                                    width = 1.dp,
                                    color = Color(0x140F1419),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .background(
                                    color = Color.White,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .focusRequester(focusRequester),
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF333333)
                            ),
                            lineLimits = TextFieldLineLimits.SingleLine,
                            cursorBrush = SolidColor(Color(0xFF16b998)),
                            onKeyboardAction = {
                                onConfirm()
                            },
                            decorator = { innerTextField ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    // Placeholder 提示文本
                                    if (inputState.text.isEmpty()) {
                                        Text(
                                            text = "https://…",
                                            style = TextStyle(
                                                fontSize = 16.sp,
                                                lineHeight = 24.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = Color(0xFF999999)
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        // 错误提示
                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage!!,
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFFE53935)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "The snapshot will be ready in a few minutes. In the mean time, you can read the original ariticle.",
                            modifier = Modifier.fillMaxWidth(),
                            style = TextStyle(
                                fontSize = 16.sp,
                                lineHeight = 22.5.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF333333)
                            )
                        )
                    }


                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(
                                color = Color(0xFF16b998),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (addedBookmarkId.isEmpty()) {
                                    onConfirm()
                                } else {
                                    onViewOriginalArticle(addedBookmarkId)
                                    internalVisible = false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (addedBookmarkId.isEmpty()) "添加收藏" else "View Original Article",
                            style = TextStyle(
                                fontSize = 16.sp,
                                lineHeight = 22.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}
