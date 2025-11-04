package com.slax.reader.utils

import platform.Foundation.NSNumericSearch
import platform.Foundation.NSOrderedAscending
import platform.Foundation.NSString
import platform.Foundation.compare
import platform.UIKit.UIDevice

@Suppress("CAST_NEVER_SUCCEEDS")
fun available(iosVersion: String): Boolean {
    val currentVersion = UIDevice.currentDevice.systemVersion
    return (currentVersion as NSString).compare(
        iosVersion,
        NSNumericSearch
    ) != NSOrderedAscending
}