package com.slax.reader.utils

expect val platformType: String

expect val platformName: String

fun isAndroid(): Boolean = platformType == "android"
fun isIOS(): Boolean = platformType == "ios"