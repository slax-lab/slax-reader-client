package com.slax.reader

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.facebook.react.ReactInstanceManager
import com.facebook.react.ReactRootView
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

    private lateinit var reactRootView: ReactRootView

    private val reactInstanceManager: ReactInstanceManager
        get() = SlaxReaderApplication.getInstance()?.getReactInstanceManager()
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

        reactRootView = ReactRootView(this)

        reactRootView.startReactApplication(
            reactInstanceManager,
            moduleName,
            initialProps
        )

        setContentView(reactRootView)
    }

    override fun onResume() {
        super.onResume()
        reactInstanceManager.onHostResume(this, this)
    }

    override fun onPause() {
        super.onPause()
        reactInstanceManager.onHostPause(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        reactRootView.unmountReactApplication()
        reactInstanceManager.onHostDestroy(this)
    }

    @SuppressLint("GestureBackNavigation")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        reactInstanceManager.onBackPressed()
    }

    override fun invokeDefaultOnBackPressed() {
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }
}