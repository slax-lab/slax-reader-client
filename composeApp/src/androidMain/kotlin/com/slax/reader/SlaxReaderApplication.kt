package com.slax.reader

import android.app.Application
import com.facebook.react.ReactHost
import com.facebook.react.ReactPackage
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint
import com.facebook.react.defaults.DefaultReactHost
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
    private var _reactHost: ReactHost? = null
    private var slaxReaderReactPackage: SlaxReaderReactPackage? = null

    override fun onCreate() {
        super.onCreate()
        instance = this

        SoLoader.init(this, OpenSourceMergedSoMapping)
        DefaultNewArchitectureEntryPoint.load()

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
    fun getReactHost(): ReactHost {
        if (_reactHost == null) {
            slaxReaderReactPackage = SlaxReaderReactPackage()
            val packages: List<ReactPackage> = listOf(
                MainReactPackage(null),
                RNGetRandomValuesPackage(),
                slaxReaderReactPackage!!
            )
            _reactHost = DefaultReactHost.getDefaultReactHost(
                this,
                packages,
                jsMainModulePath = "index",
                jsBundleAssetPath = "index.android.bundle",
                jsRuntimeFactory = null,
                useDevSupport = BuildConfig.DEBUG
            )

            println("[SlaxReaderApplication] ReactHost initialized (lazy)")
        }
        return _reactHost!!
    }

    override fun onTerminate() {
        super.onTerminate()
        _reactHost?.invalidate()
        _reactHost = null
        slaxReaderReactPackage?.cleanup()
        slaxReaderReactPackage = null
        instance = null
    }
}
