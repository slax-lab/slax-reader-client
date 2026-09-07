package com.slax.reader.domain.coordinator

import kotlinx.coroutines.flow.StateFlow

interface NetworkCoordinator {
    val syncState: StateFlow<AppSyncState>
    suspend fun isNetworkAvailable(): Boolean
}
