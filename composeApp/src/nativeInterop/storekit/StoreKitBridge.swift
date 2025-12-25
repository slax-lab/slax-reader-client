import Foundation
import StoreKit

// MARK: - 产品信息
@objc public class SKProductInfo: NSObject {
    @objc public var id: String = ""
    @objc public var displayName: String = ""
    @objc public var displayPrice: String = ""
    @objc public var productDescription: String = ""
    @objc public var priceValue: Double = 0.0
    @objc public var productType: String = ""

    @objc public override init() {
        super.init()
    }

    @objc public init(
        id: String,
        displayName: String,
        displayPrice: String,
        productDescription: String,
        priceValue: Double,
        productType: String
    ) {
        self.id = id
        self.displayName = displayName
        self.displayPrice = displayPrice
        self.productDescription = productDescription
        self.priceValue = priceValue
        self.productType = productType
        super.init()
    }
}

// MARK: - 购买结果
@objc public class SKPurchaseResult: NSObject {
    @objc public var success: Bool = false
    @objc public var productId: String = ""
    @objc public var transactionId: String = ""
    @objc public var appAccountToken: UUID?
    @objc public var errorMessage: String = ""
    @objc public var isPending: Bool = false
    @objc public var isCancelled: Bool = false
    @objc public var jwsRepresentation: String = ""

    @objc public override init() {
        super.init()
    }

    @objc public init(
        success: Bool,
        productId: String,
        transactionId: String,
        appAccountToken: UUID,
        errorMessage: String,
        isPending: Bool,
        isCancelled: Bool,
        jwsRepresentation: String = ""
    ) {
        self.success = success
        self.productId = productId
        self.transactionId = transactionId
        self.appAccountToken = appAccountToken
        self.errorMessage = errorMessage
        self.isPending = isPending
        self.isCancelled = isCancelled
        self.jwsRepresentation = jwsRepresentation
        super.init()
    }
}

// MARK: - Offer 信息
@objc public class SKOfferInfo: NSObject {
    @objc public var offerIdentifier: String = ""
    @objc public var keyIdentifier: String = ""
    @objc public var nonce: UUID = UUID()
    @objc public var signature: String = ""
    @objc public var timestamp: Int = 0

    @objc public override init() {
        super.init()
    }

    @objc public init(
        offerIdentifier: String,
        keyIdentifier: String,
        nonce: UUID,
        signature: String,
        timestamp: Int
    ) {
        self.offerIdentifier = offerIdentifier
        self.keyIdentifier = keyIdentifier
        self.nonce = nonce
        self.signature = signature
        self.timestamp = timestamp
        super.init()
    }
}

// MARK: - 回调协议
@objc public protocol StoreKitCallback {
    @objc func onProductsLoaded(_ products: [SKProductInfo])
    @objc func onProductsLoadFailed(_ error: String)
    @objc func onPurchaseResult(_ result: SKPurchaseResult)
    @objc func onEntitlementsUpdated(_ productIds: [String])
}

// MARK: - StoreKit 桥接主类
@available(iOS 15.0, macOS 12.0, *)
@objc(StoreKitBridge)
public class StoreKitBridge: NSObject {

    @objc public static let shared = StoreKitBridge()
    @objc public weak var callback: StoreKitCallback?

    private var products: [String: Product] = [:]
    private var purchasedIds: Set<String> = []
    private var transactionTask: Task<Void, Never>?

    private override init() {
        super.init()
        startTransactionListener()
    }

    deinit {
        transactionTask?.cancel()
    }

    // MARK: - 交易监听
    private func startTransactionListener() {
        transactionTask = Task { [weak self] in
            for await result in Transaction.updates {
                await self?.handleTransaction(result)
            }
        }
    }

    @MainActor
    private func handleTransaction(_ result: VerificationResult<Transaction>) async {
        guard case .verified(let transaction) = result else { return }

        if transaction.revocationDate == nil {
            purchasedIds.insert(transaction.productID)
        } else {
            purchasedIds.remove(transaction.productID)
        }

        await transaction.finish()
        await refreshEntitlementsInternal()
    }

    // MARK: - 公开 API
    @objc public func loadProducts(_ productIds: [String]) {
        Task {
            do {
                let storeProducts = try await Product.products(for: Set(productIds))

                await MainActor.run {
                    self.products.removeAll()
                    for product in storeProducts {
                        self.products[product.id] = product
                    }

                    let infos = storeProducts.map { self.convertProduct($0) }
                    self.callback?.onProductsLoaded(infos)
                }
            } catch {
                await MainActor.run {
                    self.callback?.onProductsLoadFailed(error.localizedDescription)
                }
            }
        }
    }

