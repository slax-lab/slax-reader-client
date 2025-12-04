package com.slax.reader.utils

data class IAPProduct(
    val id: String,
    val displayName: String,
    val displayPrice: String,
    val description: String,
    val price: Double,
    val type: ProductType
)

enum class ProductType { CONSUMABLE, NON_CONSUMABLE, AUTO_RENEWABLE, NON_RENEWABLE, UNKNOWN }

data class PurchaseResult(
    val success: Boolean,
    val productId: String,
    val transactionId: String? = null,
    val error: String? = null,
    val isPending: Boolean = false,
    val isCancelled: Boolean = false
)

interface IAPCallback {
    fun onProductsLoaded(products: List<IAPProduct>)
    fun onLoadFailed(error: String)
    fun onPurchaseResult(result: PurchaseResult)
    fun onEntitlementsUpdated(productIds: List<String>)
}

expect class IAPManager() {
    fun setCallback(callback: IAPCallback?)
    fun loadProducts(productIds: List<String>)
    fun purchase(productId: String)
    fun restorePurchases()
    fun isPurchased(productId: String): Boolean
    fun getPurchasedIds(): List<String>
    fun canMakePayments(): Boolean
    fun getProducts(): List<IAPProduct>
}