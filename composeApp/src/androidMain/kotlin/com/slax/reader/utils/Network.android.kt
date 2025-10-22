package com.slax.reader.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import okhttp3.internal.platform.PlatformRegistry.applicationContext

actual fun getNetWorkState(): NetworkState {
    try {
        val connectivityManager =
            applicationContext!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                ?: return NetworkState.ACCESS_DENIED

        return when {
            networkCapabilities.hasCapability(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkState.WIFI
            networkCapabilities.hasCapability(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkState.CELLULAR
            networkCapabilities.hasCapability(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkState.WIFI
            else -> NetworkState.NONE
        }
    } catch (e: SecurityException) {
        println("get network state fail: ${e.message}")
        return NetworkState.NONE
    }
}