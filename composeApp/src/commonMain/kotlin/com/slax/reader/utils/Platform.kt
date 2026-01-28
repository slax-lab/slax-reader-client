package com.slax.reader.utils

expect val platformType: String

fun isAndroid(): Boolean = platformType == "android"
fun isIOS(): Boolean = platformType == "ios"