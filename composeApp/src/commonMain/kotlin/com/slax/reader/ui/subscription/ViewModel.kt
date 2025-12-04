package com.slax.reader.ui.subscription

import androidx.lifecycle.ViewModel
import com.slax.reader.utils.IAPCallback
import com.slax.reader.utils.IAPManager
import com.slax.reader.utils.IAPProduct
import com.slax.reader.utils.PurchaseResult

class SubscriptionViewModel() : ViewModel() {
     fun purchase() {
        val manager = IAPManager()
        val callback = SubscriptionCallback()
        manager.setCallback(callback)
        manager.loadProducts(listOf("app.slax.reader.monthly"))
        manager.purchase("app.slax.reader.monthly")
    }
}

class SubscriptionCallback : IAPCallback {
    override fun onProductsLoaded(products: List<IAPProduct>) {
        println("===== onProductsLoaded: $products")
    }

    override fun onLoadFailed(error: String) {
        println("===== onLoadFailed: $error")
    }

    override fun onPurchaseResult(result: PurchaseResult) {
        println("===== onPurchaseResult: $result")
    }

    override fun onEntitlementsUpdated(productIds: List<String>) {
        println("===== onEntitlementsUpdated: $productIds")
    }
}