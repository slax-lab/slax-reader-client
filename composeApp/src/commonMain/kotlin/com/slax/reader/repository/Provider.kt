package com.slax.reader.repository

import com.powersync.db.schema.Column
import com.powersync.db.schema.Schema
import com.powersync.db.schema.Table

val AppSchema = Schema(
    listOf(
        Table(
            name = "orders",
            columns = listOf(
                Column.integer("order_id"),
                Column.integer("user_id"),
                Column.text("order_number"),
                Column.text("product_name"),
                Column.integer("quantity"),
                Column.real("unit_price"),
                Column.real("total_amount"),
                Column.real("discount_amount"),
                Column.real("final_amount"),
                Column.text("order_status"),
                Column.text("payment_method"),
                Column.text("shipping_address"),
                Column.text("order_date"),
                Column.text("shipped_date"),
                Column.text("delivered_date")
            )
        )
    )
)
