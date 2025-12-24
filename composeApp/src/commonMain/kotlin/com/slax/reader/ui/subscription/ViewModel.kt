package com.slax.reader.ui.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.slax.reader.SlaxConfig
import com.slax.reader.data.network.ApiService
import com.slax.reader.data.network.dto.CheckIapParam
import com.slax.reader.data.preferences.AppPreferences
import com.slax.reader.utils.IAPCallback
import com.slax.reader.utils.IAPManager
import com.slax.reader.utils.IAPProduct
import com.slax.reader.utils.IAPProductOffer
import com.slax.reader.utils.PurchaseResult
import com.slax.reader.utils.WebViewCookie
import com.slax.reader.utils.platformType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.uuid.ExperimentalUuidApi

sealed class PaymentState {
    data object Idle : PaymentState()
    data object Loading : PaymentState()
    data object Purchasing : PaymentState()

    data class Checking(val transactionId: String): PaymentState()

    data object Cancelled : PaymentState()

    data object Success : PaymentState()

    data class Error(val message: String) : PaymentState()
}

class SubscriptionViewModel(
    private val apiService: ApiService,
    private val appPreferences: AppPreferences
    ) : ViewModel() {
    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Loading)
    val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()
    private val paymentManager = IAPManager()

    inner class SubscriptionCallback() : IAPCallback {
        override fun onProductsLoaded(products: List<IAPProduct>) {
            _paymentState.value = PaymentState.Idle
        }

        override fun onLoadFailed(error: String) {
            _paymentState.value = PaymentState.Error(error)
        }

        @OptIn(ExperimentalUuidApi::class)
        override fun onPurchaseResult(result: PurchaseResult) {
            when {
                result.success -> {
                    if (result.transactionId == null || result.appAccountToken == null || result.jwsRepresentation == null) {
                        _paymentState.value = PaymentState.Error("Missing transaction ID")
                        return
                    }
                    result.isPending
                    _paymentState.value = PaymentState.Checking(result.jwsRepresentation)
                    startCheckTransactionStatus(result.productId, result.appAccountToken.toString(), result.jwsRepresentation)
                }
                result.isPending -> _paymentState.value = PaymentState.Purchasing
                result.isCancelled -> _paymentState.value = PaymentState.Cancelled
                result.error != null -> _paymentState.value = PaymentState.Error(result.error)
                else -> {
                    _paymentState.value = PaymentState.Error("Unknown error")
                }
            }
        }

        override fun onEntitlementsUpdated(productIds: List<String>) {
        }

        fun startCheckTransactionStatus(productId: String, orderId: String, jwsRepresentation: String) {
            viewModelScope.launch {
                try {
                    val result = apiService.checkIapResult(CheckIapParam(
                        product_id = productId,
                        order_id = orderId,
                        jws_representation = jwsRepresentation,
                        platform = platformType
                    ))
                    if (result.data?.ok == true) {
                        _paymentState.value = PaymentState.Success
                        return@launch
                    }
                } catch (e: Exception) {
                    println("===== checkTransactionStatus error: $e")
                    _paymentState.value = PaymentState.Error(e.message ?: "Transaction verification failed")
                }
                _paymentState.value = PaymentState.Error("Transaction verification failed")
            }
        }
    }

    init {
        val callback = SubscriptionCallback()
        paymentManager.setCallback(callback)
        viewModelScope.launch {
            val productIds = apiService.getIAPProductIds()
            paymentManager.loadProducts(productIds.data?.products ?: emptyList())
        }
    }

    @OptIn(ExperimentalUuidApi::class)
   fun purchase(productId: String, orderId: String, offer: IAPProductOffer? = null) {
        _paymentState.value = PaymentState.Purchasing
        try {
            if (offer != null) {
                paymentManager.purchaseWithOffer(productId, orderId, offer)
            } else {
                paymentManager.purchase(productId, orderId)
            }
        } catch (e: Exception) {
            _paymentState.value = PaymentState.Error(e.message ?: "Failed to create order")
        }
    }

    fun resetState() {
        _paymentState.value = PaymentState.Idle
    }

    fun getUserWebviewCookie() : List<WebViewCookie> {
        val token = runBlocking {
            appPreferences.getAuthInfoSuspend()
        }
        return listOf(
            WebViewCookie(
                name = "token",
                value = token!!,
                domain = SlaxConfig.WEB_DOMAIN,
                path = "/",
                httpOnly = true,
                secure = true
            )
        )
    }
}