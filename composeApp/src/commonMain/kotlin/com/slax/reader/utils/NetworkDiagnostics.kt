package com.slax.reader.utils

expect suspend fun resolveSystemDns(domain: String): List<String>
expect fun networkEnvironmentSummary(): String
