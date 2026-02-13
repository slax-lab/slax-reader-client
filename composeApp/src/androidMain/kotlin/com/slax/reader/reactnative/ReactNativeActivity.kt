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
            showReactNativeFragment()
        } catch (e: Throwable) {
            Log.e("ReactNativeActivity", "Failed to show RN fragment", e)
        }
    }

    override fun invokeDefaultOnBackPressed() {
        super.onBackPressed()
    }
}
