package com.slax.reader.ui.subscription

import androidx.lifecycle.ViewModel
import com.slax.reader.utils.IAPCallback
import com.slax.reader.utils.IAPManager
import com.slax.reader.utils.IAPProduct
import com.slax.reader.utils.PurchaseResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class PaymentState {
    data object Idle : PaymentState()
    data object Loading : PaymentState()
    data object Purchasing : PaymentState()

    data class Checking(val transactionId: String): PaymentState()

    data object Cancelled : PaymentState()

    data object Success : PaymentState()

    data class Error(val message: String) : PaymentState()
}

class SubscriptionViewModel() : ViewModel() {
    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Loading)
    val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()
    private val paymentManager = IAPManager()

    inner class SubscriptionCallback : IAPCallback {
        override fun onProductsLoaded(products: List<IAPProduct>) {
            println("===== onProductsLoaded: $products")
            _paymentState.value = PaymentState.Idle
        }

        override fun onLoadFailed(error: String) {
            println("===== onLoadFailed: $error")
            _paymentState.value = PaymentState.Error(error)
        }

        override fun onPurchaseResult(result: PurchaseResult) {
            println("===== onPurchaseResult: $result")
            when {
                result.success -> _paymentState.value = PaymentState.Purchasing
                result.isPending -> _paymentState.value = PaymentState.Purchasing
                result.isCancelled -> _paymentState.value = PaymentState.Cancelled
                result.error != null -> _paymentState.value = PaymentState.Error(result.error)
                result.transactionId != null -> _paymentState.value = PaymentState.Checking(result.transactionId)
                else -> {
                    _paymentState.value = PaymentState.Error("Unknown error")
                }
            }
        }

        override fun onEntitlementsUpdated(productIds: List<String>) {
            println("===== onEntitlementsUpdated: $productIds")
        }
    }

    init {
        val callback = SubscriptionCallback()
        paymentManager.setCallback(callback)
        paymentManager.loadProducts(listOf("app.slax.reader.monthly"))
    }

    fun purchase() {
        _paymentState.value = PaymentState.Purchasing
        paymentManager.purchase("app.slax.reader.monthly")
    }

    fun resetState() {
        _paymentState.value = PaymentState.Idle
    }
}