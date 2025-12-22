package com.slax.reader.utils

import app.slax.reader.storekit.SKProductInfo
import app.slax.reader.storekit.SKPurchaseResult
import app.slax.reader.storekit.StoreKitBridge
import app.slax.reader.storekit.StoreKitCallbackProtocol
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSUUID
import platform.darwin.NSObject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalForeignApi::class)
actual class IAPManager actual constructor() {
    private val bridge = StoreKitBridge.shared()
    private var callback: IAPCallback? = null

    private val swiftCallback = object : NSObject(), StoreKitCallbackProtocol {

        override fun onProductsLoaded(products: List<*>) {
            val infos = products.filterIsInstance<SKProductInfo>()
            val iapProducts = infos.map { it.toIAPProduct() }
            callback?.onProductsLoaded(iapProducts)
        }

        override fun onProductsLoadFailed(error: String) {
            callback?.onLoadFailed(error)
        }

        override fun onPurchaseResult(result: SKPurchaseResult) {
            callback?.onPurchaseResult(result.toPurchaseResult())
        }

        override fun onEntitlementsUpdated(productIds: List<*>) {
            val ids = productIds.filterIsInstance<String>()
            callback?.onEntitlementsUpdated(ids)
        }
    }

    init {
        bridge.setCallback(swiftCallback)
    }

    actual fun setCallback(callback: IAPCallback?) {
        this.callback = callback
    }

    actual fun loadProducts(productIds: List<String>) {
        bridge.loadProducts(productIds)
    }

    @OptIn(ExperimentalUuidApi::class)
    actual fun purchase(productId: String, orderId: String) {
        bridge.purchase(productId, NSUUID(orderId))
    }

    actual fun restorePurchases() {
        bridge.restorePurchases()
    }

    actual fun isPurchased(productId: String): Boolean {
        return bridge.isPurchased(productId)
    }

    actual fun getPurchasedIds(): List<String> {
        val ids = bridge.getPurchasedIds()
        return ids.filterIsInstance<String>()
    }

    actual fun canMakePayments(): Boolean {
        return bridge.canMakePayments()
    }

    actual fun getProducts(): List<IAPProduct> {
        val infos = bridge.getProducts()
        return infos.filterIsInstance<SKProductInfo>().map { it.toIAPProduct() }
    }

    actual fun purchaseWithOffer(productId: String, orderId: String, offer: IAPProductOffer) {
        bridge.purchaseWithOffer(
            productId, NSUUID(orderId),
            offerId = offer.offerId,
            keyID = offer.keyID,
            nonce = NSUUID(offer.nonce),
            signature = offer.signature,
            timestamp = offer.timestamp.toLong()
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun SKProductInfo.toIAPProduct() = IAPProduct(
    id = this.id(),
    displayName = this.displayName(),
    displayPrice = this.displayPrice(),
    description = this.productDescription(),
    price = this.priceValue(),
    type = when (this.productType()) {
        "consumable" -> ProductType.CONSUMABLE
        "nonConsumable" -> ProductType.NON_CONSUMABLE
        "autoRenewable" -> ProductType.AUTO_RENEWABLE
        "nonRenewable" -> ProductType.NON_RENEWABLE
        else -> ProductType.UNKNOWN
    }
)

@OptIn(ExperimentalForeignApi::class, ExperimentalUuidApi::class)
private fun SKPurchaseResult.toPurchaseResult() = PurchaseResult(
    success = this.success(),
    productId = this.productId(),
    transactionId = this.transactionId().takeIf { it.isNotEmpty() },
    appAccountToken = this.appAccountToken()?.let { Uuid.parse(it.UUIDString()) },
    error = this.errorMessage().takeIf { it.isNotEmpty() },
    isPending = this.isPending(),
    isCancelled = this.isCancelled()
)