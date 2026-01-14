package com.slax.reader.ui.sidebar

import androidx.lifecycle.ViewModel
import com.slax.reader.data.database.dao.SubscriptionDao
import com.slax.reader.data.database.dao.UserDao
import com.slax.reader.domain.coordinator.CoordinatorDomain
import com.slax.reader.utils.parseInstant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class SidebarViewModel(private val userDao: UserDao, coordinatorDomain: CoordinatorDomain, private val subscriptionDao: SubscriptionDao,
) : ViewModel() {
    val userInfo = userDao.watchUserInfo()
    val syncStatus = coordinatorDomain.syncState

    @OptIn(kotlin.time.ExperimentalTime::class)
    suspend fun checkUserIsSubscribed(): Boolean = withContext(Dispatchers.IO)  {
        val info = subscriptionDao.getSubscriptionInfo() ?: return@withContext false

        try {
            val endTime = parseInstant(info.subscription_end_time)
            val now = kotlin.time.Clock.System.now()
            endTime > now
        } catch (e: Exception) {
            println("Error checking subscription: ${e.message}")
            false
        }
    }
}