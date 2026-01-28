package com.slax.reader.data.database.model

import androidx.compose.runtime.Immutable
import com.slax.reader.utils.parseInstant
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime

@Immutable
@Serializable
data class UserSubscriptionInfo(
    val stripe_subscription_id: String,
    val stripe_customer_id: String,
    val stripe_currency: String,
    val first_subscription_time: String,
    val subscription_end_time: String,
    val next_invoice_time: String,
    val auto_renew: Int,
    val stripe_credit: Int,
    val subscribed: Int,
    val apple_original_transaction_id: String,
    val source_type: String,
)

@OptIn(ExperimentalTime::class)
fun UserSubscriptionInfo.checkIsSubscribed(): Boolean {
    return try {
        val endTime = parseInstant(subscription_end_time)
        val now = kotlin.time.Clock.System.now()
        endTime > now
    } catch (e: Exception) {
        println("Error checking subscription: ${e.message}")
        false
    }
}
