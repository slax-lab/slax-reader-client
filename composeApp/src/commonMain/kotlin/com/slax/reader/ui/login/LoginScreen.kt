package com.slax.reader.ui.login

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import app.slax.reader.SlaxConfig
import com.mmk.kmpauth.google.GoogleAuthCredentials
import com.mmk.kmpauth.google.GoogleAuthProvider
import com.mmk.kmpauth.google.GoogleButtonUiContainer
import com.slax.reader.const.InboxRoutes
import com.slax.reader.domain.auth.AppleSignInProvider
import com.slax.reader.utils.WebView
import com.slax.reader.utils.platformType
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import slax_reader_client.composeapp.generated.resources.*
import kotlin.math.roundToInt

// ================================================================================================
// 登录页面
// ================================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavHostController) {
    val viewModel: LoginViewModel = koinInject()

    var isGoogleLoading by remember { mutableStateOf(false) }
    var isAppleLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var isAgreed by remember { mutableStateOf(false) }
    var showAgreement by remember { mutableStateOf(false) }
    var pendingLoginAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val scope = rememberCoroutineScope()

    val successHandle: () -> Unit = {
        navController.navigate(InboxRoutes) {
            popUpTo(0) { inclusive = true }
        }
    }

    GoogleAuthProvider.create(
        credentials = GoogleAuthCredentials(serverId = SlaxConfig.GOOGLE_AUTH_SERVER_ID)
    )

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Login Failed") },
            text = { Text(errorMessage!!) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).windowInsetsPadding(WindowInsets.navigationBars),
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
            GoogleButtonUiContainer(onGoogleSignInResult = { googleUser ->
                scope.launch {
                    viewModel.googleSignIn(
                        googleUser,
                        onSuccess = successHandle,
                        onLoading = { isGoogleLoading = it },
                        onError = {
                            errorMessage = it
                        },
                    )
                }
            }) {
                LoginButton(
                    text = "Google 登录",
                    isLoading = isGoogleLoading,
                    drawableResource = Res.drawable.ic_sm_google,
                    onClick = {
                        if (!isAgreed) {
                            pendingLoginAction = { this.onClick() }
                            showAgreement = true
                        } else {
                            this.onClick()
                        }
                    }
                )
            }

            if (platformType == "ios") {
                val appleProvider = remember { AppleSignInProvider() }

                LoginButton(
                    modifier = Modifier.padding(top = 10.dp),
                    drawableResource = Res.drawable.ic_sm_apple,
                    isLoading = isAppleLoading,
                    text = "通过 Apple 登录",
                    onClick = {
                        if (!isAgreed) {
                            pendingLoginAction = {
                                scope.launch {
                                    val result = appleProvider.signIn()
                                    viewModel.appleSignIn(
                                        result = result,
                                        onSuccess = successHandle,
                                        onLoading = { isAppleLoading = it },
                                        onError = { errorMessage = it }
                                    )
                                }
                            }
                            showAgreement = true
                        } else {
                            scope.launch {
                                val result = appleProvider.signIn()
                                viewModel.appleSignIn(
                                    result = result,
                                    onSuccess = successHandle,
                                    onLoading = { isAppleLoading = it },
                                    onError = { errorMessage = it }
                                )
                            }
                        }
                    }
                )
            }

            AgreementText(
                agreed = isAgreed,
                modifier = Modifier.padding(top = 30.dp),
                onAgreedClick = { isAgreed = !it },
                onPrivacyPolicyClick = { showAgreement = true },
                onUserAgreementClick = { showAgreement = true }
            )
        }
    }

    // 用户协议弹窗
    AgreementBottomSheet(
        visible = showAgreement,
        onDismiss = {
            showAgreement = false
            pendingLoginAction = null
        },
        onAgree = {
            showAgreement = false
            isAgreed = true
            pendingLoginAction?.invoke()
            pendingLoginAction = null
        },
        onDisagree = {
            showAgreement = false
            isAgreed = false
            pendingLoginAction = null
        }
    )
}

// ================================================================================================
// 登录按钮
// ================================================================================================

@Composable
private fun LoginButton(
    text: String,
    isLoading: Boolean = false,
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
            enabled = !isLoading,
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
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0x336A6E83)
                    )

                    return@Row
                }

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
// 用户协议和隐私政策文案
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
// 隐私政策与用户协议
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
        enableDrag = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(fraction = 0.8F)
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "用户协议与隐私政策",
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

            WebView(
                url = "https://slax.com/blog.html",
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction = 0.7F)
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

            Box(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
            )
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
