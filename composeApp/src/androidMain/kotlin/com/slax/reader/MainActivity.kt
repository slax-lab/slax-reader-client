package com.slax.reader

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.navigation.compose.rememberNavController
import com.slax.reader.domain.auth.GoogleSignInProvider
import com.slax.reader.reactnative.setCurrentActivity
import com.slax.reader.ui.SlaxNavigation

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        ComposeFoundationFlags.isNewContextMenuEnabled = true

        super.onCreate(savedInstanceState)

        // Set activity reference for Google Sign-In
        GoogleSignInProvider.setActivity(this)

        setCurrentActivity(this)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                scrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                scrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT
            )
        )

        setContent {
            val ctrl = rememberNavController()
            SlaxNavigation(ctrl)
        }
    }

    override fun onResume() {
        super.onResume()
        setCurrentActivity(this)
    }

    override fun onDestroy() {
        setCurrentActivity(null)
        super.onDestroy()
    }
}
