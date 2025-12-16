package com.slax.reader.const

import kotlinx.serialization.Serializable

@Serializable
object DebugRoutes

@Serializable
object InboxRoutes

@Serializable
object LoginRoutes

@Serializable
object SpaceManagerRoutes

@Serializable
data class BookmarkRoutes(val bookmarkId: String)

@Serializable
object SettingsRoutes

@Serializable
object AboutRoutes

@Serializable
object DeleteAccountRoutes

@Serializable
object SubscribeRoutes

@Serializable
object SubscriptionManagerRoutes

