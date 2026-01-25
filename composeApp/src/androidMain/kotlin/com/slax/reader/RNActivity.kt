package com.slax.reader

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.facebook.react.ReactDelegate
import com.facebook.react.ReactHost
import com.facebook.react.modules.core.DefaultHardwareBackBtnHandler
import com.slax.reader.reactnative.setCurrentActivity

class RNActivity : AppCompatActivity(), DefaultHardwareBackBtnHandler {

    companion object {
        private const val EXTRA_MODULE_NAME = "module_name"
        private const val EXTRA_INITIAL_PROPS = "initial_props"

        fun createIntent(
            context: Context,
            moduleName: String,
            initialProps: Bundle? = null
        ): Intent {
            return Intent(context, RNActivity::class.java).apply {
                putExtra(EXTRA_MODULE_NAME, moduleName)
                initialProps?.let { putExtra(EXTRA_INITIAL_PROPS, it) }
            }
        }
    }

    private lateinit var reactDelegate: ReactDelegate

    private val reactHost: ReactHost
        get() = SlaxReaderApplication.getInstance()?.getReactHost()
            ?: throw IllegalStateException("Application not initialized")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCurrentActivity(this)
        val moduleName = intent.getStringExtra(EXTRA_MODULE_NAME)
            ?: throw IllegalArgumentException("Module name required")
        val initialProps = intent.getBundleExtra(EXTRA_INITIAL_PROPS)

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

        reactDelegate = ReactDelegate(this, reactHost, moduleName, initialProps)
        reactDelegate.loadApp(moduleName)
        setContentView(reactDelegate.reactRootView)
    }

    override fun onResume() {
        super.onResume()
        reactDelegate.onHostResume()
    }

    override fun onPause() {
        super.onPause()
        reactDelegate.onHostPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        reactDelegate.onHostDestroy()
    }

    @SuppressLint("GestureBackNavigation")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!reactDelegate.onBackPressed()) {
            super.onBackPressed()
        }
    }

    override fun invokeDefaultOnBackPressed() {
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }
}
