package com.slax.reader.utils

import kotlinx.cinterop.*
import platform.SystemConfiguration.*
import platform.posix.AF_INET
import platform.posix.sockaddr
import platform.posix.sockaddr_in

@OptIn(ExperimentalForeignApi::class)
actual fun getNetWorkState(): NetworkState {
    val sizeSockaddr = sizeOf<sockaddr_in>()
    val alignSockaddr = alignOf<sockaddr_in>()
    val zeroAddress = nativeHeap.alloc(sizeSockaddr, alignSockaddr)
        .reinterpret<sockaddr_in>()
        .apply {
            sin_len = sizeOf<sockaddr_in>().toUByte()
            sin_family = AF_INET.convert()
        }

    val reachabilityRef = SCNetworkReachabilityCreateWithAddress(
        null,
        zeroAddress.ptr.reinterpret<sockaddr>()
    ) ?: run {
        nativeHeap.free(zeroAddress)
        return NetworkState.NONE
    }

    val networkStatus = memScoped {
        val flags = alloc<SCNetworkReachabilityFlagsVar>()
        if (SCNetworkReachabilityGetFlags(reachabilityRef, flags.ptr)) {
            val flagValue = flags.value

            val isReachable = (flagValue and kSCNetworkReachabilityFlagsReachable) > 0u
            val needsConnection = (flagValue and kSCNetworkReachabilityFlagsConnectionRequired) > 0u
            val isMobileConnection = (flagValue and kSCNetworkReachabilityFlagsIsWWAN) > 0u

            when {
                !isReachable || needsConnection -> NetworkState.NONE
                isMobileConnection -> NetworkState.CELLULAR
                else -> NetworkState.WIFI
            }
        } else {
            NetworkState.NONE
        }
    }

    nativeHeap.free(zeroAddress)
    return networkStatus
}