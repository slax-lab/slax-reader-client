package com.slax.reader.data.database.dao

import com.powersync.PowerSyncDatabase
import com.powersync.db.getString
import com.powersync.db.getStringOptional
import com.slax.reader.data.database.model.UserInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*

class UserDao(
    private val scope: CoroutineScope,
    private val database: PowerSyncDatabase
) {
    private val _userInfoFlow: StateFlow<UserInfo?> by lazy {
        println("[watch][database] _userInfoFlow")

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
                    name = cursor.getStringOptional("name") ?: "",
                    picture = cursor.getStringOptional("picture") ?: "",
                    given_name = cursor.getStringOptional("given_name") ?: "",
                    family_name = cursor.getStringOptional("family_name") ?: "",
                    lang = cursor.getString("lang"),
                    ai_lang = cursor.getStringOptional("ai_lang") ?: "",
                    timezone = cursor.getString("timezone"),
                    account = cursor.getStringOptional("account") ?: "",
                    last_read_at = cursor.getStringOptional("last_read_at") ?: "",
                    invite_code = cursor.getStringOptional("invite_code") ?: "",
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