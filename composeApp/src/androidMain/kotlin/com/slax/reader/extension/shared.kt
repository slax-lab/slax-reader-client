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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class ShareActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置全屏沉浸式
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        setContent {
            MaterialTheme {
                SharePopup(
                    onDismiss = { finish() },
                    onProcess = { processShare() }
                )
            }
        }
    }

    private suspend fun processShare(): Boolean {
        val text = when (intent?.action) {
            Intent.ACTION_SEND -> {
                when {
                    intent.type?.startsWith("text/") == true -> {
                        intent.getStringExtra(Intent.EXTRA_TEXT)
                    }

                    else -> null
                }
            }

            else -> null
        }

        return if (text != null) {
            collectionShare()
        } else {
            false
        }
    }
}

@Composable
fun SharePopup(
    onDismiss: () -> Unit,
    onProcess: suspend () -> Boolean
) {
    var state by remember { mutableStateOf(0) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
        delay(500)
        val success = onProcess()
        state = if (success) 1 else 2
        delay(2000)
        isVisible = false
        delay(300)
        onDismiss()
    }

    if (isVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { onDismiss() }
        ) {
            // 底部弹窗
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
                Card(
                    shape = RoundedCornerShape(
                        topStart = 24.dp,
                        topEnd = 24.dp,
                        bottomStart = 0.dp,
                        bottomEnd = 0.dp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = false) { } // 阻止点击穿透
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
                            0 -> {
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

                            1 -> {
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

                            2 -> {
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
                                            "✗",
                                            style = MaterialTheme.typography.headlineLarge,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text(
                                        text = getShareLabelText("failed"),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color(0xFFF44336)
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