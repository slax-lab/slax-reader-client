package com.slax.reader.utils

enum class NetworkState {
    NONE,
    WIFI,
    CELLULAR,
    ACCESS_DENIED
}

expect fun getNetWorkState(): NetworkState