package com.slax.reader

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

actual fun onButtonClicked(buttonTitle: String, route: String) {
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    println("🖥️ [$timestamp] JVM桌面用户点击了: $buttonTitle -> 导航到: $route")
}

@Composable
@Preview
actual fun HomeScreen(navController: NavController) {
    MaterialTheme {
        CommonHomeScreen(navController)
    }
}