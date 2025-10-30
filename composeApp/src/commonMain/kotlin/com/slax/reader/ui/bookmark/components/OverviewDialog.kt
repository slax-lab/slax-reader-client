package com.slax.reader.ui.bookmark.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slax.reader.ui.bookmark.OverviewViewBounds
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_xs_blue_down_arrow
import kotlin.math.roundToInt

/**
 * Overview 弹窗组件
 */
@Composable
fun OverviewDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    sourceBounds: OverviewViewBounds,
    overview: String = "",
    keyTakeaways: List<String> = emptyList()
) {
    println("[watch][UI] recomposition OverviewDialog")

    val density = LocalDensity.current

    var screenWidth by remember { mutableStateOf(0f) }
    var screenHeight by remember { mutableStateOf(0f) }
    var contentHeight by remember { mutableStateOf(0f) }

    // 内部动画触发状态
    var internalVisible by remember { mutableStateOf(false) }

    // 延迟触发动画，确保组件先添加到组合树再开始动画
    LaunchedEffect(visible) {
        internalVisible = visible
    }

    val animationProgress by animateFloatAsState(
        targetValue = if (internalVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "dialogAnimation"
    )

    val alpha by animateFloatAsState(
        targetValue = if (internalVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "alphaAnimation"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    screenWidth = coordinates.size.width.toFloat()
                    screenHeight = coordinates.size.height.toFloat()
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCCF5F5F3))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onDismissRequest()
                    }
            )

            val targetWidth = with(density) { (screenWidth - 24.dp.toPx()) }
            val maxDialogHeight = screenHeight * 0.8f
            val targetHeight = if (contentHeight > 0f) {
                minOf(contentHeight, maxDialogHeight)
            } else {
                maxDialogHeight
            }

            val currentWidth = sourceBounds.width + (targetWidth - sourceBounds.width) * animationProgress
            val currentHeight = sourceBounds.height + (targetHeight - sourceBounds.height) * animationProgress

            val targetX = (screenWidth - targetWidth) / 2f
            val targetY = (screenHeight - targetHeight) / 2f

            val currentX = sourceBounds.x + (targetX - sourceBounds.x) * animationProgress
            val currentY = sourceBounds.y + (targetY - sourceBounds.y) * animationProgress

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Surface(
                    modifier = Modifier
                        .width(with(density) { currentWidth.toDp() })
                        .height(with(density) { currentHeight.toDp() })
                        .absoluteOffset {
                            IntOffset(
                                x = currentX.roundToInt(),
                                y = currentY.roundToInt()
                            )
                        }
                        .graphicsLayer {
                            this.alpha = alpha
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                ) {

                    val paddingTopValue = 24.dp
                    val buttonHeight = 45.dp
                    Column(
                        modifier = Modifier
                            .padding(top = paddingTopValue)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .onGloballyPositioned { coordinates ->
                                    val measuredHeight = coordinates.size.height.toFloat()
                                    if (measuredHeight > 0) {
                                        val paddingPx = with(density) { (paddingTopValue + buttonHeight).toPx() }
                                        contentHeight = measuredHeight + paddingPx
                                    }
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (overview.isNotEmpty()) {
                                val annotatedText = remember(overview) {
                                    buildAnnotatedString {
                                        // 灰色前缀
                                        withStyle(style = SpanStyle(color = Color(0xFF999999))) {
                                            append("全文概要: ")
                                        }
                                        append(overview)
                                    }
                                }

                                Text(
                                    annotatedText,
                                    modifier = Modifier.fillMaxWidth(),
                                    style = TextStyle(
                                        fontSize = 15.sp,
                                        lineHeight = 24.sp,
                                        color = Color(0xFF333333)
                                    )
                                )
                            }

                            if (keyTakeaways.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))

                                keyTakeaways.forEachIndexed { index, takeaway ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(IntrinsicSize.Min),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(6.dp)
                                                .fillMaxHeight()
                                        ) {

                                            val lineColor = Color(0x14333333)
                                            if (index > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopCenter)
                                                        .width(1.dp)
                                                        .fillMaxHeight(0.5f)
                                                        .background(lineColor)
                                                )
                                            }

                                            if (index < keyTakeaways.size - 1) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomCenter)
                                                        .width(1.dp)
                                                        .fillMaxHeight(0.5f)
                                                        .background(lineColor)
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.Center)
                                                    .size(6.dp)
                                                    .background(Color(0xCCF5F5F3))
                                                    .border(1.26.dp, Color(0xFFD3D3D3), CircleShape)
                                            )
                                        }

                                        // 右侧：文本内容
                                        Text(
                                            text = takeaway,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(start = 12.dp)
                                                .padding(vertical = 6.dp),
                                            style = TextStyle(
                                                fontSize = 14.sp,
                                                lineHeight = 20.sp,
                                                color = Color(0xCC333333)
                                            )
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .padding(top = 24.dp)
                                    .height(0.5.dp)
                                    .background(Color(0x14333333))
                                    .fillMaxWidth()
                            )
                        }

                        Surface(
                            onClick = {
                                onDismissRequest()
                            },
                            color = Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(buttonHeight),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "收起",
                                        style = TextStyle(
                                            fontSize = 12.sp,
                                            lineHeight = 16.5.sp,
                                            color = Color(0xFF5490C2)
                                        )
                                    )
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_xs_blue_down_arrow),
                                        contentDescription = null,
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(8.dp).graphicsLayer { rotationZ = 180f }
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
