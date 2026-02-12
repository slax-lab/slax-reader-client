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
import com.slax.reader.const.InboxRoutes
import com.slax.reader.domain.auth.AppleSignInProvider
import com.slax.reader.domain.auth.GoogleSignInProvider
import com.slax.reader.utils.WebView
import com.slax.reader.utils.i18n
import com.slax.reader.utils.isIOS
import com.slax.reader.utils.rememberAppWebViewState
import com.slax.reader.utils.userEvent
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import slax_reader_client.composeapp.generated.resources.*
import kotlin.math.roundToInt

// 协议类型枚举
enum class AgreementType {
    TERMS,      // 用户协议
    PRIVACY     // 隐私政策
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavHostController) {
    val viewModel: LoginViewModel = koinInject()

    var isGoogleLoading by remember { mutableStateOf(false) }
    var isAppleLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var isAgreed by remember { mutableStateOf(false) }
    var agreementType by remember { mutableStateOf<AgreementType?>(null) }
    var pendingLoginAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val scope = rememberCoroutineScope()

    val successHandle: () -> Unit = {
        navController.navigate(InboxRoutes) {
            popUpTo(0) { inclusive = true }
        }
    }

    val googleProvider = remember { GoogleSignInProvider() }

    val withAgreementCheck: (AgreementType, () -> Unit) -> Unit = { type, action ->
        if (!isAgreed) {
            pendingLoginAction = action
            agreementType = type
        } else {
            action()
        }
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("login_failed".i18n()) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("btn_ok".i18n())
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // 顶部内容区域
        Column(
            modifier = Modifier.weight(1f, fill = true)
        ) {
            Spacer(modifier = Modifier.height(84.dp))

            Text(
                text = "login_welcome_title".i18n(),
                style = TextStyle(
                    fontSize = 27.sp,
                    lineHeight = 40.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F1419)
                ),
                modifier = Modifier.padding(start = 40.dp)
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
                    text = "login_welcome_subtitle".i18n(),
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

        Spacer(modifier = Modifier.height(16.dp))

        // 底部登录按钮区域
        Column(modifier = Modifier.padding(bottom = 30.dp)) {
            LoginButton(
                text = "login_google".i18n(),
                isLoading = isGoogleLoading,
                drawableResource = Res.drawable.ic_sm_google,
                onClick = {
                    withAgreementCheck(AgreementType.TERMS) {
                        scope.launch {
                            val result = googleProvider.signIn()
                            userEvent.action("login_start").method("google").send()
                            if (result.isFailure) {
                                val error = result.exceptionOrNull()
                                if (!error?.message.orEmpty().contains("cancel", ignoreCase = true)) {
                                    errorMessage = error?.message
                                }
                                return@launch
                            }
                            viewModel.googleSignIn(
                                result = result,
                                onSuccess = successHandle,
                                onLoading = { isGoogleLoading = it },
                                onError = { errorMessage = it }
                            )
                        }
                    }
                }
            )

            if (isIOS()) {
                val appleProvider = remember { AppleSignInProvider() }

                LoginButton(
                    modifier = Modifier.padding(top = 10.dp),
                    drawableResource = Res.drawable.ic_sm_apple,
                    isLoading = isAppleLoading,
                    text = "login_apple".i18n(),
                    onClick = {
                        withAgreementCheck(AgreementType.PRIVACY) {
                            scope.launch {
                                val result = appleProvider.signIn()
                                userEvent.action("login_start").method("apple").send()
                                if (result.isFailure) {
                                    val error = result.exceptionOrNull()
                                    if (!error?.message.orEmpty().contains("canceled", ignoreCase = true)) {
                                        errorMessage = error?.message
                                    }
                                    return@launch
                                }
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
                onPrivacyPolicyClick = {
                    agreementType = AgreementType.PRIVACY
                },
                onUserAgreementClick = {
                    agreementType = AgreementType.TERMS
                }
            )
        }
    }

    // 用户协议弹窗
    AgreementBottomSheet(
        visible = agreementType != null,
        initialType = agreementType,
        onDismiss = {
            agreementType = null
            pendingLoginAction = null
        },
        onAgree = {
            agreementType = null
            isAgreed = true
            pendingLoginAction?.invoke()
            pendingLoginAction = null
        },
        onDisagree = {
            agreementType = null
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
        append("agreement_prefix".i18n())

        pushStringAnnotation(
            tag = "user_agreement",
            annotation = "user_agreement"
        )
        withStyle(style = SpanStyle(color = Color(0xff5490C2))) {
            append("agreement_user_agreement".i18n())
        }
        pop()

        append("agreement_and".i18n())

        pushStringAnnotation(
            tag = "privacy_policy",
            annotation = "privacy_policy"
        )
        withStyle(style = SpanStyle(color = Color(0xff5490C2))) {
            append("agreement_privacy_policy".i18n())
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
    initialType: AgreementType?,
    onDismiss: () -> Unit,
    onAgree: () -> Unit,
    onDisagree: () -> Unit
) {
    // 0 = 用户协议，1 = 隐私政策
    var selectedTabIndex by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val webState = rememberAppWebViewState(scope)

    LaunchedEffect(visible, initialType) {
        if (visible && initialType != null) {
            selectedTabIndex = when (initialType) {
                AgreementType.TERMS -> 0
                AgreementType.PRIVACY -> 1
            }
        }
    }

    val currentUrl = remember(selectedTabIndex) {
        if (selectedTabIndex == 0) {
            "${SlaxConfig.WEB_BASE_URL}/terms"
        } else {
            "${SlaxConfig.WEB_BASE_URL}/privacy"
        }
    }

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "agreement_tab_terms".i18n(),
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = if (selectedTabIndex == 0) FontWeight.SemiBold else FontWeight.Normal,
                        lineHeight = 20.sp,
                        color = if (selectedTabIndex == 0) Color(0xFF0F1419) else Color(0xFF999999)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            selectedTabIndex = 0
                        }
                        .padding(vertical = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Text(
                    text = "agreement_tab_privacy".i18n(),
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = if (selectedTabIndex == 1) FontWeight.SemiBold else FontWeight.Normal,
                        lineHeight = 20.sp,
                        color = if (selectedTabIndex == 1) Color(0xFF0F1419) else Color(0xFF999999)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            selectedTabIndex = 1
                        }
                        .padding(vertical = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(1.dp)
                    .background(Color(0x14333333))
            )

            WebView(
                url = currentUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction = 0.7F),
                webState = webState
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
                    "agreement_btn_agree".i18n(),
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
                    "agreement_btn_disagree".i18n(),
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
