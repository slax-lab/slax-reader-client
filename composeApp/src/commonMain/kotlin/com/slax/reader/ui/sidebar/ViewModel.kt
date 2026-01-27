package com.slax.reader.ui.sidebar

import androidx.lifecycle.ViewModel
import com.slax.reader.data.database.dao.SubscriptionDao
import com.slax.reader.data.database.dao.UserDao
import com.slax.reader.domain.coordinator.CoordinatorDomain

class SidebarViewModel(private val userDao: UserDao, coordinatorDomain: CoordinatorDomain, private val subscriptionDao: SubscriptionDao,
) : ViewModel() {
    val userInfo = userDao.watchUserInfo()
    val subscriptionInfo = subscriptionDao.watchSubscriptionInfo()
    val syncStatus = coordinatorDomain.syncState
}