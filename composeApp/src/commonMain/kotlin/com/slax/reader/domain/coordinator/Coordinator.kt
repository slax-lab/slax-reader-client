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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

sealed class AppSyncState {
    data object NoNetwork : AppSyncState()
    data object Connecting : AppSyncState()
    data class Downloading(val progress: Float?) : AppSyncState()
    data object Uploading : AppSyncState()
    data object Connected : AppSyncState()
    data class Error(val message: String) : AppSyncState()
}

internal fun hasNetworkConnection(status: Connectivity.Status): Boolean =
    status is Connectivity.Status.Connected

class CoordinatorDomain(
    private val database: PowerSyncDatabase,
    private val connector: Connector,
    private val powerSyncDao: PowerSyncDao
) : NetworkCoordinator {
    private var workerScope: CoroutineScope? = null

    private var isConnected = false
    private val connectivity = Connectivity()

    private val _syncState = MutableStateFlow<AppSyncState>(AppSyncState.Connecting)
    override val syncState: StateFlow<AppSyncState> = _syncState.asStateFlow()

    /**
     * Query the platform's current network status instead of inferring it from
     * the asynchronous sync state. This is important while the first
     * connectivity event is still being delivered during app startup.
     */
    override suspend fun isNetworkAvailable(): Boolean {
        return try {
            hasNetworkConnection(connectivity.status())
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Network checks are a gate for API requests; fail closed if the
            // platform provider cannot produce a status.
            false
        }
    }

    fun startup() {
        workerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        workerScope!!.launch {
            connect()
            connectivity.start()
        }

        workerScope!!.launch {
            connectivity.statusUpdates.collect { status ->
                if (status is Connectivity.Status.Connected) {
                    reconnect()
                } else {
                    disconnect()
                }
            }
        }

        workerScope!!.launch {
            combine(
                powerSyncDao.watchPowerSyncStatus(),
                connectivity.statusUpdates
            ) { syncStatus, networkStatus ->
                val hasNetwork = hasNetworkConnection(networkStatus)

                when {
                    !hasNetwork -> AppSyncState.NoNetwork
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

                    else -> AppSyncState.Connecting
                }
            }.collect { state ->
                _syncState.value = state
            }
        }
    }

    private suspend fun reconnect() {
        disconnect()
        connect()
    }

    private suspend fun connect() {
        if (isConnected) return
        try {
            database.connect(connector, params = ConnectParams, options = ConnectOptions)
            isConnected = true
        } catch (e: Exception) {
            println("PowerSync connect failed: ${e.message}")
        }
    }

    private suspend fun disconnect() {
        if (!isConnected) return
        try {
            database.disconnect()
            isConnected = false
        } catch (e: Exception) {
            println("PowerSync disconnect failed: ${e.message}")
        }
    }

    suspend fun cleanup(clear: Boolean) {
        workerScope?.cancel()
        workerScope = null
        if (isConnected) {
            connectivity.stop()
            database.disconnectAndClear(clearLocal = clear, soft = true)
            isConnected = false
        }
    }
}
