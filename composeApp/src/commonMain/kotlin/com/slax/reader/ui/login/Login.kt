package com.slax.reader.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.slax.reader.SlaxConfig
import com.mmk.kmpauth.google.GoogleAuthCredentials
import com.mmk.kmpauth.google.GoogleAuthProvider
import com.mmk.kmpauth.google.GoogleButtonUiContainer
import com.slax.reader.const.AppError
import com.slax.reader.domain.auth.AuthDomain
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {}
) {
    val authDomain: AuthDomain = koinInject()
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    GoogleAuthProvider.create(
        credentials = GoogleAuthCredentials(serverId = SlaxConfig.GOOGLE_AUTH_SERVER_ID)
    )

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Login Failed") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F3))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GoogleButtonUiContainer(onGoogleSignInResult = { googleUser ->
                val idToken = googleUser?.idToken
                println("id token: $idToken =============")

                if (idToken != null) {
                    isLoading = true
                    scope.launch {
                        val result = authDomain.signIn(idToken)
                        isLoading = false

                        result.onSuccess {
                            onLoginSuccess()
                        }.onFailure { exception ->
                            // Extract proper error message
                            val message = when (exception) {
                                is AppError.ApiException.HttpError -> {
                                    "Login failed (${exception.code}): ${exception.message}"
                                }

                                is AppError.AuthException -> {
                                    "Auth error: ${exception.message}"
                                }

                                else -> {
                                    exception.message ?: "Unknown error occurred"
                                }
                            }
                            errorMessage = message
                            println("Login error: $message")
                        }
                    }
                } else {
                    errorMessage = "Failed to get Google ID token"
                }
            }) {
                Button(
                    onClick = { this.onClick() },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                    } else {
                        Text("Google Sign-In(Custom Design)")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

}