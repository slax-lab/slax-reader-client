package com.slax.reader.repository

import com.powersync.db.schema.Column
import com.powersync.db.schema.Schema
import com.powersync.db.schema.Table
import com.powersync.db.schema.TrackPreviousValuesOptions

val srUserTable = Table(
    name = "sr_user",
    ignoreEmptyUpdates = true,
    trackPreviousValues = TrackPreviousValuesOptions(columnFilter = null, onlyWhenChanged = true),
    columns = listOf(
        Column.text("email"),
        Column.text("name"),
        Column.text("picture"),
        Column.text("given_name"),
        Column.text("family_name"),
        Column.text("lang"),
        Column.text("ai_lang"),
        Column.text("timezone"),
        Column.text("account"),
        Column.text("last_read_at"),
        Column.text("invite_code")
    ),
)

val srUserTagTable = Table(
    name = "sr_user_tag",
    columns = listOf(
        Column.text("user_id"),
        Column.text("tag_name"),
        Column.text("display"),
        Column.text("created_at"),
    )
)

val srPlatformBindTable = Table(
    name = "sr_platform_bind",
    columns = listOf(
        Column.text("user_id"),
        Column.text("platform"),
        Column.text("platform_id"),
        Column.text("user_name"),
        Column.text("created_at"),
    )
)

val srUserNotificationTable = Table(
    name = "sr_user_notification",
    columns = listOf(
        Column.text("user_id"),
        Column.text("type"),
        Column.text("source"),
        Column.text("title"),
        Column.text("body"),
        Column.text("details"),
        Column.integer("is_read"),
        Column.text("created_at"),
    )
)

val srUserBookmarkOverviewTable = Table(
    name = "sr_user_bookmark_overview",
    columns = listOf(
        Column.text("user_id"),
        Column.text("overview"),
        Column.text("content"),
    )
)

val srUserBookmarkTable = Table(
    name = "sr_user_bookmark",
    columns = listOf(
        Column.text("user_id"),
        Column.integer("is_read"),
        Column.integer("archive_status"),
        Column.integer("is_starred"),
        Column.text("created_at"),
        Column.text("updated_at"),
        Column.text("alias_title"),
        Column.integer("type"),
        Column.text("deleted_at"),
        Column.text("metadata"),
    )
)

val AppSchema = Schema(
    listOf(
        srUserTable,
        srUserTagTable,
        srPlatformBindTable,
        srUserNotificationTable,
        srUserBookmarkOverviewTable,
        srUserBookmarkTable
    )
)