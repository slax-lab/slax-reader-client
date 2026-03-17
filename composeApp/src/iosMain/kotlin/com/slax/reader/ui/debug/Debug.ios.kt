package com.slax.reader.ui.debug

import io.ktor.utils.io.core.toByteArray
import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.*
import platform.UIKit.UIDevice
import platform.darwin.freeifaddrs
import platform.darwin.getifaddrs
import platform.darwin.ifaddrs
import platform.darwin.inet_aton
import platform.darwin.inet_ntoa
import platform.posix.*

actual fun getSystemInfo(): Map<String, String> {
    val info = mutableMapOf<String, String>()

    val device = UIDevice.currentDevice
    info["OS"] = "iOS ${device.systemVersion}"
    info["Device"] = device.model
    info["IP Addresses"] = getLocalIPAddresses()
    info["DNS Servers"] = getDnsServers()

    return info
}

@OptIn(ExperimentalForeignApi::class)
private fun getLocalIPAddresses(): String {
    val addresses = mutableListOf<String>()
    memScoped {
        val ifap = alloc<CPointerVar<ifaddrs>>()
        if (getifaddrs(ifap.ptr) == 0) {
            var ifa = ifap.value
            while (ifa != null) {
                val addr = ifa.pointed.ifa_addr
                if (addr != null && addr.pointed.sa_family.toInt() == AF_INET) {
                    val name = ifa.pointed.ifa_name?.toKString() ?: ""
                    if (!name.startsWith("lo")) {
                        val sockAddr = addr.reinterpret<sockaddr_in>()
                        val ip = inet_ntoa(sockAddr.pointed.sin_addr.readValue())?.toKString() ?: ""
                        if (ip.isNotEmpty()) addresses.add("$name: $ip")
                    }
                }
                ifa = ifa.pointed.ifa_next
            }
            freeifaddrs(ifap.value)
        }
    }
    return addresses.joinToString("\n").ifEmpty { "None" }
}

private fun getDnsServers(): String {
    return try {
        val content = NSString.stringWithContentsOfFile(
            "/etc/resolv.conf",
            encoding = NSUTF8StringEncoding,
            error = null
        ) ?: return "Unknown"
        val servers = (content as String).lines()
            .filter { it.trimStart().startsWith("nameserver") }
            .map { it.trimStart().removePrefix("nameserver").trim() }
        servers.joinToString(", ").ifEmpty { "Unknown" }
    } catch (e: Exception) {
        "Unknown"
    }
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun resolveDns(domain: String, dnsServer: String?): String = withContext(Dispatchers.IO) {
    if (dnsServer == null) {
        // 使用系统 DNS via getaddrinfo
        memScoped {
            val hints = alloc<addrinfo>()
            hints.ai_family = AF_INET
            hints.ai_socktype = SOCK_STREAM

            val result = alloc<CPointerVar<addrinfo>>()
            val ret = getaddrinfo(domain, null, hints.ptr, result.ptr)
            if (ret != 0) throw Exception(gai_strerror(ret)?.toKString() ?: "getaddrinfo failed")

            val addresses = mutableListOf<String>()
            var ai = result.value
            while (ai != null) {
                val addr = ai.pointed.ai_addr
                if (addr != null && addr.pointed.sa_family.toInt() == AF_INET) {
                    val sin = addr.reinterpret<sockaddr_in>()
                    val ip = inet_ntoa(sin.pointed.sin_addr.readValue())?.toKString()
                    if (ip != null) addresses.add(ip)
                }
                ai = ai.pointed.ai_next
            }
            freeaddrinfo(result.value)

            if (addresses.isEmpty()) throw Exception("No addresses resolved")
            addresses.distinct().joinToString(", ")
        }
    } else {
        // 指定 DNS 服务器，手动 UDP 查询
        queryDnsServer(domain, dnsServer)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun queryDnsServer(domain: String, dnsServer: String): String {
    return memScoped {
        val sockFd = socket(AF_INET, SOCK_DGRAM, 0)
        if (sockFd < 0) throw Exception("socket() failed")

        try {
            val timeout = alloc<timeval>()
            timeout.tv_sec = 5
            timeout.tv_usec = 0
            setsockopt(sockFd, SOL_SOCKET, SO_RCVTIMEO, timeout.ptr, sizeOf<timeval>().convert())

            val serverAddr = alloc<sockaddr_in>()
            serverAddr.sin_family = AF_INET.convert()
            serverAddr.sin_port = ((53u shr 8) or ((53u and 0xFFu) shl 8)).toUShort()
            inet_aton(dnsServer, serverAddr.sin_addr.ptr)

            val query = buildDnsQuery(domain)
            val queryBytes = query.toByteArray()
            val sendBuf = allocArray<ByteVar>(queryBytes.size)
            queryBytes.forEachIndexed { i, b -> sendBuf[i] = b }

            sendto(
                sockFd, sendBuf, queryBytes.size.convert(), 0,
                serverAddr.ptr.reinterpret(), sizeOf<sockaddr_in>().convert()
            )

            val recvBuf = allocArray<ByteVar>(512)
            val recvLen = recv(sockFd, recvBuf, 512u, 0).toInt()
            if (recvLen <= 0) throw Exception("No response")

            val buf = ByteArray(recvLen) { recvBuf[it] }
            parseDnsResponse(buf, recvLen)
        } finally {
            close(sockFd)
        }
    }
}

private fun buildDnsQuery(domain: String): List<Byte> {
    val buf = mutableListOf<Byte>()
    buf.addAll(listOf(0x00, 0x01, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00))
    for (label in domain.split(".")) {
        buf.add(label.length.toByte())
        buf.addAll(label.toByteArray().toList())
    }
    buf.add(0x00)
    buf.addAll(listOf(0x00, 0x01, 0x00, 0x01))
    return buf
}

private fun parseDnsResponse(buf: ByteArray, len: Int): String {
    val answerCount = ((buf[6].toInt() and 0xFF) shl 8) or (buf[7].toInt() and 0xFF)
    if (answerCount == 0) throw Exception("No answers")

    var offset = 12
    while (offset < len && buf[offset] != 0.toByte()) {
        if (buf[offset].toInt() and 0xC0 == 0xC0) { offset += 2; break }
        offset += (buf[offset].toInt() and 0xFF) + 1
    }
    if (offset < len && buf[offset] == 0.toByte()) offset++
    offset += 4

    val ips = mutableListOf<String>()
    repeat(answerCount) {
        if (offset >= len) return@repeat
        if (buf[offset].toInt() and 0xC0 == 0xC0) offset += 2
        else { while (offset < len && buf[offset] != 0.toByte()) offset++; offset++ }
        val type = ((buf[offset].toInt() and 0xFF) shl 8) or (buf[offset + 1].toInt() and 0xFF)
        offset += 8
        val rdLength = ((buf[offset].toInt() and 0xFF) shl 8) or (buf[offset + 1].toInt() and 0xFF)
        offset += 2
        if (type == 1 && rdLength == 4) {
            val ip = "${buf[offset].toInt() and 0xFF}.${buf[offset+1].toInt() and 0xFF}.${buf[offset+2].toInt() and 0xFF}.${buf[offset+3].toInt() and 0xFF}"
            ips.add(ip)
        }
        offset += rdLength
    }
    return ips.joinToString(", ").ifEmpty { throw Exception("No A records") }
}
