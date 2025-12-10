package com.slax.reader.ui.setting

import androidx.lifecycle.ViewModel
import com.slax.reader.const.AppError
import com.slax.reader.data.network.ApiService
import com.slax.reader.data.network.dto.DeleteAccountReason
import com.slax.reader.domain.auth.AuthDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class SettingViewModel(
    private val apiService: ApiService,
    private val authDomain: AuthDomain
) : ViewModel() {

    /**
     * 删除账号
     * @param onLoading 加载状态回调
     * @param onSuccess 删除成功回调
     * @param onError 删除失败回调（包含错误消息）
     */
    suspend fun deleteAccount(
        onLoading: (isLoading: Boolean) -> Unit,
        onSuccess: () -> Unit,
        onError: (err: String) -> Unit
    ) {
        withContext(Dispatchers.Main) { onLoading(true) }

        try {
            val result = withContext(Dispatchers.IO) {
                apiService.deleteAccount()
            }

            // 检查 HTTP 响应码
            if (result.code == 200) {
                val deleteData = result.data
                if (deleteData == null) {
                    withContext(Dispatchers.Main) { onError("服务器响应数据为空") }
                    return
                }

                // 检查是否可以删除
                if (deleteData.canDelete) {
                    // 删除成功后，清除本地认证状态
                    withContext(Dispatchers.IO) {
                        authDomain.signOut()
                    }
                    withContext(Dispatchers.Main) { onSuccess() }
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
                    withContext(Dispatchers.Main) { onError(errorMessage) }
                }
            } else {
                val errorMessage = result.message.ifEmpty { "删除账号失败 (${result.code})" }
                withContext(Dispatchers.Main) { onError(errorMessage) }
            }
        } catch (e: Exception) {
            val message = when (e) {
                is AppError.ApiException.HttpError -> {
                    "删除失败 (${e.code}): ${e.message}"
                }
                is AppError.AuthException -> {
                    "认证错误: ${e.message}"
                }
                else -> {
                    e.message ?: "删除账号时发生未知错误"
                }
            }
            withContext(Dispatchers.Main) { onError(message) }
        } finally {
            withContext(Dispatchers.Main) { onLoading(false) }
        }
    }
}