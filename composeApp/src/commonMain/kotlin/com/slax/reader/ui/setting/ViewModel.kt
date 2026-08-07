package com.slax.reader.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slax.reader.data.network.ApiService
import com.slax.reader.data.network.dto.DeleteAccountReason
import com.slax.reader.data.preferences.AppPreferences
import com.slax.reader.domain.cache.CacheCategory
import com.slax.reader.domain.cache.CacheManager
import com.slax.reader.domain.cache.CacheUsage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class DeleteAccountState {
    data object Idle : DeleteAccountState()

    data object Loading : DeleteAccountState()

    data object Success : DeleteAccountState()

    data class Error(val message: String) : DeleteAccountState()
}

sealed class ClearCacheState {
    data object Idle : ClearCacheState()

    data object Clearing : ClearCacheState()

    data class Done(val freedBytes: Long) : ClearCacheState()
}

class SettingViewModel(
    private val apiService: ApiService,
    private val appPreferences: AppPreferences,
    private val cacheManager: CacheManager
) : ViewModel() {
    private val _deleteAccountState = MutableStateFlow<DeleteAccountState>(DeleteAccountState.Idle)
    val deleteAccountState: StateFlow<DeleteAccountState> = _deleteAccountState.asStateFlow()

    private val _cacheUsage = MutableStateFlow(CacheUsage())
    val cacheUsage: StateFlow<CacheUsage> = _cacheUsage.asStateFlow()

    private val _clearCacheState = MutableStateFlow<ClearCacheState>(ClearCacheState.Idle)
    val clearCacheState: StateFlow<ClearCacheState> = _clearCacheState.asStateFlow()
    private var cacheRefreshJob: Job? = null

    val cacheCount: StateFlow<Int> = appPreferences.getCacheCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 50)

    val downloadImages: StateFlow<Boolean> = appPreferences.getDownloadImages()
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    init {
        refreshCacheSize()
    }

    fun refreshCacheSize() {
        cacheRefreshJob?.cancel()
        cacheRefreshJob = viewModelScope.launch {
            _cacheUsage.value = cacheManager.clearableCacheUsage()
        }
    }

    fun clearCache(categories: Set<CacheCategory>) {
        if (categories.isEmpty()) return
        if (_clearCacheState.value is ClearCacheState.Clearing) return
        cacheRefreshJob?.cancel()
        _clearCacheState.value = ClearCacheState.Clearing
        viewModelScope.launch {
            val freed = cacheManager.clearCache(categories)
            _cacheUsage.value = cacheManager.clearableCacheUsage()
            _clearCacheState.value = ClearCacheState.Done(freed)
        }
    }

    fun acknowledgeClearCache() {
        _clearCacheState.value = ClearCacheState.Idle
    }

    fun updateCacheCount(count: Int) {
        viewModelScope.launch {
            appPreferences.setCacheCount(count)
        }
    }

    fun updateDownloadImages(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setDownloadImages(enabled)
        }
    }

    /**
     * 删除账号
     */
    suspend fun deleteAccount() {
        _deleteAccountState.value = DeleteAccountState.Loading

        try {
            val result = apiService.deleteAccount()
            val deleteData = result.data!!

            // 检查是否可以删除
            if (deleteData.canDelete) {
                _deleteAccountState.value = DeleteAccountState.Success
            } else {
                // 不能删除，根据原因生成友好的错误消息
                val errorMessage = when (deleteData.reason) {
                    DeleteAccountReason.ACTIVE_SUBSCRIPTION ->
                        "无法删除账号：您有正在进行的订阅，请先取消订阅后再试"
                    DeleteAccountReason.STRIPE_CONNECT_EXISTS ->
                        "无法删除账号：您的账号关联了 Stripe Connect，请先解除关联"
                    DeleteAccountReason.COLLECTION_HAS_SUBSCRIBERS ->
                        "无法删除账号：您的合集有订阅者，请先处理相关合集"
                    DeleteAccountReason.ACTIVE_COLLECTION_SUBSCRIPTION ->
                        "无法删除账号：您订阅了某些合集，请先取消订阅"
                    DeleteAccountReason.HAS_FREE_SUBSCRIPTION_HISTORY ->
                        "无法删除账号：您有免费订阅记录，请联系客服处理"
                    null ->
                        "无法删除账号，原因未知"
                }

                _deleteAccountState.value = DeleteAccountState.Error(errorMessage)
            }
        } catch (e: Exception) {
            _deleteAccountState.value = DeleteAccountState.Error(e.message ?: "删除账号时发生未知错误")
        }
    }

    fun resetState() {
        _deleteAccountState.value = DeleteAccountState.Idle
    }
}
