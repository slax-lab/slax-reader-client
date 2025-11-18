package com.slax.reader.utils

import platform.Foundation.*
import platform.posix.*

actual fun isNetworkException(e: Any): Boolean {
    val errorString = e.toString()
    if (errorString.contains("NSURLErrorDomain")) return true

    val nsError = (e as? NSError) ?: return false

    return when (nsError.domain) {
        NSURLErrorDomain -> isNSURLError(nsError.code)
        NSPOSIXErrorDomain -> isPOSIXNetworkError(nsError.code)
        else -> false
    }
}

private fun isNSURLError(code: Long): Boolean {
    return when (code) {
        NSURLErrorNotConnectedToInternet,        // -1009
        NSURLErrorTimedOut,                      // -1001
        NSURLErrorCannotFindHost,                // -1003
        NSURLErrorCannotConnectToHost,           // -1004
        NSURLErrorNetworkConnectionLost,         // -1005
        NSURLErrorDNSLookupFailed,               // -1006
        NSURLErrorDataNotAllowed,                // -1020
        NSURLErrorInternationalRoamingOff,       // -1018
        NSURLErrorCallIsActive,                  // -1019
        NSURLErrorSecureConnectionFailed,        // -1200
        NSURLErrorServerCertificateHasBadDate,   // -1201
        NSURLErrorServerCertificateUntrusted,    // -1202
        NSURLErrorServerCertificateHasUnknownRoot, // -1203
        NSURLErrorServerCertificateNotYetValid,  // -1204
        NSURLErrorClientCertificateRejected,     // -1205
        NSURLErrorClientCertificateRequired      // -1206
            -> true

        else -> false
    }
}

private fun isPOSIXNetworkError(code: Long): Boolean {
    return when (code.toInt()) {
        ECONNREFUSED,   // Connection refused
        ECONNRESET,     // Connection reset
        ECONNABORTED,   // Connection aborted
        ENETUNREACH,    // Network unreachable
        EHOSTUNREACH,   // Host unreachable
        ETIMEDOUT,      // Timeout
        EPIPE,          // Broken pipe
        ENOTCONN        // Not connected
            -> true

        else -> false
    }
}