    @objc public func getProducts() -> [SKProductInfo] {
        return products.values.map { convertProduct($0) }
    }

    @objc public func purchase(_ productId: String, appAccountToken: UUID) {
        Task {
            guard let product = products[productId] else {
                await MainActor.run {
                    let result = SKPurchaseResult(
                        success: false,
                        productId: productId,
                        transactionId: "",
                        appAccountToken: appAccountToken,
                        errorMessage: "Product not found: \(productId)",
                        isPending: false,
                        isCancelled: false
                    )
                    self.callback?.onPurchaseResult(result)
                }
                return
            }

            do {
                let purchaseResult = try await product.purchase(options: [
                    .appAccountToken(appAccountToken)
                ])

                await MainActor.run {
                    switch purchaseResult {
                    case .success(let verification):
                        switch verification {
                        case .verified(let transaction):
                            self.purchasedIds.insert(productId)
                            Task { await transaction.finish() }

                            let result = SKPurchaseResult(
                                success: true,
                                productId: productId,
                                transactionId: String(transaction.id),
                                appAccountToken: transaction.appAccountToken ?? appAccountToken,
                                errorMessage: "",
                                isPending: false,
                                isCancelled: false,
                                jwsRepresentation: verification.jwsRepresentation
                            )
                            self.callback?.onPurchaseResult(result)

                        case .unverified(_, let verificationError):
                            let result = SKPurchaseResult(
                                success: false,
                                productId: productId,
                                transactionId: "",
                                appAccountToken: appAccountToken,
                                errorMessage: "Verification failed: \(verificationError.localizedDescription)",
                                isPending: false,
                                isCancelled: false
                            )
                            self.callback?.onPurchaseResult(result)
                        }

                    case .pending:
                        let result = SKPurchaseResult(
                            success: false,
                            productId: productId,
                            transactionId: "",
                            appAccountToken: appAccountToken,
                            errorMessage: "",
                            isPending: true,
                            isCancelled: false
                        )
                        self.callback?.onPurchaseResult(result)

                    case .userCancelled:
                        let result = SKPurchaseResult(
                            success: false,
                            productId: productId,
                            transactionId: "",
                            appAccountToken: appAccountToken,
                            errorMessage: "",
                            isPending: false,
                            isCancelled: true
                        )
                        self.callback?.onPurchaseResult(result)

                    @unknown default:
                        let result = SKPurchaseResult(
                            success: false,
                            productId: productId,
                            transactionId: "",
                            appAccountToken: appAccountToken,
                            errorMessage: "Unknown result",
                            isPending: false,
                            isCancelled: false
                        )
                        self.callback?.onPurchaseResult(result)
                    }
                }
            } catch {
                await MainActor.run {
                    let result = SKPurchaseResult(
                        success: false,
                        productId: productId,
                        transactionId: "",
                        appAccountToken: appAccountToken,
                        errorMessage: error.localizedDescription,
                        isPending: false,
                        isCancelled: false
                    )
                    self.callback?.onPurchaseResult(result)
                }
            }
        }
    }

