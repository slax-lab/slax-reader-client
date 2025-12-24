package com.slax.reader.data.database.dao

import com.powersync.PowerSyncDatabase
import com.powersync.db.getStringOptional
import com.slax.reader.data.database.model.UserSubscriptionInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*

class SubscriptionDao (
    private val scope: CoroutineScope,
    private val database: PowerSyncDatabase
) {
    private val _subscriptionInfoFlow: StateFlow<UserSubscriptionInfo?> by lazy {
        println("[watch][database] _userSubscribeInfoFlow")

        database.watch("""
          SELECT * FROM sr_user_subscription LIMIT 1
        """.trimIndent(),
            parameters = listOf(),
            mapper = { cursor ->
                UserSubscriptionInfo(
                    stripe_subscription_id = cursor.getStringOptional("stripe_subscription_id") ?: "",
                    stripe_customer_id = cursor.getStringOptional("stripe_customer_id") ?: "",
                    stripe_currency = cursor.getStringOptional("stripe_stripe_currency") ?: "",
                    first_subscription_time = cursor.getStringOptional("first_subscription_time") ?: "",
                    subscription_end_time = cursor.getStringOptional("subscription_end_time") ?: "",
                    next_invoice_time = cursor.getStringOptional("next_invoice_time") ?: "",
                    auto_renew = cursor.getStringOptional("auto_renew")?.toIntOrNull() ?: 0,
                    stripe_credit = cursor.getStringOptional("stripe_credit")?.toIntOrNull() ?: 0,
                    subscribed = cursor.getStringOptional("subscribed")?.toIntOrNull() ?: 0,
                    apple_original_transaction_id = cursor.getStringOptional("apple_original_transaction_id") ?: "",
                    source_type = cursor.getStringOptional("source_type") ?: "",
                )
            }
        ).map { it.firstOrNull() }
            .catch { e ->
                println("Error watching user info: ${e.message}")
            }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.WhileSubscribed(5000), null)
    }

    fun watchSubscriptionInfo(): StateFlow<UserSubscriptionInfo?> = _subscriptionInfoFlow
}