package com.slax.reader.ui.debug

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface

actual fun getSystemInfo(): Map<String, String> {
    val info = mutableMapOf<String, String>()

    info["OS"] = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    info["Device"] = "${Build.MANUFACTURER} ${Build.MODEL}"

    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: emptyList()
        val ipAddresses = interfaces
            .filter { it.isUp && !it.isLoopback }
            .flatMap { iface ->
                iface.inetAddresses.toList()
                    .filter { !it.isLoopbackAddress && it.hostAddress?.contains(':') == false }
                    .map { "${iface.displayName}: ${it.hostAddress}" }
            }
        info["IP Addresses"] = ipAddresses.joinToString("\n").ifEmpty { "None" }
    } catch (e: Exception) {
        info["IP Addresses"] = "Error: ${e.message}"
    }

    info["DNS Servers"] = getDnsServers()

    return info
}

private fun getDnsServers(): String {
    return try {
        val context = GlobalContext.get().get<Context>()
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return "No active network"
        val props: LinkProperties = cm.getLinkProperties(network) ?: return "No link properties"
        val servers = props.dnsServers.map { it.hostAddress ?: "?" }
        servers.joinToString(", ").ifEmpty { "None" }
    } catch (e: Exception) {
        "Error: ${e.message}"
    }
}

actual suspend fun resolveDns(domain: String, dnsServer: String?): String = withContext(Dispatchers.IO) {
    if (dnsServer == null) {
        // 使用系统 DNS
        try {
            val addresses = InetAddress.getAllByName(domain)
            addresses.mapNotNull { it.hostAddress }
                .filter { !it.contains(':') } // 只取 IPv4
                .joinToString(", ")
                .ifEmpty { throw Exception("No IPv4 addresses") }
        } catch (e: Exception) {
            throw Exception("Failed: ${e.message}")
        }
    } else {
        // 手动构造 DNS UDP 查询，指定 DNS 服务器
        queryDnsServer(domain, dnsServer)
    }
}

// 构造 DNS A 记录查询报文，发送到指定 DNS 服务器
private fun queryDnsServer(domain: String, dnsServer: String): String {
    val socket = DatagramSocket()
    socket.soTimeout = 5000

    try {
        val query = buildDnsQuery(domain)
        val serverAddr = InetAddress.getByName(dnsServer)
        val sendPacket = DatagramPacket(query, query.size, serverAddr, 53)
        socket.send(sendPacket)

        val buf = ByteArray(512)
        val recvPacket = DatagramPacket(buf, buf.size)
        socket.receive(recvPacket)

        return parseDnsResponse(buf, recvPacket.length)
    } finally {
        socket.close()
    }
}

private fun buildDnsQuery(domain: String): ByteArray {
    val buf = mutableListOf<Byte>()
    // Transaction ID
    buf.addAll(listOf(0x00, 0x01))
    // Flags: standard query
    buf.addAll(listOf(0x01, 0x00))
    // Questions: 1
    buf.addAll(listOf(0x00, 0x01))
    // Answer/Authority/Additional RRs: 0
    buf.addAll(listOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00))
    // QNAME
    for (label in domain.split(".")) {
        buf.add(label.length.toByte())
        buf.addAll(label.toByteArray().toList())
    }
    buf.add(0x00) // end of QNAME
    // QTYPE: A (1)
    buf.addAll(listOf(0x00, 0x01))
    // QCLASS: IN (1)
    buf.addAll(listOf(0x00, 0x01))
    return buf.toByteArray()
}

private fun parseDnsResponse(buf: ByteArray, len: Int): String {
    // Answer count at offset 6-7
    val answerCount = ((buf[6].toInt() and 0xFF) shl 8) or (buf[7].toInt() and 0xFF)
    if (answerCount == 0) throw Exception("No answers")

    // Skip header (12 bytes) + question section
    var offset = 12
    // Skip QNAME
    while (offset < len && buf[offset] != 0.toByte()) {
        if (buf[offset].toInt() and 0xC0 == 0xC0) { offset += 2; break }
        offset += (buf[offset].toInt() and 0xFF) + 1
    }
    if (buf[offset] == 0.toByte()) offset++ // null terminator
    offset += 4 // skip QTYPE + QCLASS

    val ips = mutableListOf<String>()
    repeat(answerCount) {
        if (offset >= len) return@repeat
        // Skip name (may be pointer)
        if (buf[offset].toInt() and 0xC0 == 0xC0) offset += 2
        else {
            while (offset < len && buf[offset] != 0.toByte()) offset++
            offset++
        }
        val type = ((buf[offset].toInt() and 0xFF) shl 8) or (buf[offset + 1].toInt() and 0xFF)
        offset += 8 // skip type, class, ttl
        val rdLength = ((buf[offset].toInt() and 0xFF) shl 8) or (buf[offset + 1].toInt() and 0xFF)
        offset += 2
        if (type == 1 && rdLength == 4) { // A record
            val ip = "${buf[offset].toInt() and 0xFF}.${buf[offset+1].toInt() and 0xFF}.${buf[offset+2].toInt() and 0xFF}.${buf[offset+3].toInt() and 0xFF}"
            ips.add(ip)
        }
        offset += rdLength
    }

    return ips.joinToString(", ").ifEmpty { throw Exception("No A records") }
}
