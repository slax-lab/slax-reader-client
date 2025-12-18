package com.slax.reader.ui.setting

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.slax.reader.SlaxConfig
import com.slax.reader.domain.auth.AuthDomain
import com.slax.reader.utils.WebView
import com.slax.reader.utils.i18n
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_sm_back

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAccountScreen(onBackClick: () -> Unit) {
    val viewModel: SettingViewModel = koinInject()
    val authDomain: AuthDomain = koinInject()
    val deleteAccountState by viewModel.deleteAccountState.collectAsState()

    val scope = rememberCoroutineScope()

    // 状态管理
    var hasScrolledToBottom by remember { mutableStateOf(false) }
    var hasScrolled by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "delete_account_title".i18n(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF0F1419)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_sm_back),
                            contentDescription = "btn_back".i18n(),
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF5F5F3)
                )
            )
        },
        containerColor = Color(0xFFF5F5F3),
        bottomBar = {
            // 底部按钮区域
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 30.dp, horizontal = 24.dp)
            ) {
                Button(
                    onClick = {
                        showConfirmDialog = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    enabled = hasScrolled && hasScrolledToBottom,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFFF45454),
                        disabledContainerColor = Color(0xFFE0E0E0),
                        disabledContentColor = Color(0xFF999999)
                    )
                ) {
                    Text(
                        text = "delete_account_button".i18n(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    ) { paddingValues ->
        WebView(
            url = "${SlaxConfig.WEB_BASE_URL}/delete-account-notice",
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            onScroll = { _, scrollY, contentHeight, visibleHeight ->
                // 标记已经开始滚动（认为WebView已加载完成）
                if (!hasScrolled) {
                    hasScrolled = true
                }

                // 判断是否曾经滚动到底部（一旦激活就永久保持激活）
                if (!hasScrolledToBottom) {
                    // 确保内容高度和可见高度都已经正确初始化（大于0）
                    if (contentHeight > 0 && visibleHeight > 0) {
                        // 如果内容高度小于等于可见高度，说明不需要滚动，直接激活按钮
                        if (contentHeight <= visibleHeight) {
                            hasScrolledToBottom = true
                        } else {
                            // 需要滚动的情况，判断当前是否在底部
                            // 容错值设置为5dp（考虑iOS弹性滚动等边界情况）
                            val threshold = 5.0
                            val isAtBottom = scrollY + visibleHeight >= contentHeight - threshold
                            if (isAtBottom) {
                                hasScrolledToBottom = true
                            }
                        }
                    }
                }
            }
        )
    }

    // 二次确认对话框
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "delete_account_confirm_title".i18n(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = "delete_account_confirm_message".i18n(),
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        scope.launch {
                            viewModel.deleteAccount()
                        }
                    },
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFFF45454),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "delete_account_confirm_button".i18n(),
                            color = Color(0xFFF45454),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmDialog = false }
                ) {
                    Text(
                        text = "btn_cancel".i18n(),
                        color = Color(0xFF333333),
                        fontSize = 16.sp
                    )
                }
            }
        )
    }

    when (val state = deleteAccountState) {
        is DeleteAccountState.Loading -> {
            isDeleting = true
        }
        is DeleteAccountState.Error -> {
            isDeleting = false
            val error = state.message
            AlertDialog(
                onDismissRequest = { },
                containerColor = Color.White,
                title = {
                    Text(
                        text = "delete_account_error_title".i18n(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Text(
                        text = error,
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.resetState()
                        }
                    ) {
                        Text(
                            text = "btn_ok".i18n(),
                            color = Color(0xFF333333),
                            fontSize = 16.sp
                        )
                    }
                }
            )
        }
        is DeleteAccountState.Success -> {
            isDeleting = false
            authDomain.signOut()
        }
        else -> {}
    }
}