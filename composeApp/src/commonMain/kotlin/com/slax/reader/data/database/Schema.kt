package com.slax.reader.data.database

import com.powersync.db.schema.Column
import com.powersync.db.schema.Schema
import com.powersync.db.schema.Table
import com.powersync.db.schema.TrackPreviousValuesOptions

val srUserTable = Table(
    name = "sr_user",
    ignoreEmptyUpdates = true,
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
    trackPreviousValues = TrackPreviousValuesOptions(),
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

val srUserBookmarkTable = Table(
    name = "sr_user_bookmark",
    trackPreviousValues = TrackPreviousValuesOptions(onlyWhenChanged = true, columnFilter = null),
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

val srUserSubscriptionTable = Table(
    name = "sr_user_subscription",
    columns = listOf(
        Column.text("stripe_subscription_id"),
        Column.text("stripe_customer_id"),
        Column.text("stripe_stripe_currency"),
        Column.integer("first_subscription_time"),
        Column.integer("subscription_end_time"),
        Column.integer("next_invoice_time"),
        Column.integer("auto_renew"),
        Column.integer("stripe_credit"),
        Column.integer("subscribed"),
        Column.text("apple_original_transaction_id"),
        Column.text("source_type")
    )
)

val srBookmarkComment = Table(
    name = "sr_bookmark_comment",
    columns = listOf(
        Column.integer("type"),
        Column.text("source"),
        Column.text("user_bookmark_uuid"),
        Column.text("comment"),
        Column.text("approx_source"),
        Column.text("content"),
        Column.integer("is_deleted"),
        Column.text("created_at"),
        Column.text("metadata"),
    )
)

val localBookmarkInfo = Table(
    name = "local_bookmark_info",
    localOnly = true,
    columns = listOf(
        Column.text("overview"),
        Column.text("key_takeaways"),
        Column.integer("is_downloaded")
    )
)

val AppSchema = Schema(
    listOf(
        srUserTable,
        srUserTagTable,
        srPlatformBindTable,
        srUserNotificationTable,
        srUserBookmarkTable,
        srUserSubscriptionTable,
        srBookmarkComment,

        // local only, not sync to server
        localBookmarkInfo
    )
)