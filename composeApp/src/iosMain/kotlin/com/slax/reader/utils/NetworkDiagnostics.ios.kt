@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.slax.reader.utils

import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readValue
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.darwin.AF_INET
import platform.darwin.SOCK_STREAM
import platform.darwin.addrinfo
import platform.darwin.freeifaddrs
import platform.darwin.freeaddrinfo
import platform.darwin.gai_strerror
import platform.darwin.getifaddrs
import platform.darwin.getaddrinfo
import platform.darwin.ifaddrs
import platform.darwin.inet_ntoa
import platform.darwin.sockaddr_in
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding

actual suspend fun resolveSystemDns(domain: String): List<String> = withContext(Dispatchers.IO) {
    resolveSystemDnsBlocking(domain)
}

private fun resolveSystemDnsBlocking(domain: String): List<String> {
    return memScoped {
        val hints = alloc<addrinfo>()
        hints.ai_family = AF_INET
        hints.ai_socktype = SOCK_STREAM
        val result = alloc<CPointerVar<addrinfo>>()
        result.value = null
        val status = getaddrinfo(domain, null, hints.ptr, result.ptr)
        if (status != 0) throw IllegalStateException(gai_strerror(status)?.toKString() ?: "getaddrinfo failed")
        val resultHead = result.value ?: throw IllegalStateException("No addresses resolved")

        try {
            val addresses = mutableListOf<String>()
            var item: CPointer<addrinfo>? = resultHead
            while (item != null) {
                val address = item.pointed.ai_addr
                if (address != null && address.pointed.sa_family.toInt() == AF_INET) {
                    val ipv4 = address.reinterpret<sockaddr_in>()
                    inet_ntoa(ipv4.pointed.sin_addr.readValue())?.toKString()?.let(addresses::add)
                }
                item = item.pointed.ai_next
            }
            addresses.distinct().ifEmpty { throw IllegalStateException("No addresses resolved") }
        } finally {
            freeaddrinfo(resultHead)
        }
    }
}

actual fun networkEnvironmentSummary(): String = runCatching {
    val addresses = memScoped {
        val result = mutableListOf<String>()
        val interfaces = alloc<CPointerVar<ifaddrs>>()
        interfaces.value = null
        if (getifaddrs(interfaces.ptr) == 0) {
            var item = interfaces.value
            while (item != null) {
                val address = item.pointed.ifa_addr
                val name = item.pointed.ifa_name?.toKString().orEmpty()
                if (address != null && address.pointed.sa_family.toInt() == AF_INET && !name.startsWith("lo")) {
                    val ipv4 = address.reinterpret<sockaddr_in>()
                    val value = inet_ntoa(ipv4.pointed.sin_addr.readValue())?.toKString()
                    if (!value.isNullOrEmpty()) result += "$name:$value"
                }
                item = item.pointed.ifa_next
            }
            freeifaddrs(interfaces.value)
        }
        result
    }
    val dns = NSString.stringWithContentsOfFile(
        "/etc/resolv.conf",
        encoding = NSUTF8StringEncoding,
        error = null,
    )?.toString()?.lines()
        ?.filter { it.trimStart().startsWith("nameserver") }
        ?.map { it.trimStart().removePrefix("nameserver").trim() }
        .orEmpty()
    "interfaces=${addresses.joinToString(",").ifEmpty { "none" }} dns=${dns.joinToString(",").ifEmpty { "unknown" }}"
}.getOrElse { "network_diagnostics_error=${it.message}" }
