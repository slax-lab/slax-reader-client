package com.slax.reader.domain.purchases

import app.slax.reader.SlaxConfig
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration
import com.revenuecat.purchases.kmp.configure

class PurchasesDomain() {

    fun init(userId: String) {
        Purchases.configure(SlaxConfig.REVENUE_CAT_API_KEY) {
            appUserId = userId
        }
    }
}