package com.slax.reader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.facebook.react.ReactInstanceManager
import com.facebook.react.ReactRootView
import com.facebook.react.common.LifecycleState
import com.facebook.react.modules.core.DefaultHardwareBackBtnHandler
import com.facebook.react.shell.MainReactPackage
import com.slax.reader.reactnative.SlaxReaderReactPackage

class RNActivity : AppCompatActivity(), DefaultHardwareBackBtnHandler {

    companion object {
        private const val EXTRA_MODULE_NAME = "module_name"

        fun createIntent(context: Context, moduleName: String): Intent {
            return Intent(context, RNActivity::class.java).apply {
                putExtra(EXTRA_MODULE_NAME, moduleName)
            }
        }
    }

    private var reactRootView: ReactRootView? = null
    private var reactInstanceManager: ReactInstanceManager? = null
    private var slaxReaderReactPackage: SlaxReaderReactPackage? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val moduleName = intent.getStringExtra(EXTRA_MODULE_NAME) ?: "SlaxReaderRN"

        reactRootView = ReactRootView(this)
        slaxReaderReactPackage = SlaxReaderReactPackage()

        reactInstanceManager = ReactInstanceManager.builder()
            .setApplication(application)
            .setCurrentActivity(this)
            .apply {
                if (BuildConfig.DEBUG) {
                    setJSMainModulePath("index")
                    setUseDeveloperSupport(true)
                } else {
                    setBundleAssetName("index.android.bundle")
                    setUseDeveloperSupport(false)
                }
            }
            .addPackage(MainReactPackage(null))
            .addPackage(slaxReaderReactPackage!!)
            .setInitialLifecycleState(LifecycleState.RESUMED)
            .build()

        reactRootView?.startReactApplication(reactInstanceManager, moduleName, null)
        setContentView(reactRootView)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                reactInstanceManager?.onBackPressed() ?: finish()
            }
        })
    }

    override fun invokeDefaultOnBackPressed() {
        onBackPressedDispatcher.onBackPressed()
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
        slaxReaderReactPackage?.cleanup()
        reactRootView = null
        reactInstanceManager = null
        slaxReaderReactPackage = null
    }
}