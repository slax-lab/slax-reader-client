package com.slax.reader.utils

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSKeyValueChangeNewKey
import platform.Foundation.NSKeyValueChangeOldKey
import platform.Foundation.NSKeyValueObservingOptionInitial
import platform.Foundation.NSKeyValueObservingOptionNew
import platform.Foundation.NSKeyValueObservingOptionOld
import platform.Foundation.addObserver
import platform.Foundation.removeObserver
import platform.darwin.NSObject
import platform.foundation.NSKeyValueObservingProtocol

@OptIn(ExperimentalForeignApi::class)
class KVOObserver(
    private val onValueChanged: (keyPath: String, oldValue: Any?, newValue: Any?) -> Unit
) : NSObject(), NSKeyValueObservingProtocol {

    @OptIn(ExperimentalForeignApi::class)
    override fun observeValueForKeyPath(
        keyPath: String?,
        ofObject: Any?,
        change: Map<Any?, *>?,
        context: COpaquePointer?
    ) {
        keyPath?.let { path ->
            val newValue = change?.get(NSKeyValueChangeNewKey)
            val oldValue = change?.get(NSKeyValueChangeOldKey)
            onValueChanged(path, newValue, oldValue)
        }
    }

    fun observe(keyPath: String, ofObject: NSObject) {
        ofObject.addObserver(
            observer = this,
            forKeyPath = keyPath,
            options = NSKeyValueObservingOptionNew or NSKeyValueObservingOptionOld,
            context = null
        )
    }

    fun unobserve(keyPath: String, ofObject: NSObject) {
        ofObject.removeObserver(observer = this, forKeyPath = keyPath)
    }
}
