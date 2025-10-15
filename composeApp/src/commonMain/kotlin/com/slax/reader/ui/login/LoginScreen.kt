package com.slax.reader.ui.login

// Compose Animation
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

// Compose Foundation
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText

// Compose Material3
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

// Compose Runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// Compose UI
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// WebView
import com.multiplatform.webview.setting.PlatformWebSettings
import com.multiplatform.webview.util.KLogSeverity
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import com.slax.reader.ui.bookmark.optimizedHtml

// Resources
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.bg_login
import slax_reader_client.composeapp.generated.resources.ic_sm_apple
import slax_reader_client.composeapp.generated.resources.ic_sm_google
import slax_reader_client.composeapp.generated.resources.ic_radio_disabled
import slax_reader_client.composeapp.generated.resources.ic_radio_enabled

// Kotlin
import kotlin.math.roundToInt

// ================================================================================================
// 主屏幕组件
// ================================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen() {
    var isAgreed by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 顶部内容区域
        Column {
            Text(
                text = "欢迎来到 \nSlax Reader",
                style = TextStyle(
                    fontSize = 27.sp,
                    lineHeight = 40.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F1419)
                ),
                modifier = Modifier.padding(start = 40.dp, top = 84.dp)
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(Res.drawable.bg_login),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(375f / 314f),
                    contentScale = ContentScale.Fit
                )

                Text(
                    text = "Read Smarter\nConnect Deeper",
                    style = TextStyle(
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xCC333333)
                    ),
                    modifier = Modifier.padding(start = 40.dp, top = 24.dp)
                )
            }
        }

        // 底部登录按钮区域
        Column(modifier = Modifier.padding(bottom = 30.dp)) {
            LoginButton(
                text = "Google 登录",
                drawableResource = Res.drawable.ic_sm_google,
                onClick = { }
            )

            LoginButton(
                modifier = Modifier.padding(top = 10.dp),
                text = "通过 Apple 登录",
                drawableResource = Res.drawable.ic_sm_apple,
                onClick = { }
            )

            AgreementText(
                agreed = isAgreed,
                modifier = Modifier.padding(top = 30.dp),
                onAgreedClick = { isAgreed = !it },
                onPrivacyPolicyClick = { showBottomSheet = true },
                onUserAgreementClick = { showBottomSheet = true }
            )
        }
    }

    // 用户协议弹窗
    AgreementBottomSheet(
        visible = showBottomSheet,
        onDismiss = { showBottomSheet = false },
        onAgree = {
            showBottomSheet = false
            isAgreed = true
        },
        onDisagree = {
            showBottomSheet = false
            isAgreed = false
        }
    )
}

// ================================================================================================
// UI组件 - 登录按钮
// ================================================================================================

@Composable
private fun LoginButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalMargin: Dp = 40.dp,
    maxWidth: Dp = 295.dp,
    drawableResource: DrawableResource? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalMargin)
            .height(50.dp)
            .widthIn(max = maxWidth)
    ) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(25.dp),
            border = BorderStroke(0.5.dp, Color(0x336A6E83)),
            contentPadding = PaddingValues(),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                drawableResource?.let {
                    Icon(
                        painter = painterResource(it),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text,
                    style = TextStyle(
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF0F1419)
                    ),
                    modifier = Modifier.padding(start = if (drawableResource != null) 10.dp else 0.dp)
                )
            }
        }
    }
}

// ================================================================================================
// UI组件 - 用户协议
// ================================================================================================

@Composable
private fun AgreementText(
    agreed: Boolean,
    modifier: Modifier = Modifier,
    onAgreedClick: (agree: Boolean) -> Unit,
    onUserAgreementClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit
) {
    val annotatedString = buildAnnotatedString {
        append("阅读并同意")

        pushStringAnnotation(
            tag = "user_agreement",
            annotation = "user_agreement"
        )
        withStyle(style = SpanStyle(color = Color(0xff5490C2))) {
            append("《用户协议》")
        }
        pop()

        append("和")

        pushStringAnnotation(
            tag = "privacy_policy",
            annotation = "privacy_policy"
        )
        withStyle(style = SpanStyle(color = Color(0xff5490C2))) {
            append("《隐私政策》")
        }
        pop()
    }

    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        IconButton(
            onClick = { onAgreedClick(agreed) },
            modifier = Modifier.size(12.dp)
        ) {
            Icon(
                painter = painterResource(
                    if (agreed) Res.drawable.ic_radio_enabled
                    else Res.drawable.ic_radio_disabled
                ),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(12.dp)
            )
        }

        BasicText(
            text = annotatedString,
            style = TextStyle(
                lineHeight = 16.5.sp,
                fontSize = 12.sp,
                color = Color(0xCC333333)
            ),
            onTextLayout = { layoutResult.value = it },
            modifier = Modifier
                .padding(start = 6.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        layoutResult.value?.let { layout ->
                            val position = layout.getOffsetForPosition(offset)

                            annotatedString.getStringAnnotations(
                                tag = "user_agreement",
                                start = position,
                                end = position
                            ).firstOrNull()?.let {
                                onUserAgreementClick()
                            }

                            annotatedString.getStringAnnotations(
                                tag = "privacy_policy",
                                start = position,
                                end = position
                            ).firstOrNull()?.let {
                                onPrivacyPolicyClick()
                            }
                        }
                    }
                }
        )
    }
}

// ================================================================================================
// UI组件 - 底部弹窗
// ================================================================================================

@Composable
private fun AgreementBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onAgree: () -> Unit,
    onDisagree: () -> Unit
) {
    CustomModalBottomSheet(
        visible = visible,
        onDismissRequest = onDismiss,
        enableDrag = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "用户协议与隐私政策",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 20.sp,
                    color = Color(0xFF0F1419)
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .height(1.dp)
                    .background(Color(0x14333333))
            )

            // WebView 内容
            val webState = rememberWebViewStateWithHTMLData(optimizedHtml)
            webState.webSettings.apply {
                isJavaScriptEnabled = true
                supportZoom = false
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false
                logSeverity = KLogSeverity.Error

                androidWebSettings.apply {
                    domStorageEnabled = true
                    safeBrowsingEnabled = true
                    allowFileAccess = false
                    layerType = PlatformWebSettings.AndroidWebSettings.LayerType.HARDWARE
                }
            }

            WebView(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .heightIn(max = 470.dp),
                state = webState
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0x14333333))
            )

            Button(
                onClick = onAgree,
                shape = RoundedCornerShape(25.dp),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xff16B998)
                )
            ) {
                Text(
                    "同意",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 16.sp,
                        lineHeight = 22.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Button(
                onClick = onDisagree,
                shape = RoundedCornerShape(25.dp),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent
                )
            ) {
                Text(
                    "不同意",
                    style = TextStyle(
                        color = Color(0xff999999),
                        fontSize = 15.sp,
                        lineHeight = 21.sp
                    )
                )
            }
        }
    }
}

/**
 * 通用底部弹窗组件
 */
@Composable
private fun CustomModalBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    enableDrag: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val density = LocalDensity.current
    var offsetY by remember { mutableStateOf(0f) }

    // 背景淡入淡出
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
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
    }

    // 内容从底部滑入
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(300)
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300)
        )
    ) {
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
                            Modifier.draggable(
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
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {}
                        }
                    ),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(0.dp),
                    content = content
                )
            }
        }
    }
}