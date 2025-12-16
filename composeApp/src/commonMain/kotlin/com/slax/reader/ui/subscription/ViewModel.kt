package com.slax.reader.ui.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slax.reader.data.network.ApiService
import com.slax.reader.utils.IAPCallback
import com.slax.reader.utils.IAPManager
import com.slax.reader.utils.IAPProduct
import com.slax.reader.utils.PurchaseResult
import kotlinx.coroutines.delay
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

class SubscriptionViewModel(private val apiService: ApiService) : ViewModel() {
    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Loading)
    val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()
    private val paymentManager = IAPManager()

    inner class SubscriptionCallback() : IAPCallback {
        override fun onProductsLoaded(products: List<IAPProduct>) {
            println("===== onProductsLoaded: $products")
            _paymentState.value = PaymentState.Idle
        }

        override fun onLoadFailed(error: String) {
            println("===== onLoadFailed: $error")
            _paymentState.value = PaymentState.Error(error)
        }

        @OptIn(ExperimentalUuidApi::class)
        override fun onPurchaseResult(result: PurchaseResult) {
            println("===== onPurchaseResult: $result")
            when {
                result.success -> {
                    if (result.transactionId == null || result.appAccountToken == null) {
                        _paymentState.value = PaymentState.Error("Missing transaction ID")
                        return
                    }
                    _paymentState.value = PaymentState.Checking(result.transactionId)
                    startCheckTransactionStatus(result.productId, result.appAccountToken.toString())
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
            println("===== onEntitlementsUpdated: $productIds")
        }

        fun startCheckTransactionStatus(productId: String, ticketId: String) {
            viewModelScope.launch {
                repeat(10) {
                    try {
                        val result = apiService.checkIapResult(ticketId, productId)
                        if (result.data?.ok == true) {
                            _paymentState.value = PaymentState.Success
                            return@launch
                        }
                        delay(1000L)
                    } catch (e: Exception) {
                        _paymentState.value = PaymentState.Error(e.message ?: "Failed to verify transaction")
                        return@launch
                    }
                }
                _paymentState.value = PaymentState.Error("Transaction verification timed out")
            }
        }
    }

    init {
        val callback = SubscriptionCallback()
        paymentManager.setCallback(callback)
        paymentManager.loadProducts(listOf("app.slax.reader.monthly"))
    }

    @OptIn(ExperimentalUuidApi::class)
   fun purchase() {
        _paymentState.value = PaymentState.Purchasing
        try {
            val orderInfo = runBlocking {
                apiService.createIapOrderId("app.slax.reader.monthly")
            }
            val orderId = orderInfo.data?.orderId
            if (orderId == null) {
                _paymentState.value = PaymentState.Error("Failed to create order: ${orderInfo.message}")
                return
            }
            paymentManager.purchase("app.slax.reader.monthly", orderId)
        } catch (e: Exception) {
            _paymentState.value = PaymentState.Error(e.message ?: "Failed to create order")
        }
    }

    fun resetState() {
        _paymentState.value = PaymentState.Idle
    }
}