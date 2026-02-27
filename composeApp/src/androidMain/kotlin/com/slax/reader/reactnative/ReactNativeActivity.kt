package com.slax.reader.reactnative

import android.os.Bundle
import android.util.Log
import com.facebook.react.modules.core.DefaultHardwareBackBtnHandler
import com.slax.reactnativeapp.BrownfieldActivity
import com.slax.reactnativeapp.showReactNativeFragment

class ReactNativeActivity : BrownfieldActivity(), DefaultHardwareBackBtnHandler {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            BrownfieldMessagingBridge.ensureRegistered()
            val launchOptions = intent?.extras?.let { extras ->
                Bundle(extras)
            }
            showReactNativeFragment(launchOptions)
        } catch (e: Throwable) {
            Log.e("ReactNativeActivity", "Failed to show RN fragment", e)
        }
    }

    override fun invokeDefaultOnBackPressed() {
        super.onBackPressed()
    }
}
