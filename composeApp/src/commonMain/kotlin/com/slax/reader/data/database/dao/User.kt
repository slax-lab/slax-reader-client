package com.slax.reader.data.database.dao

import com.powersync.PowerSyncDatabase
import com.powersync.db.getString
import com.slax.reader.data.database.model.UserInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*

class UserDao(
    private val database: PowerSyncDatabase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _userInfoFlow: StateFlow<UserInfo?> by lazy {
        database.watch(
            """SELECT email, name, picture, given_name, family_name,
                           lang, ai_lang, timezone, account,
                           last_read_at, invite_code
                FROM sr_user
                LIMIT 1
                """,
            parameters = listOf(),
            mapper = { cursor ->
                UserInfo(
                    email = cursor.getString("email"),
                    name = cursor.getString("name"),
                    picture = cursor.getString("picture"),
                    given_name = cursor.getString("given_name"),
                    family_name = cursor.getString("family_name"),
                    lang = cursor.getString("lang"),
                    ai_lang = cursor.getString("ai_lang"),
                    timezone = cursor.getString("timezone"),
                    account = cursor.getString("account"),
                    last_read_at = cursor.getString("last_read_at"),
                    invite_code = cursor.getString("invite_code"),
                )
            }
        ).map { it.firstOrNull() }
            .catch { e ->
                println("Error watching user info: ${e.message}")
            }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.WhileSubscribed(5000), null)
    }

    fun watchUserInfo(): StateFlow<UserInfo?> = _userInfoFlow
}