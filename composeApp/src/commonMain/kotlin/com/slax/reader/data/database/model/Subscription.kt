package com.slax.reader.data.database.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class UserSubscriptionInfo(
    val stripe_subscription_id: String,
    val stripe_customer_id: String,
    val stripe_currency: String,
    val first_subscription_time: Long,
    val subscription_end_time: Long,
    val next_invoice_time: Long,
    val auto_renew: Int,
    val stripe_credit: Int,
    val subscribed: Int,
    val apple_original_transaction_id: String,
    val source_type: String,
)