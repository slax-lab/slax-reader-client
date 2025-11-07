package com.slax.reader.ui.sidebar

import androidx.lifecycle.ViewModel
import com.slax.reader.data.database.dao.UserDao
import com.slax.reader.domain.coordinator.CoordinatorDomain

class SidebarViewModel(private val userDao: UserDao, coordinatorDomain: CoordinatorDomain) : ViewModel() {
    val userInfo = userDao.watchUserInfo()
    val syncStatus = coordinatorDomain.syncState
}