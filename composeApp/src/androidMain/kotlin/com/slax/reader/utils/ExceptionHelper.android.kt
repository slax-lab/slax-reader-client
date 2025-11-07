package com.slax.reader.utils

import io.ktor.utils.io.*
import java.io.EOFException
import java.net.*
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException

actual fun isNetworkException(e: Any): Boolean {
    return when (e) {
        is ConnectException -> true
        is NoRouteToHostException -> true
        is PortUnreachableException -> true
        is ProtocolException -> true
        is ClosedByteChannelException -> true
        is SSLHandshakeException -> true
        is SSLException -> true
        is SocketException -> true
        is SocketTimeoutException -> true
        is UnknownHostException -> true
        is EOFException -> true
        else -> false
    }
}