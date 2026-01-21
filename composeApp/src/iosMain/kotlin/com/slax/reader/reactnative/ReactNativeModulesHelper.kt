package com.slax.reader.reactnative

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import react_native.RCTBridgeModuleProtocol

class ReactNativeModulesHelper {
    private val supervisorJob = SupervisorJob()
    private val coroutineScope = CoroutineScope(supervisorJob + Dispatchers.Default)

    fun createNativeModules(): List<RCTBridgeModuleProtocol> {
        val providers = listOf(
            TestModuleProvider()
        )

        return providers.map { it.getModule(coroutineScope) }
    }

    fun cleanup() {
        supervisorJob.cancel()
    }
}
