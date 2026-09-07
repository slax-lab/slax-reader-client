package com.slax.reader.data.network

import com.slax.reader.data.network.dto.CheckIapParam
import com.slax.reader.data.network.dto.CheckIapResult
import com.slax.reader.data.network.dto.DeleteAccountData
import com.slax.reader.data.network.dto.HttpData
import com.slax.reader.data.network.dto.ProductIdsResult
import com.slax.reader.data.network.dto.OutlineResponse
import com.slax.reader.data.network.dto.OverviewResponse
import com.slax.reader.data.network.dto.FeedbackParams
import kotlinx.coroutines.flow.Flow

interface FeedbackApi {
    suspend fun sendFeedback(param: FeedbackParams): HttpData<String>
}

interface AccountApi {
    suspend fun deleteAccount(): HttpData<DeleteAccountData>
}

interface PaymentApi {
    suspend fun getIAPProductIds(): HttpData<ProductIdsResult>
    suspend fun checkIapResult(param: CheckIapParam): HttpData<CheckIapResult>
}

interface BookmarkAiApi {
    fun getBookmarkOutline(bookmarkId: String): Flow<OutlineResponse>
    fun getBookmarkOverview(bookmarkId: String): Flow<OverviewResponse>
    suspend fun sendMetrics(type: MetricsType)
}
