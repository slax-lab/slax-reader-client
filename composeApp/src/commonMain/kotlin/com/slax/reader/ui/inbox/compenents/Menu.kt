package com.slax.reader.ui.inbox.compenents

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay

@Composable
fun Menu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    offset: DpOffset,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    println("[watch][UI] recomposition Menu")

    var showPopup by remember { mutableStateOf(false) }
    var animateIn by remember { mutableStateOf(false) }

    LaunchedEffect(expanded) {
        if (expanded) {
            showPopup = true
            delay(50)
            animateIn = true
        } else {
            animateIn = false
            delay(200)
            showPopup = false
        }
    }

    if (showPopup) {
        val density = LocalDensity.current

        Popup(
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(
                focusable = true,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            ),
            offset = IntOffset(
                x = with(density) { offset.x.roundToPx() },
                y = with(density) { offset.y.roundToPx() }
            )
        ) {
            AnimatedVisibility(
                visible = animateIn,
                enter = fadeIn(animationSpec = tween(durationMillis = 200)) +
                        scaleIn(
                            initialScale = 0.92f,
                            animationSpec = tween(durationMillis = 200)
                        ),
                exit = fadeOut(animationSpec = tween(durationMillis = 150)) +
                        scaleOut(
                            targetScale = 0.92f,
                            animationSpec = tween(durationMillis = 150)
                        )
            ) {
                Box(
                    modifier = modifier
                        .dropShadow(
                            shape = RoundedCornerShape(20.dp),
                            shadow = Shadow(
                                radius = 10.dp,
                                spread = 0.dp,
                                color = Color(0x14000000),
                                offset = DpOffset(x = 0.dp, 5.dp)
                            )
                        )
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .blur(radius = 100.dp)
                            .background(
                                color = Color(0xF0F5F5F3),
                                shape = RoundedCornerShape(20.dp)
                            )
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .border(
                                width = 1.dp,
                                color = Color.White,
                                shape = RoundedCornerShape(20.dp)
                            )
                    )

                    Column(
                        modifier = Modifier
                            .wrapContentSize()
                            .padding(horizontal = 8.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun MenuItem(
    icon: Painter,
    text: String,
    color: Color = Color(0xFF0F1419),
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(46.5.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isPressed) Color(0x14000000) else Color.Transparent
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Image(
                painter = icon,
                contentDescription = text,
                modifier = Modifier.size(20.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = text,
                color = color,
                fontSize = 16.sp,
                lineHeight = 22.5.sp,
                style = TextStyle(
                    fontWeight = FontWeight.Normal
                )
            )
        }
    }
}