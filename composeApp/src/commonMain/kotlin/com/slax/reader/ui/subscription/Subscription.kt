package com.slax.reader.ui.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.slax.reader.SlaxConfig
import com.slax.reader.utils.UserWebView
import com.slax.reader.utils.WebViewEvent
import com.slax.reader.utils.rememberUserWebViewState
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_sm_back

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionManagerScreen(onBackClick: () -> Unit) {
    val viewmodel: SubscriptionViewModel = koinInject()
    val paymentState by viewmodel.paymentState.collectAsState()

    var isLoading by remember { mutableStateOf(true) }

    val userWebViewState = rememberUserWebViewState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pro Reader",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF0F1419)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_sm_back),
                            contentDescription = "返回",
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
        containerColor = Color(0xFFF5F5F3)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            UserWebView(
//                url = "${SlaxConfig.WEB_BASE_URL}/subscription/inapp-purchase",
                url = "http://192.168.9.103:3000/subscription/inapp-purchase",
                modifier = Modifier.fillMaxSize(),
                userWebViewState = userWebViewState,
                onPageLoaded = {
                    isLoading = false
                }
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF16B998),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }

    LaunchedEffect(userWebViewState) {
        userWebViewState.events.collect { event ->
            when (event) {
                is WebViewEvent.GetSubscription -> {
                    val status = when (paymentState) {
                        is PaymentState.Success -> "active"
                        is PaymentState.Error, is PaymentState.Cancelled -> "inactive"
                        else -> "unknown"
                    }

                    userWebViewState.sendSubscriptionUpdate(status)
                }
                is WebViewEvent.StartSubscription -> {
                    viewmodel.purchase()
                }
                else -> {
                }
            }
        }
    }

    LaunchedEffect(paymentState) {
        if (paymentState is PaymentState.Success) {
            userWebViewState.sendSubscriptionUpdate("active")
        } else if (paymentState is PaymentState.Error || paymentState is PaymentState.Cancelled) {
            userWebViewState.sendSubscriptionUpdate("inactive")
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