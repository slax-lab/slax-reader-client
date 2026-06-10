package com.slax.reader

import android.app.Application
import com.slax.reader.data.preferences.AppPreferences
import com.slax.reader.di.configureKoin
import com.slax.reader.utils.AppEnv
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class SlaxReaderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidLogger(Level.INFO)
                androidContext(this@SlaxReaderApplication)
                configureKoin()
            }
        }
        runBlocking {
            AppEnv.init(GlobalContext.get().get<AppPreferences>().getSelectedEnv())
        }
    }
}
