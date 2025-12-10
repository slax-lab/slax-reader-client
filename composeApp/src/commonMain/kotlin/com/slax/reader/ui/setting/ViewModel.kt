package com.slax.reader.ui.setting

import androidx.lifecycle.ViewModel
import com.slax.reader.const.AppError
import com.slax.reader.data.network.ApiService
import com.slax.reader.data.network.dto.DeleteAccountData
import com.slax.reader.data.network.dto.DeleteAccountReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class DeleteAccountState {
    data object Idle : DeleteAccountState()

    data object Loading : DeleteAccountState()

    data object Success : DeleteAccountState()

    data class Error(val message: String) : DeleteAccountState()
}

class SettingViewModel(private val apiService: ApiService) : ViewModel() {
    private val _deleteAccountState = MutableStateFlow<DeleteAccountState>(DeleteAccountState.Idle)
    val deleteAccountState: StateFlow<DeleteAccountState> = _deleteAccountState.asStateFlow()

    /**
     * 删除账号
     */
    suspend fun deleteAccount() {
        _deleteAccountState.value = DeleteAccountState.Loading

        try {
            val result = apiService.deleteAccount()
            val deleteData = result.data!!
            processDeleteAccountData(deleteData)
        } catch (e: Exception) {
            if (e is AppError.ApiException.HttpError && e.data is DeleteAccountData) {
                processDeleteAccountData(e.data)
            } else {
                _deleteAccountState.value = DeleteAccountState.Error(e.message ?: "删除账号时发生未知错误")
            }
        }
    }

    private fun processDeleteAccountData(deleteData: DeleteAccountData) {
        // 检查是否可以删除
        if (deleteData.canDelete) {
            _deleteAccountState.value = DeleteAccountState.Success
        } else {
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
    }

    fun resetState() {
        _deleteAccountState.value = DeleteAccountState.Idle
    }
}