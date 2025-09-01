package com.slax.reader.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

expect fun onButtonClicked(buttonTitle: String, route: String)

@Composable
fun HomeScreen(navController: NavController) {
    MaterialTheme {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Slax Reader", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            val routes = listOf(
                "Chrome Reader" to "chrome",
                "Hybrid Reader" to "hyper",
                "Rich Render" to "rich",
                "Raw WebView" to "raw_webview",
                "About" to "about"
            )

            routes.forEach { (title, route) ->
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    onClick = {
                        onButtonClicked(title, route)
                        navController.navigate(route)
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(vertical = 4.dp)
                ) {
                    Text(title, color = Color.White)
                }
            }
        }
    }
}

@Composable
expect fun HomeScreens(navController: NavController)
