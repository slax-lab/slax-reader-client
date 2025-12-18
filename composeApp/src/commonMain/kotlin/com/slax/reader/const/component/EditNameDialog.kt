package com.slax.reader.const.component

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
import com.slax.reader.utils.i18n
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_xs_dialog_close

@Composable
fun EditNameDialog(
    initialTitle: String,
    onConfirm: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val inputState = remember { TextFieldState(initialTitle) }
    val focusRequester = remember { FocusRequester() }

    val (visible, dismiss) = rememberDismissableVisibility(
        scope = rememberCoroutineScope(),
        animationDuration = 300L,
        onDismissRequest = onDismissRequest
    )

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }

    AnimatedVisibility(
        visible = visible,
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
                            text = "edit_title_dialog_title".i18n(),
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
                            contentDescription = "btn_close".i18n(),
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
                            val text = inputState.text.toString().trim()
                            if (text.isNotEmpty()) {
                                onConfirm(text)
                                dismiss()
                            }
                        },
                        decorator = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                innerTextField()
                            }
                        }
                    )

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
                                val text = inputState.text.toString().trim()
                                if (text.isNotEmpty()) {
                                    onConfirm(text)
                                    dismiss()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "btn_confirm".i18n(),
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
