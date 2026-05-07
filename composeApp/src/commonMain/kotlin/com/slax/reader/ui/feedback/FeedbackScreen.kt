package com.slax.reader.ui.feedback

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.slax.reader.SlaxConfig
import com.slax.reader.data.network.ApiService
import com.slax.reader.data.network.dto.FeedbackParams
import com.slax.reader.utils.feedbackEvent
import com.slax.reader.utils.i18n
import com.slax.reader.utils.platformName
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_agree_disable
import slax_reader_client.composeapp.generated.resources.ic_agree_enable
import slax_reader_client.composeapp.generated.resources.ic_sm_back

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    title: String? = null,
    href: String? = null,
    email: String? = null,
    bookmarkId: String? = null,
    entryPoint: String? = null,
    version: String? = null,
    onBackClick: () -> Unit
) {
    val apiService: ApiService = koinInject()
    val scope = rememberCoroutineScope()

    var feedbackText by remember { mutableStateOf("") }
    var allowFollowUp by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showResultDialog by remember { mutableStateOf<FeedbackResult?>(null) }

    val isSubmitEnabled = feedbackText.trim().isNotEmpty() && !isSubmitting
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "feedback_title".i18n(),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 24.sp,
                        color = Color(0xFF0F1419)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_sm_back),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (!isSubmitEnabled) return@TextButton
                            isSubmitting = true
                            val environment = "$platformName ${SlaxConfig.APP_VERSION_CODE}"
                            val params = FeedbackParams(
                                bookmark_uuid = bookmarkId ?: "",
                                entry_point = entryPoint ?: "",
                                type = "parse_error",
                                content = feedbackText.trim(),
                                platform = "app",
                                environment = environment,
                                version = version ?: "${SlaxConfig.APP_VERSION_NAME} (${SlaxConfig.APP_VERSION_CODE})",
                                target_url = href ?: "",
                                allow_follow_up = allowFollowUp
                            )
                            scope.launch {
                                try {
                                    apiService.sendFeedback(params)
                                    feedbackEvent.action("submit_start").param("scope", "app").send()
                                    showResultDialog = FeedbackResult.Success
                                } catch (e: Exception) {
                                    showResultDialog = FeedbackResult.Error(e.message ?: "")
                                } finally {
                                    isSubmitting = false
                                }
                            }
                        },
                        enabled = isSubmitEnabled
                    ) {
                        Text(
                            text = if (isSubmitting) "feedback_submitting".i18n() else "feedback_submit".i18n(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSubmitEnabled) Color(0xFF16B998) else Color(0xFF999999)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF5F5F3)
                )
            )
        },
        containerColor = Color(0xFFF5F5F3),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .pointerInput(Unit) {
                    detectTapGestures { focusManager.clearFocus() }
                }
        ) {
            if (!title.isNullOrEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(top = 20.dp, bottom = 20.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 22.sp,
                        color = Color(0xFF0F1419),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!href.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = href,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color(0xFF5490C2),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .background(
                        color = Color(0xFFFCFCFC),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0x0F0F1419),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                BasicTextField(
                    value = feedbackText,
                    onValueChange = { feedbackText = it },
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        lineHeight = 22.5.sp,
                        color = Color(0xFF333333)
                    ),
                    modifier = Modifier.fillMaxSize(),
                    decorationBox = { innerTextField ->
                        Box {
                            if (feedbackText.isEmpty()) {
                                Text(
                                    text = "feedback_placeholder".i18n(),
                                    style = TextStyle(
                                        fontSize = 15.sp,
                                        lineHeight = 22.5.sp,
                                        color = Color(0x4D000000)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { allowFollowUp = !allowFollowUp }
                    .padding(20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(
                        if (allowFollowUp) Res.drawable.ic_agree_enable
                        else Res.drawable.ic_agree_disable
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "feedback_allow_follow_up".i18n(mapOf("email" to (email ?: ""))),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Color(0xCC333333)
                )
            }
        }
    }

    showResultDialog?.let { result ->
        AlertDialog(
            onDismissRequest = {
                showResultDialog = null
                if (result is FeedbackResult.Success) onBackClick()
            },
            title = {
                Text(
                    text = when (result) {
                        is FeedbackResult.Success -> "feedback_success_title".i18n()
                        is FeedbackResult.Error -> "feedback_error_title".i18n()
                    }
                )
            },
            text = {
                Text(
                    text = when (result) {
                        is FeedbackResult.Success -> "feedback_success_message".i18n()
                        is FeedbackResult.Error -> "${"feedback_error_message".i18n()} ${result.message}"
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showResultDialog = null
                    if (result is FeedbackResult.Success) onBackClick()
                }) {
                    Text("btn_ok".i18n())
                }
            }
        )
    }
}

private sealed interface FeedbackResult {
    data object Success : FeedbackResult
    data class Error(val message: String) : FeedbackResult
}
