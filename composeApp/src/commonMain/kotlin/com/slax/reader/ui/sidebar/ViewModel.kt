package com.slax.reader.ui.sidebar

import androidx.lifecycle.ViewModel
import com.slax.reader.data.database.dao.SubscriptionRepository
import com.slax.reader.data.database.dao.UserRepository
import com.slax.reader.domain.coordinator.NetworkCoordinator

class SidebarViewModel(private val userDao: UserRepository, coordinatorDomain: NetworkCoordinator, private val subscriptionDao: SubscriptionRepository,
) : ViewModel() {
    val userInfo = userDao.watchUserInfo()
    val subscriptionInfo = subscriptionDao.watchSubscriptionInfo()
    val syncStatus = coordinatorDomain.syncState
}
