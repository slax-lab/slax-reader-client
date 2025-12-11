package com.slax.reader.ui.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.runBlocking
import org.koin.compose.koinInject

@Composable
fun SubscriptionManagerScreen() {
    val viewmodel: SubscriptionViewModel = koinInject()
    val paymentState by viewmodel.paymentState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).windowInsetsPadding(WindowInsets.navigationBars),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(375f / 128f)
            )

            Text(
                text = "欢迎来到 \nSlax Reader",
                style = TextStyle(
                    fontSize = 27.sp,
                    lineHeight = 40.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F1419)
                ),
                modifier = Modifier.padding(start = 40.dp)
            )

            Button(
                onClick = {
                    viewmodel.purchase()
                },
                enabled = paymentState is PaymentState.Idle,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 32.dp, bottom = 16.dp)
            ) {
                if (paymentState is PaymentState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(text = "订阅以解锁全部功能")
                }
            }
        }
    }

    when (val state = paymentState) {
        is PaymentState.Purchasing, is PaymentState.Checking -> {
            Dialog(
                onDismissRequest = { },
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color.White, shape = RoundedCornerShape(8.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = if (state is PaymentState.Purchasing) "处理中..." else "验证中...",
                            style = TextStyle(fontSize = 16.sp)
                        )
                    }
                }
            }
        }
        is PaymentState.Cancelled -> {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("支付已取消") },
                text = { Text("您已取消本次支付") },
                confirmButton = {
                    TextButton(onClick = { viewmodel.resetState() }) {
                        Text("确定")
                    }
                }
            )
        }
        is PaymentState.Error -> {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("支付失败") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = { viewmodel.resetState() }) {
                        Text("确定")
                    }
                }
            )
        }
        is PaymentState.Success -> {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("支付成功") },
                text = { Text("感谢您的订阅!") },
                confirmButton = {
                    TextButton(onClick = { viewmodel.resetState() }) {
                        Text("确定")
                    }
                }
            )
        }
        else -> {
        }
    }
}