package com.slax.reader

import android.app.Application
import com.facebook.react.ReactInstanceManager
import com.facebook.react.common.LifecycleState
import com.facebook.react.shell.MainReactPackage
import com.facebook.react.soloader.OpenSourceMergedSoMapping
import com.facebook.soloader.SoLoader
import com.slax.reader.di.configureKoin
import com.slax.reader.reactnative.SlaxReaderReactPackage
import org.linusu.RNGetRandomValuesPackage
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class SlaxReaderApplication : Application() {

    companion object {
        @Volatile
        private var instance: SlaxReaderApplication? = null

        fun getInstance(): SlaxReaderApplication? = instance
    }

    @Volatile
    private var _reactInstanceManager: ReactInstanceManager? = null
    private var slaxReaderReactPackage: SlaxReaderReactPackage? = null

    override fun onCreate() {
        super.onCreate()
        instance = this

        SoLoader.init(this, OpenSourceMergedSoMapping)

        if (GlobalContext.getOrNull() == null) {
            Firebase.initialize(this)
            startKoin {
                androidLogger(Level.INFO)
                androidContext(this@SlaxReaderApplication)
                configureKoin()
            }
        }
    }

    @Synchronized
    fun getReactInstanceManager(): ReactInstanceManager {
        if (_reactInstanceManager == null) {
            slaxReaderReactPackage = SlaxReaderReactPackage()

            _reactInstanceManager = ReactInstanceManager.builder()
                .setApplication(this)
                .setCurrentActivity(null)
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
                .addPackage(RNGetRandomValuesPackage())
                .addPackage(slaxReaderReactPackage!!)
                .setInitialLifecycleState(LifecycleState.BEFORE_CREATE)
                .build()

            println("[SlaxReaderApplication] ReactInstanceManager initialized (lazy)")
        }
        return _reactInstanceManager!!
    }

    override fun onTerminate() {
        super.onTerminate()
        _reactInstanceManager?.destroy()
        _reactInstanceManager = null
        slaxReaderReactPackage?.cleanup()
        slaxReaderReactPackage = null
        instance = null
    }
}
