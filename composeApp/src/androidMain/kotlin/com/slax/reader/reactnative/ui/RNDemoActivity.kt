package com.slax.reader.reactnative.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.facebook.react.ReactInstanceManager
import com.facebook.react.ReactRootView
import com.facebook.react.common.LifecycleState
import com.facebook.react.modules.core.DefaultHardwareBackBtnHandler
import com.facebook.react.shell.MainReactPackage
import com.slax.reader.BuildConfig
import com.slax.reader.reactnative.SlaxReaderReactPackage

class RNDemoActivity : AppCompatActivity(), DefaultHardwareBackBtnHandler {

    private var reactRootView: ReactRootView? = null
    private var reactInstanceManager: ReactInstanceManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        reactRootView = ReactRootView(this)

        // Create ReactInstanceManager with explicit package configuration
        // Use RN_RELEASE_MODE to control whether to use release bundle or dev server
        val useReleaseBundle = System.getenv("RN_RELEASE_MODE")?.toBoolean() ?: false

        reactInstanceManager = ReactInstanceManager.builder()
            .setApplication(application)
            .setCurrentActivity(this)
            .apply {
                if (useReleaseBundle) {
                    // Load pre-built release bundle from assets (RN release mode)
                    setBundleAssetName("index.android.bundle")
                    setUseDeveloperSupport(false)
                } else {
                    // Connect to Metro Bundler for live reload (RN debug mode)
                    setJSMainModulePath("index")
                    setUseDeveloperSupport(BuildConfig.DEBUG)
                }
            }
            .addPackage(MainReactPackage(null))  // Explicitly add MainReactPackage
            .addPackage(SlaxReaderReactPackage())  // Add KMP-generated modules
            .setInitialLifecycleState(LifecycleState.RESUMED)
            .build()

        // Start the app
        reactRootView?.startReactApplication(reactInstanceManager, "SlaxReaderRN", null)

        setContentView(reactRootView)
    }

    override fun invokeDefaultOnBackPressed() {
        super.onBackPressed()
    }

    override fun onPause() {
        super.onPause()
        reactInstanceManager?.onHostPause(this)
    }

    override fun onResume() {
        super.onResume()
        reactInstanceManager?.onHostResume(this, this)
    }

    override fun onDestroy() {
        super.onDestroy()
        reactInstanceManager?.onHostDestroy(this)
        reactRootView?.unmountReactApplication()
        reactRootView = null
        reactInstanceManager = null
    }
}