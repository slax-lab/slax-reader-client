package com.slax.reader.const

import kotlinx.serialization.Serializable

@Serializable
object InboxListRoute

@Serializable
object LoginRoute

@Serializable
data class BookmarkDetailRoute(val id: String)


@Serializable
object DebugRoute