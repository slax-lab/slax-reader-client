package com.slax.reader.data.database.dao

import com.powersync.PowerSyncDatabase
import com.powersync.sync.SyncStatusData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class PowerSyncDao(
    private val database: PowerSyncDatabase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _syncStatusFlow: StateFlow<SyncStatusData?> by lazy {
        database.currentStatus.asFlow()
            .stateIn(scope, SharingStarted.WhileSubscribed(5000), null)
    }

    fun watchPowerSyncStatus(): StateFlow<SyncStatusData?> = _syncStatusFlow
}