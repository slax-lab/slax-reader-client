package com.slax.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.slax.reader.di.appModule
import com.slax.reader.ui.SlaxNavigation
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.compose.KoinApplication
import org.koin.core.logger.Level

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            KoinApplication(
                application = {
                    androidLogger(Level.INFO)
                    androidContext(androidContext = this@MainActivity)
                    modules(appModule)
                }
            ) {
                val ctrl = rememberNavController()
                SlaxNavigation(ctrl)
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    val ctrl = rememberNavController()
    SlaxNavigation(ctrl)
}