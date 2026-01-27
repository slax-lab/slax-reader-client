package com.slax.reader.utils

enum class SlaxPlatform {
    ANDROID,
    IOS,
}

expect val platformType: SlaxPlatform

fun isAndroid(): Boolean = platformType == SlaxPlatform.ANDROID
fun isIOS(): Boolean = platformType == SlaxPlatform.IOS