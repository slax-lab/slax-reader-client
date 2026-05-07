package com.slax.reader.const

import kotlinx.serialization.Serializable

@Serializable
object InboxRoutes

@Serializable
object LoginRoutes
@Serializable
data class BookmarkRoutes(val bookmarkId: String)

@Serializable
object SettingsRoutes

@Serializable
object AboutRoutes

@Serializable
object DeleteAccountRoutes

@Serializable
object SubscriptionManagerRoutes

@Serializable
object DebugRoutes

@Serializable
data class FeedbackRoutes(
    val title: String? = null,
    val href: String? = null,
    val email: String? = null,
    val bookmarkId: String? = null,
    val entryPoint: String? = null,
    val version: String? = null,
)

