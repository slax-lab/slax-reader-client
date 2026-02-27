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
import com.slax.reader.di.configureKoin
import com.slax.reader.domain.auth.GoogleSignInProvider
import com.slax.reader.ui.SlaxNavigation
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        ComposeFoundationFlags.isNewContextMenuEnabled = true

        super.onCreate(savedInstanceState)

        GoogleSignInProvider.setActivity(this)

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

        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidLogger(Level.INFO)
                androidContext(this@MainActivity.applicationContext)
                configureKoin()
            }
        }

        setContent {
            val ctrl = rememberNavController()
            SlaxNavigation(ctrl)
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
