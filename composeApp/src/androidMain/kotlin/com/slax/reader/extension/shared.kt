package com.slax.reader.extension

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class ShareActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        setContent {
            MaterialTheme {
                SharePopup(
                    onDismiss = { finish() },
                )
            }
        }
    }

    private suspend fun processShare(): String {
        var text: String?
        var subject: String?

        return if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            text = intent.getStringExtra(Intent.EXTRA_TEXT)
            subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
            return if (!text.isNullOrEmpty()) {
                collectionShare(text, subject, null)
            } else {
                "Not have share content"
            }
        } else {
            "Not support content"
        }
    }

    @Composable
    fun SharePopupProcessing() {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(56.dp),
                strokeWidth = 5.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = getShareLabelText("collecting"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    @Composable
    fun SharePopupSuccess() {
        val scale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = ""
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        Color(0xFF4CAF50),
                        CircleShape
                    )
                    .scale(scale),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "✓",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = getShareLabelText("success"),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF4CAF50)
            )
        }
    }

    @Composable
    fun SharePopupFailed(state: String) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        Color(0xFFF44336),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "X",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = state,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFF44336)
            )
        }
    }

    @Composable
    fun SharePopupCard(state: String) {
        Card(
            shape = RoundedCornerShape(
                topStart = 24.dp,
                topEnd = 24.dp,
                bottomStart = 0.dp,
                bottomEnd = 0.dp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false) { }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 32.dp,
                        bottom = 48.dp,
                        start = 24.dp,
                        end = 24.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (state) {
                    "" -> {
                        // 处理中
                        SharePopupProcessing()
                    }

                    "ok" -> {
                        // 成功
                        SharePopupSuccess()
                    }

                    else -> {
                        // 失败
                        SharePopupFailed(state)
                    }
                }
            }
        }
    }

    @Composable
    fun SharePopup(
        onDismiss: () -> Unit,
    ) {
        val haptics = LocalHapticFeedback.current

        var state by remember { mutableStateOf("") }
        var isVisible by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            isVisible = true

            state = processShare()
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)

            if (state == "ok") {
                delay(2000)
                isVisible = false
                delay(100)
                onDismiss()
            }
        }

        if (isVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0f))
                    .clickable { onDismiss() }
            ) {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(250)
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    SharePopupCard(state)
                }
            }
        }
    }
}