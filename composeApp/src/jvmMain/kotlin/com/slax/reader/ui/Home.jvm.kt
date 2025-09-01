package com.slax.reader.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
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
actual fun HomeScreens(navController: NavController) {
    HomeScreen(navController)
}