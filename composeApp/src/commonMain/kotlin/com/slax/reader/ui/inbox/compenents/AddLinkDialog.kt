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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.slax.reader.const.component.rememberDismissableVisibility
import com.slax.reader.ui.inbox.InboxListViewModel
import com.slax.reader.utils.getText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_xs_dialog_close

data class ShowErrorMessage(val message: String, val color: Color)

@Composable
fun AddLinkDialog(
    inboxView: InboxListViewModel,
    onDismissRequest: () -> Unit,
) {
    println("[watch][UI] recomposition AddLinkDialog")

    val inputState = remember { TextFieldState("") }
    val focusRequester = remember { FocusRequester() }
    val clipboard = LocalClipboard.current
    val (visible, dismiss) = rememberDismissableVisibility(
        scope = rememberCoroutineScope(),
        animationDuration = 300L,
        onDismissRequest = onDismissRequest
    )

    var errorMessage by remember { mutableStateOf<ShowErrorMessage?>(null) }
    val matchRegexp = remember {
        "(https?://)?(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)".toRegex()
    }

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()

        val clipEntry = clipboard.getClipEntry()
        val text = clipEntry?.getText()
        if (text == null || text.isEmpty()) return@LaunchedEffect

        val res = matchRegexp.findAll(text)
        if (res.count() == 0) {
            errorMessage = ShowErrorMessage("剪贴板中没有有效的链接", Color(0xfff3b336))
        } else if (res.count() > 1) {
            errorMessage = ShowErrorMessage("剪贴板发现多个链接，请手动处理", Color(0xfff3b336))
        } else {
            inputState.setTextAndPlaceCursorAtEnd(res.firstOrNull()!!.value)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { inputState.text }
            .collect {
                if (errorMessage != null) {
                    errorMessage = null
                }
            }
    }

    fun onConfirm() {
        val text = inputState.text.toString().trim()
        if (!text.startsWith("https://") && !text.startsWith("http://")) {
            errorMessage = ShowErrorMessage("请输入有效的网址链接", Color(0xFFE53935))
            return
        }
        inboxView.viewModelScope.launch {
            inboxView.addLinkBookmark(text)
            delay(100)
            inboxView.scrollToTop()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        DisposableEffect(Unit) {
            onDispose {
                dismiss()
            }
        }

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
                        dismiss()
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

                        Text(
                            text = "添加链接",
                            modifier = Modifier.align(Alignment.CenterStart),
                            style = TextStyle(
                                fontSize = 18.sp,
                                lineHeight = 25.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF0F1419)
                            )
                        )

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
                                    dismiss()
                                }
                        )
                    }

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
                            dismiss()
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
                            text = errorMessage!!.message,
                            style = TextStyle(
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Normal,
                                color = errorMessage!!.color
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val confirmButtonInteractionSource = remember { MutableInteractionSource() }
                    val isPressed by confirmButtonInteractionSource.collectIsPressedAsState()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(
                                color = if (isPressed) Color(0xFF14A68F) else Color(0xFF16b998),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable(
                                interactionSource = confirmButtonInteractionSource,
                                indication = null
                            ) {
                                onConfirm()
                                dismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "添加收藏",
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
