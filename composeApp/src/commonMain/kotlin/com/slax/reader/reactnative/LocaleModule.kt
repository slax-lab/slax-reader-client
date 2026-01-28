package com.slax.reader.reactnative

import com.slax.reader.const.localeString
import com.slax.reader.utils.LocaleString
import de.voize.reaktnativetoolkit.annotation.ReactNativeMethod
import de.voize.reaktnativetoolkit.annotation.ReactNativeModule

@ReactNativeModule("LocaleModule")
class LocaleModule {

    @ReactNativeMethod
    fun getCurrentLocale(): String {
        return LocaleString.currentLocale
    }
}
