package com.slax.reader.domain.coordinator

import com.powersync.PowerSyncDatabase
import com.slax.reader.data.database.dao.PowerSyncDao
import com.slax.reader.utils.ConnectOptions
import com.slax.reader.utils.ConnectParams
import com.slax.reader.utils.Connector
import com.slax.reader.utils.isNetworkException
import dev.jordond.connectivity.Connectivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed class AppSyncState {
    data object NoNetwork : AppSyncState()
    data object Connecting : AppSyncState()
    data class Downloading(val progress: Float?) : AppSyncState()
    data object Uploading : AppSyncState()
    data object Connected : AppSyncState()
    data class Error(val message: String) : AppSyncState()
}

class CoordinatorDomain(
    private val database: PowerSyncDatabase,
    private val connector: Connector,
    private val powerSyncDao: PowerSyncDao
) {
    private var workerScope: CoroutineScope? = null

    private var isConnected = false
    private val connectivity = Connectivity()
    private val lifecycleMutex = Mutex()
    private val connectionMutex = Mutex()

    private val _syncState = MutableStateFlow<AppSyncState>(AppSyncState.Connecting)
    val syncState: StateFlow<AppSyncState> = _syncState.asStateFlow()

    suspend fun startup() {
        lifecycleMutex.withLock {
            if (workerScope != null) return@withLock

            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            workerScope = scope

            scope.launch {
                connect()
                connectivity.start()
            }

            scope.launch {
                connectivity.statusUpdates.collect { status ->
                    if (status is Connectivity.Status.Connected) {
                        connect()
                    } else {
                        disconnect()
                    }
                }
            }

            scope.launch {
                combine(
                    powerSyncDao.watchPowerSyncStatus(),
                    connectivity.statusUpdates
                ) { syncStatus, networkStatus ->
                    val hasNetwork = networkStatus is Connectivity.Status.Connected

                    when {
                        syncStatus == null -> AppSyncState.Connecting
                        syncStatus.connected -> AppSyncState.Connected
                        syncStatus.downloading -> AppSyncState.Downloading(
                            syncStatus.downloadProgress?.let { progress ->
                                if (progress.totalOperations > 0) {
                                    (progress.downloadedOperations.toFloat() / progress.totalOperations.toFloat()).coerceIn(
                                        0f,
                                        1f
                                    )
                                } else {
                                    0f
                                }
                            } ?: 0f
                        )

                        syncStatus.uploading -> AppSyncState.Uploading
                        syncStatus.connecting -> AppSyncState.Connecting
                        syncStatus.anyError != null -> {
                            if (isNetworkException(syncStatus.anyError!!)) {
                                AppSyncState.NoNetwork
                            } else {
                                println(syncStatus.anyError.toString())
                                AppSyncState.Error(syncStatus.anyError.toString())
                            }
                        }

                        !hasNetwork -> AppSyncState.NoNetwork
                        else -> AppSyncState.Connecting
                    }
                }.collect { state ->
                    _syncState.value = state
                }
            }
        }
    }

    private suspend fun connect() {
        connectionMutex.withLock {
            if (isConnected) return@withLock
            try {
                database.connect(connector, params = ConnectParams, options = ConnectOptions)
                isConnected = true
            } catch (error: CancellationException) {
                throw error
            } catch (e: Exception) {
                println("PowerSync connect failed: ${e.message}")
            }
        }
    }

    private suspend fun disconnect() {
        connectionMutex.withLock {
            if (!isConnected) return@withLock
            try {
                database.disconnect()
                isConnected = false
            } catch (error: CancellationException) {
                throw error
            } catch (e: Exception) {
                println("PowerSync disconnect failed: ${e.message}")
            }
        }
    }

    suspend fun cleanup(clear: Boolean) = withContext(NonCancellable) {
        lifecycleMutex.withLock {
            workerScope?.cancel()
            workerScope = null
            runCatching { connectivity.stop() }
                .onFailure { error -> println("Connectivity cleanup failed: ${error.message}") }
            connectionMutex.withLock {
                try {
                    if (clear) {
                        database.disconnectAndClear(clearLocal = true, soft = true)
                    } else if (isConnected) {
                        database.disconnect()
                    }
                } catch (e: Exception) {
                    println("PowerSync cleanup failed: ${e.message}")
                } finally {
                    isConnected = false
                }
            }
        }
    }
}
