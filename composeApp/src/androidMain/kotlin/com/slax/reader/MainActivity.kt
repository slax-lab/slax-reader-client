package com.slax.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.slax.reader.core.SlaxNavigation
import com.slax.reader.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        
        // 检查Koin是否已经启动，避免重复启动
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(this@MainActivity)
                modules(appModule)
            }
        } else {
            println("Koin application already started, skipping initialization...")
        }
        
        setContent {
            SlaxNavigation()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    SlaxNavigation()
}