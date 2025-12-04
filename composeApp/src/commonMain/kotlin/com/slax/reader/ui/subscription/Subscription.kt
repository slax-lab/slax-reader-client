package com.slax.reader.ui.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.runBlocking
import org.koin.compose.koinInject

@Composable
fun SubscriptionManagerScreen() {
    val viewmodel: SubscriptionViewModel = koinInject()

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
                    runBlocking {
                        viewmodel.purchase()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 32.dp, bottom = 16.dp)
            ) {
                Text(text = "订阅以解锁全部功能")
            }
        }
    }
}