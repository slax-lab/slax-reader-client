package com.slax.reader.reactnative

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import react_native.RCTBridgeModuleProtocol

class ReactNativeModulesHelper {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun createNativeModules(): List<RCTBridgeModuleProtocol> {
        val providers = listOf(
            TestModuleProvider()
        )

        return providers.map { it.getModule(coroutineScope) }
    }
}
