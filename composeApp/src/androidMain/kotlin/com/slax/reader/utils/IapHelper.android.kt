package com.slax.reader.utils

actual class IAPManager actual constructor() {
    actual fun setCallback(callback: IAPCallback?) {
        TODO("Not yet implemented")
    }

    actual fun loadProducts(productIds: List<String>) {
        TODO("Not yet implemented")
    }

    actual fun purchase(productId: String, orderId: String) {
        TODO("Not yet implemented")
    }

    actual fun restorePurchases() {
        TODO("Not yet implemented")
    }

    actual fun isPurchased(productId: String): Boolean {
        TODO("Not yet implemented")
    }

    actual fun getPurchasedIds(): List<String> {
        TODO("Not yet implemented")
    }

    actual fun canMakePayments(): Boolean {
        TODO("Not yet implemented")
    }

    actual fun getProducts(): List<IAPProduct> {
        TODO("Not yet implemented")
    }

    actual fun purchaseWithOffer(productId: String, orderId: String, offer: IAPProductOffer) {
        TODO("Not yet implemented")
    }
}