package com.slax.reader.utils

interface IapGateway {
    fun setCallback(callback: IAPCallback?)
    fun loadProducts(productIds: List<String>)
    fun purchase(productId: String, orderId: String)
    fun purchaseWithOffer(productId: String, orderId: String, offer: IAPProductOffer)
}

class PlatformIapGateway(
    private val manager: IAPManager = IAPManager()
) : IapGateway {
    override fun setCallback(callback: IAPCallback?) = manager.setCallback(callback)
    override fun loadProducts(productIds: List<String>) = manager.loadProducts(productIds)
    override fun purchase(productId: String, orderId: String) = manager.purchase(productId, orderId)
    override fun purchaseWithOffer(productId: String, orderId: String, offer: IAPProductOffer) =
        manager.purchaseWithOffer(productId, orderId, offer)
}
