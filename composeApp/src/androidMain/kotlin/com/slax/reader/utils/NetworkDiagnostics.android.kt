package com.slax.reader.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext
import java.net.InetAddress

actual suspend fun resolveSystemDns(domain: String): List<String> = withContext(Dispatchers.IO) {
    InetAddress.getAllByName(domain)
        .mapNotNull { it.hostAddress }
        .distinct()
        .ifEmpty { throw IllegalStateException("No addresses resolved") }
}

actual fun networkEnvironmentSummary(): String = runCatching {
    val context = GlobalContext.get().get<Context>()
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = manager.activeNetwork ?: return@runCatching "network=none"
    val capabilities = manager.getNetworkCapabilities(network)
    val properties = manager.getLinkProperties(network)
    val transports = buildList {
        if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) add("wifi")
        if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) add("cellular")
        if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) add("vpn")
        if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true) add("ethernet")
    }.ifEmpty { listOf("other") }
    val addresses = properties?.linkAddresses?.map { it.address.hostAddress ?: "unknown" }.orEmpty()
    val dns = properties?.dnsServers?.map { it.hostAddress ?: "unknown" }.orEmpty()
    "network=${transports.joinToString("+")} interface=${properties?.interfaceName ?: "unknown"} " +
        "addresses=${addresses.joinToString(",").ifEmpty { "none" }} dns=${dns.joinToString(",").ifEmpty { "none" }}"
}.getOrElse { "network_diagnostics_error=${it.message}" }
