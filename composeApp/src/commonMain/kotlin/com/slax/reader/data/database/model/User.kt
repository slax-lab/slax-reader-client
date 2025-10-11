package com.slax.reader.data.database.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class UserInfo(
    val email: String,
    val name: String,
    val picture: String,
    val given_name: String,
    val family_name: String,
    val lang: String,
    val ai_lang: String,
    val timezone: String,
    val account: String,
    val last_read_at: String,
    val invite_code: String,
)