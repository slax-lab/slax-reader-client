package com.slax.reader.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.powersync.DatabaseDriverFactory
import com.powersync.PowerSyncDatabase
import com.powersync.connectors.PowerSyncBackendConnector
import com.powersync.connectors.PowerSyncCredentials
import com.powersync.db.getString
import com.slax.reader.repository.AppSchema
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

data class Order(
    val orderId: Int,
    val userId: Int,
    val orderNumber: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalAmount: Double,
    val discountAmount: Double,
    val finalAmount: Double,
    val orderStatus: OrderStatus,
    val paymentMethod: PaymentMethod,
    val shippingAddress: String,
    val orderDate: String,
    val shippedDate: String?,
    val deliveredDate: String?
)

enum class OrderStatus(val value: String) {
    PENDING("pending"),
    CONFIRMED("confirmed"),
    SHIPPED("shipped"),
    DELIVERED("delivered"),
    CANCELLED("cancelled")
}

enum class PaymentMethod(val value: String) {
    CREDIT_CARD("credit_card"),
    DEBIT_CARD("debit_card"),
    PAYPAL("paypal"),
    BANK_TRANSFER("bank_transfer"),
    CASH_ON_DELIVERY("cash_on_delivery")
}

class Connector() : PowerSyncBackendConnector() {
    override suspend fun fetchCredentials(): PowerSyncCredentials {
        return PowerSyncCredentials(
            endpoint = "",
            token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImVuYy0xNzU2ODc1MzMxIn0.eyJpYXQiOjE3NTY4NzgyOTUsImV4cCI6MTc1Njg5OTg5NSwibmJmIjoxNzU2ODc4Mjk1LCJpc3MiOiJwb3dlcnN5bmMiLCJzdWIiOiIxIiwiYXVkIjoicG93ZXJzeW5jIiwidXNlcl9pZCI6MX0.eIuqi4giU-EDxcBW3caJ5MBOecMp1ZcoNbembqfpHRvv70syXXKlmwNXHt4kMsxO7OFKujMlpjJ9pDu1dhoxHnHvTUm7yPweMEkUOAWnW9wTyX5VQyvxxMNFVk4oxXG2YCx9ctZykPoT7xbryZ4ZK2NwXlEfiB43m8FcORjix70KNFDrh6P1L7ov0KH3TbOna1LUCI6cIWj1z7bXBteD5ltOoteRJAB8-EEvKsO8_b7rcEqFvKZu18DRB5SYaO-Rk2ufxWiOZDiKA3sbm8fIUQWyUoCIwaIpkvVemX7dn4beg37-0T11IDQH2DnoNhUUSvuEUtx94XgCcRV4vbbKMA"
        )
    }

    override suspend fun uploadData(database: PowerSyncDatabase) {
        // No-op for download-only sync
        // 后端API：
        // PUT
        // DELETE
    }
}

@Composable
fun OrdersScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val factory = koinInject<DatabaseDriverFactory>()

    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var connectionStatus by remember { mutableStateOf("Disconnected") }

    val connector = remember { Connector() }
    val database = remember {
        PowerSyncDatabase(
            factory = factory,
            schema = AppSchema,
            dbFilename = "slax-reader.db",
        )
    }

    // Helper function to connect and wait for PowerSync
    suspend fun connectAndWait(): String {
        database.connect(connector)

        // Wait for connection
        var attempts = 0
        while (attempts < 5) {
            kotlinx.coroutines.delay(1000)
            attempts++
            val status = database.currentStatus
            if (status.connected) {
                return "Connected"
            } else if (!status.connecting && attempts > 3) {
                return "Failed"
            }
        }
        return "Failed"
    }

    LaunchedEffect(Unit) {
        connectionStatus = "Connecting..."
        try {
            connectionStatus = connectAndWait()

            if (database.currentStatus.connected) {
                // Watch for orders
                database.watch(
                    "SELECT * FROM orders ORDER BY order_date DESC"
                ) { cursor ->
                    Order(
                        orderId = cursor.getString("order_id")?.toIntOrNull() ?: 0,
                        userId = cursor.getString("user_id")?.toIntOrNull() ?: 0,
                        orderNumber = cursor.getString("order_number") ?: "",
                        productName = cursor.getString("product_name") ?: "",
                        quantity = cursor.getString("quantity")?.toIntOrNull() ?: 0,
                        unitPrice = cursor.getString("unit_price")?.toDoubleOrNull() ?: 0.0,
                        totalAmount = cursor.getString("total_amount")?.toDoubleOrNull() ?: 0.0,
                        discountAmount = cursor.getString("discount_amount")?.toDoubleOrNull() ?: 0.0,
                        finalAmount = cursor.getString("final_amount")?.toDoubleOrNull() ?: 0.0,
                        orderStatus = OrderStatus.entries.find {
                            it.value == cursor.getString("order_status")
                        } ?: OrderStatus.PENDING,
                        paymentMethod = PaymentMethod.entries.find {
                            it.value == cursor.getString("payment_method")
                        } ?: PaymentMethod.CREDIT_CARD,
                        shippingAddress = cursor.getString("shipping_address") ?: "",
                        orderDate = cursor.getString("order_date") ?: "",
                        shippedDate = try {
                            cursor.getString("shipped_date")
                        } catch (_: Exception) {
                            ""
                        },
                        deliveredDate = try {
                            cursor.getString("delivered_date")
                        } catch (_: Exception) {
                            ""
                        }
                    )
                }
                    .catch { e ->
                        connectionStatus = "Error: ${e.message}"
                    }
                    .collect { ordersList ->
                        orders = ordersList
                    }
            }
        } catch (e: Exception) {
            connectionStatus = "Error: ${e.message}"
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            scope.launch {
                try {
                    database.disconnect()
                    orders = emptyList()
                    connectionStatus = "Disconnected"
                } catch (_: Exception) {
                    // Ignore disconnect errors
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Orders",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    connectionStatus == "Connected" -> MaterialTheme.colorScheme.primaryContainer
                    connectionStatus.startsWith("Error") -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.secondaryContainer
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Status: $connectionStatus",
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Orders: ${orders.size}",
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    try {
                        // Disconnect first
                        database.disconnect()

                        // Clear all data and reset checkpoint
                        database.execute("DELETE FROM orders")

                        // Reset PowerSync's internal state by clearing all sync data
                        database.execute("DELETE FROM ps_oplog")
                        database.execute("DELETE FROM ps_buckets")
                        database.execute("DELETE FROM ps_crud")

                        // Clear local state
                        orders = emptyList()
                        connectionStatus = "Disconnected"

                        // Reconnect to start fresh
                        connectionStatus = connectAndWait()
                    } catch (_: Exception) {
                        connectionStatus = "Error resetting"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset All Data & Sync")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (orders.isNotEmpty()) {
            Text(
                text = "Orders List:",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(orders) { order ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "Order #${order.orderNumber}",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = order.productName,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Amount: $${order.finalAmount}",
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Status: ${order.orderStatus.value}",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}