    // MARK: - 带 Offer 的购买
    @objc public func purchaseWithOffer(_ productId: String, appAccountToken: UUID, offer: SKOfferInfo) {
        Task {
            guard let product = products[productId] else {
                await MainActor.run {
                    let result = SKPurchaseResult(
                        success: false,
                        productId: productId,
                        transactionId: "",
                        appAccountToken: appAccountToken,
                        errorMessage: "Product not found: \(productId)",
                        isPending: false,
                        isCancelled: false
                    )
                    self.callback?.onPurchaseResult(result)
                }
                return
            }

            do {
                guard let signatureData = Data(base64Encoded: offer.signature) else {
                    await MainActor.run {
                        let result = SKPurchaseResult(
                            success: false,
                            productId: productId,
                            transactionId: "",
                            appAccountToken: appAccountToken,
                            errorMessage: "Invalid signature format",
                            isPending: false,
                            isCancelled: false
                        )
                        self.callback?.onPurchaseResult(result)
                    }
                    return
                }

                let purchaseResult = try await product.purchase(options: [
                    .appAccountToken(appAccountToken),
                    .promotionalOffer(
                        offerID: offer.offerIdentifier,
                        keyID: offer.keyIdentifier,
                        nonce: offer.nonce,
                        signature: signatureData,
                        timestamp: offer.timestamp
                    )
                ])

                await MainActor.run {
                    switch purchaseResult {
                    case .success(let verification):
                        switch verification {
                        case .verified(let transaction):
                            self.purchasedIds.insert(productId)
                            Task { await transaction.finish() }

                            let result = SKPurchaseResult(
                                success: true,
                                productId: productId,
                                transactionId: String(transaction.id),
                                appAccountToken: transaction.appAccountToken ?? appAccountToken,
                                errorMessage: "",
                                isPending: false,
                                isCancelled: false,
                                jwsRepresentation: verification.jwsRepresentation
                            )
                            self.callback?.onPurchaseResult(result)

                        case .unverified(_, let verificationError):
                            let result = SKPurchaseResult(
                                success: false,
                                productId: productId,
                                transactionId: "",
                                appAccountToken: appAccountToken,
                                errorMessage: "Verification failed: \(verificationError.localizedDescription)",
                                isPending: false,
                                isCancelled: false
                            )
                            self.callback?.onPurchaseResult(result)
                        }

                    case .pending:
                        let result = SKPurchaseResult(
                            success: false,
                            productId: productId,
                            transactionId: "",
                            appAccountToken: appAccountToken,
                            errorMessage: "",
                            isPending: true,
                            isCancelled: false
                        )
                        self.callback?.onPurchaseResult(result)

                    case .userCancelled:
                        let result = SKPurchaseResult(
                            success: false,
                            productId: productId,
                            transactionId: "",
                            appAccountToken: appAccountToken,
                            errorMessage: "",
                            isPending: false,
                            isCancelled: true
                        )
                        self.callback?.onPurchaseResult(result)

                    @unknown default:
                        let result = SKPurchaseResult(
                            success: false,
                            productId: productId,
                            transactionId: "",
                            appAccountToken: appAccountToken,
                            errorMessage: "Unknown result",
                            isPending: false,
                            isCancelled: false
                        )
                        self.callback?.onPurchaseResult(result)
                    }
                }
            } catch {
                await MainActor.run {
                    let result = SKPurchaseResult(
                        success: false,
                        productId: productId,
                        transactionId: "",
                        appAccountToken: appAccountToken,
                        errorMessage: error.localizedDescription,
                        isPending: false,
                        isCancelled: false
                    )
                    self.callback?.onPurchaseResult(result)
                }
            }
        }
    }

    @objc public func restorePurchases() {
        Task {
            do {
                try await AppStore.sync()
                await refreshEntitlementsInternal()
            } catch {
                await MainActor.run {
                    self.callback?.onProductsLoadFailed("Restore failed: \(error.localizedDescription)")
                }
            }
        }
    }

    @objc public func refreshEntitlements() {
        Task {
            await refreshEntitlementsInternal()
        }
    }

    private func refreshEntitlementsInternal() async {
        var ids: Set<String> = []

        for await result in Transaction.currentEntitlements {
            if case .verified(let transaction) = result {
                if transaction.revocationDate == nil {
                    ids.insert(transaction.productID)
                }
            }
        }

        let finalIds = ids

        await MainActor.run {
            self.purchasedIds = finalIds
            self.callback?.onEntitlementsUpdated(Array(finalIds))
        }
    }

    @objc public func isPurchased(_ productId: String) -> Bool {
        return purchasedIds.contains(productId)
    }

    @objc public func getPurchasedIds() -> [String] {
        return Array(purchasedIds)
    }

    @objc public func canMakePayments() -> Bool {
        return SKPaymentQueue.canMakePayments()
    }

    // MARK: - Helper
    private func convertProduct(_ product: Product) -> SKProductInfo {
        let typeString: String
        switch product.type {
        case .consumable:
            typeString = "consumable"
        case .nonConsumable:
            typeString = "nonConsumable"
        case .autoRenewable:
            typeString = "autoRenewable"
        case .nonRenewable:
            typeString = "nonRenewable"
        default:
            typeString = "unknown"
        }

        return SKProductInfo(
            id: product.id,
            displayName: product.displayName,
            displayPrice: product.displayPrice,
            productDescription: product.description,
            priceValue: NSDecimalNumber(decimal: product.price).doubleValue,
            productType: typeString
        )
    }
}