package com.slax.reader.domain.silent

object BackgroundTaskRunner {
    suspend fun onSilentPush(data: Map<String, String>) {
        println("[BackgroundTask] Silent push received. data=$data")
    }
}