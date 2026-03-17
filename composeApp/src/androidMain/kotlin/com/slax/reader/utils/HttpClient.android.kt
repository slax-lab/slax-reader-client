package com.slax.reader.utils

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.Dns
import java.net.Inet4Address

actual fun platformEngine(): HttpClientEngine = OkHttp.create {
    config {
        dns { hostname -> Dns.SYSTEM.lookup(hostname).sortedBy { if (it is Inet4Address) 0 else 1 } }
        retryOnConnectionFailure(true)
    }
}