package com.slax.reader

import com.slax.reader.data.network.PaymentApi
import com.slax.reader.data.network.dto.CheckIapParam
import com.slax.reader.data.network.dto.CheckIapResult
import com.slax.reader.data.network.dto.HttpData
import com.slax.reader.data.network.dto.ProductIdsResult
import com.slax.reader.ui.subscription.PaymentState
import com.slax.reader.ui.subscription.SubscriptionViewModel
import com.slax.reader.utils.IAPProductOffer
import com.slax.reader.utils.PurchaseResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class, kotlin.uuid.ExperimentalUuidApi::class)
class SubscriptionViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var api: FakePaymentApi
    private lateinit var iap: FakeIapGateway

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        api = FakePaymentApi()
        iap = FakeIapGateway()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun product_load_moves_state_to_idle_and_purchase_paths_are_delegated() = runTest {
        val viewModel = SubscriptionViewModel(api, FakeSettingsPreferences(), iap)
        advanceUntilIdle()
        assertEquals(PaymentState.Idle, viewModel.paymentState.value)

        viewModel.purchase("monthly", "order")
        assertEquals(PaymentState.Purchasing, viewModel.paymentState.value)
        assertEquals(listOf("monthly:order"), iap.purchases)

        viewModel.purchase(
            "monthly",
            "order-2",
            IAPProductOffer("offer", "key", "nonce", "signature", 1L)
        )
        assertEquals(listOf("monthly:order-2:offer"), iap.offerPurchases)
    }

    @Test
    fun purchase_callback_covers_cancel_pending_missing_fields_and_verification() = runTest {
        val viewModel = SubscriptionViewModel(api, FakeSettingsPreferences(), iap)
        advanceUntilIdle()
        val callback = iap.registeredCallback!!

        callback.onPurchaseResult(PurchaseResult(false, "monthly", isCancelled = true))
        assertEquals(PaymentState.Cancelled, viewModel.paymentState.value)

        callback.onPurchaseResult(PurchaseResult(false, "monthly", isPending = true))
        assertEquals(PaymentState.Purchasing, viewModel.paymentState.value)

        callback.onPurchaseResult(PurchaseResult(true, "monthly"))
        assertEquals(PaymentState.Error("Missing transaction ID"), viewModel.paymentState.value)

        callback.onPurchaseResult(PurchaseResult(false, "monthly", error = "store error"))
        assertEquals(PaymentState.Error("store error"), viewModel.paymentState.value)

        callback.onPurchaseResult(
            PurchaseResult(
                success = true,
                productId = "monthly",
                transactionId = "tx",
                appAccountToken = kotlin.uuid.Uuid.random(),
                jwsRepresentation = "jws"
            )
        )
        assertTrue(viewModel.paymentState.value is PaymentState.Checking)
        advanceUntilIdle()
        assertEquals(PaymentState.Success, viewModel.paymentState.value)
    }

    @Test
    fun failed_verification_becomes_error_and_cookie_is_built_without_blocking() = runTest {
        api.checkResult = HttpData(CheckIapResult(false), "", 200)
        val prefs = FakeSettingsPreferences().also { it.token = "auth-token" }
        val viewModel = SubscriptionViewModel(api, prefs, FakeIapGateway())
        advanceUntilIdle()
        assertEquals("auth-token", viewModel.getUserWebviewCookie().single().value)
        val gateway = FakeIapGateway()
        val secondViewModel = SubscriptionViewModel(api, prefs, gateway)
        advanceUntilIdle()
        gateway.registeredCallback!!.onPurchaseResult(
            PurchaseResult(true, "monthly", "tx", kotlin.uuid.Uuid.random(), jwsRepresentation = "jws")
        )
        advanceUntilIdle()
        assertEquals(PaymentState.Error("Transaction verification failed"), secondViewModel.paymentState.value)
    }
}

private class FakePaymentApi(
    var checkResult: HttpData<CheckIapResult> = HttpData(CheckIapResult(true), "", 200),
    var checkError: Throwable? = null
) : PaymentApi {
    override suspend fun getIAPProductIds() =
        HttpData(ProductIdsResult(listOf("monthly")), "", 200)

    override suspend fun checkIapResult(param: CheckIapParam): HttpData<CheckIapResult> {
        checkError?.let { throw it }
        return checkResult
    }
}
