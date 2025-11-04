package com.slax.reader.ui.inbox.compenents

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slax.reader.data.database.model.InboxListBookmarkItem
import com.slax.reader.ui.inbox.InboxListViewModel
import kotlinx.coroutines.launch

@Composable
fun TitleEditOverlay(
    bookmark: InboxListBookmarkItem,
    viewModel: InboxListViewModel,
    onDismiss: () -> Unit,
) {
    println("[watch][UI] recomposition TitleEditOverlay")

    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val textState = rememberTextFieldState(bookmark.displayTitle())

    // 半透明背景的动画
    val backgroundAlpha = remember { Animatable(0f) }
    val slideOffset = remember { Animatable(-150f) }

    LaunchedEffect(Unit) {
        backgroundAlpha.animateTo(1f, animationSpec = tween(durationMillis = 300))
        slideOffset.animateTo(0f, animationSpec = tween(durationMillis = 300))
        focusRequester.requestFocus()
    }

    val handleConfirm: () -> Unit = {
        scope.launch {
            launch {
                slideOffset.animateTo(-150f, animationSpec = tween(durationMillis = 300))
            }
            launch {
                backgroundAlpha.animateTo(0f, animationSpec = tween(durationMillis = 300))
            }.join()

            val trimmed = textState.text.trim().toString()
            if (trimmed.isNotEmpty() && trimmed != bookmark.displayTitle()) {
                viewModel.confirmEditTitle(bookmark.id, trimmed)
            }
            onDismiss()
        }
    }

    val handleDismiss: () -> Unit = {
        scope.launch {
            launch {
                slideOffset.animateTo(-150f, animationSpec = tween(durationMillis = 300))
            }
            launch {
                backgroundAlpha.animateTo(0f, animationSpec = tween(durationMillis = 300))
            }.join()
            onDismiss()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = backgroundAlpha.value }
                .background(Color(0xF2FCFCFC))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    handleDismiss()
                }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = slideOffset.value.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                    clip = false
                )
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                )
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            BasicTextField(
                state = textState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .focusRequester(focusRequester),
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    color = Color(0xFF0F1419)
                ),
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                onKeyboardAction = {
                    handleConfirm()
                }
            )
        }
    }
}
