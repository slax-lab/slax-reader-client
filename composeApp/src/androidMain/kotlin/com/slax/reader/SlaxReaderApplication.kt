package com.slax.reader

import android.app.Application
import android.content.res.Configuration
import com.slax.reactnativeapp.BrownfieldLifecycleDispatcher

class SlaxReaderApplication : Application() {

    companion object {
        lateinit var instance: SlaxReaderApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        BrownfieldLifecycleDispatcher.onApplicationCreate(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        BrownfieldLifecycleDispatcher.onConfigurationChanged(this, newConfig)
    }
}
