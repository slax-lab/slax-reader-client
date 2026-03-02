package com.slax.reader.reactnative

import android.os.Bundle
import android.util.Log
import com.facebook.react.modules.core.DefaultHardwareBackBtnHandler
import com.slax.reactnativeapp.BrownfieldActivity
import com.slax.reactnativeapp.ReactNativeHostManager
import com.slax.reactnativeapp.ReactNativeViewFactory
import com.slax.reactnativeapp.RootComponent
import com.slax.reactnativeapp.setUpNativeBackHandling

class ReactNativeActivity : BrownfieldActivity(), DefaultHardwareBackBtnHandler {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            BrownfieldMessagingBridge.ensureRegistered()
            ReactNativeHostManager.shared.initialize(application)

            val launchOptions = intent?.extras?.let { extras ->
                Bundle(extras)
            }

            val reactView = ReactNativeViewFactory.createFrameLayout(
                this,
                this,
                RootComponent.Main,
                launchOptions
            )

            setContentView(reactView)
            setUpNativeBackHandling()
        } catch (e: Throwable) {
            Log.e("ReactNativeActivity", "Failed to show RN fragment", e)
        }
    }

    override fun invokeDefaultOnBackPressed() {
        super.onBackPressed()
    }
}
