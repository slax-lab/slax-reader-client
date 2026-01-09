package com.slax.reader

import android.app.Application
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactNativeHost
import com.facebook.react.ReactPackage
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint
import com.facebook.react.defaults.DefaultReactNativeHost
import com.facebook.react.shell.MainReactPackage
import com.facebook.soloader.SoLoader
import com.slax.reader.di.configureKoin
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class SlaxReaderApplication : Application(), ReactApplication {

    override val reactNativeHost: ReactNativeHost =
        object : DefaultReactNativeHost(this) {
            override fun getPackages(): List<ReactPackage> {
                // Directly use MainReactPackage
                return listOf(MainReactPackage(null))
            }

            override fun getJSMainModuleName(): String = "index"

            override fun getUseDeveloperSupport(): Boolean = BuildConfig.DEBUG

            // Disable New Architecture (Bridgeless Mode)
            override val isNewArchEnabled: Boolean = false
            override val isHermesEnabled: Boolean = true
        }

    override fun onCreate() {
        super.onCreate()

        // Initialize SoLoader for React Native
        SoLoader.init(this, false)
        if (BuildConfig.DEBUG) {
            DefaultNewArchitectureEntryPoint.load()
        }

        // Initialize Firebase and Koin
        if (GlobalContext.getOrNull() == null) {
            Firebase.initialize(this)
            startKoin {
                androidLogger(Level.INFO)
                androidContext(this@SlaxReaderApplication)
                configureKoin()
            }
        }
    }
}
