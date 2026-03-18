package com.slax.reader.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.slax.reader.SlaxConfig
import com.powersync.sync.SyncStatusData
import com.slax.reader.data.database.dao.PowerSyncDao
import com.slax.reader.utils.i18n
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_sm_back
import kotlin.time.measureTime

data class NetworkTestResult(
    val name: String,
    val status: TestStatus,
    val message: String,
    val duration: Long = 0
)

sealed class TestStatus {
    object Idle : TestStatus()
    object Testing : TestStatus()
    object Success : TestStatus()
    object Failed : TestStatus()
}

class DebugViewModel(
    private val httpClient: HttpClient,
    private val powerSyncDao: PowerSyncDao
) : ViewModel() {

    private val _testResults = mutableStateListOf<NetworkTestResult>()
    val testResults: List<NetworkTestResult> = _testResults

    private val _isTestingAll = mutableStateOf(false)
    val isTestingAll: State<Boolean> = _isTestingAll

    val powerSyncStatus: StateFlow<SyncStatusData?> = powerSyncDao.watchPowerSyncStatus()

    val systemInfo = getSystemInfo()

    init {
        initializeTests()
        runAllTests()
    }

    private fun initializeTests() {
        _testResults.clear()
        _testResults.addAll(
            listOf(
                NetworkTestResult("Public IP (cip.cc)", TestStatus.Idle, ""),
                NetworkTestResult("Public IP (Cloudflare)", TestStatus.Idle, ""),
                NetworkTestResult("Google DNS", TestStatus.Idle, ""),
                NetworkTestResult("Slax API", TestStatus.Idle, ""),
                NetworkTestResult("DNS (System)", TestStatus.Idle, ""),
                NetworkTestResult("DNS (8.8.8.8)", TestStatus.Idle, ""),
                NetworkTestResult("DNS (1.1.1.1)", TestStatus.Idle, "")
            )
        )
    }

    fun runAllTests() {
        if (_isTestingAll.value) return

        viewModelScope.launch(Dispatchers.IO) {
            _isTestingAll.value = true

            testPublicIpCipCc()
            testPublicIpCloudflare()
            testGoogleDns()
            testSlaxApi()
            testDnsResolution(4, "System", null)
            testDnsResolution(5, "8.8.8.8", "8.8.8.8")
            testDnsResolution(6, "1.1.1.1", "1.1.1.1")

            _isTestingAll.value = false
        }
    }

    private suspend fun testPublicIpCipCc() {
        updateTestStatus(0, TestStatus.Testing, "Testing...")

        try {
            var ip = "Unknown"
            val duration = measureTime {
                val response = httpClient.get("http://cip.cc") {
                    timeout {
                        requestTimeoutMillis = 10000
                        connectTimeoutMillis = 5000
                        socketTimeoutMillis = 5000
                    }
                }
                if (response.status.value !in 200..299) {
                    throw Exception("HTTP ${response.status.value}")
                }
                val body = response.bodyAsText()
                val ipLine = body.lines().firstOrNull { it.startsWith("IP") }
                ip = ipLine?.substringAfter(":")?.trim() ?: "Unknown"
            }

            updateTestStatus(0, TestStatus.Success, "$ip (${duration.inWholeMilliseconds}ms)")
        } catch (e: Exception) {
            updateTestStatus(0, TestStatus.Failed, e.message ?: "Connection failed")
        }
    }

    private suspend fun testPublicIpCloudflare() {
        updateTestStatus(1, TestStatus.Testing, "Testing...")

        try {
            var ip = "Unknown"
            val duration = measureTime {
                val response = httpClient.get("https://cloudflare.com/cdn-cgi/trace") {
                    timeout {
                        requestTimeoutMillis = 10000
                        connectTimeoutMillis = 5000
                        socketTimeoutMillis = 5000
                    }
                }
                if (response.status.value !in 200..299) {
                    throw Exception("HTTP ${response.status.value}")
                }
                val body = response.bodyAsText()
                val ipLine = body.lines().firstOrNull { it.startsWith("ip=") }
                ip = ipLine?.substringAfter("=")?.trim() ?: "Unknown"
            }

            updateTestStatus(1, TestStatus.Success, "$ip (${duration.inWholeMilliseconds}ms)")
        } catch (e: Exception) {
            updateTestStatus(1, TestStatus.Failed, e.message ?: "Connection failed")
        }
    }

    private suspend fun testGoogleDns() {
        updateTestStatus(2, TestStatus.Testing, "Testing...")

        try {
            val duration = measureTime {
                val response = httpClient.get("https://dns.google") {
                    timeout {
                        requestTimeoutMillis = 10000
                        connectTimeoutMillis = 5000
                        socketTimeoutMillis = 5000
                    }
                }
                if (response.status.value !in 200..299) {
                    throw Exception("HTTP ${response.status.value}")
                }
            }

            updateTestStatus(
                2,
                TestStatus.Success,
                "Connected (${duration.inWholeMilliseconds}ms)"
            )
        } catch (e: Exception) {
            updateTestStatus(
                2,
                TestStatus.Failed,
                e.message ?: "Connection failed"
            )
        }
    }

    private suspend fun testSlaxApi() {
        updateTestStatus(3, TestStatus.Testing, "Testing...")

        try {
            val duration = measureTime {
                val response = httpClient.get("${SlaxConfig.API_BASE_URL}/ping") {
                    timeout {
                        requestTimeoutMillis = 10000
                        connectTimeoutMillis = 5000
                        socketTimeoutMillis = 5000
                    }
                }
                if (response.status.value !in 200..299) {
                    throw Exception("HTTP ${response.status.value}")
                }
            }

            updateTestStatus(
                3,
                TestStatus.Success,
                "Connected (${duration.inWholeMilliseconds}ms)"
            )
        } catch (e: Exception) {
            updateTestStatus(
                3,
                TestStatus.Failed,
                e.message ?: "Connection failed"
            )
        }
    }

    private suspend fun testDnsResolution(index: Int, label: String, dnsServer: String?) {
        updateTestStatus(index, TestStatus.Testing, "Testing...")

        try {
            val domain = SlaxConfig.API_BASE_URL
                .removePrefix("https://")
                .removePrefix("http://")
                .substringBefore("/")
            val result = resolveDns(domain, dnsServer)

            updateTestStatus(
                index,
                TestStatus.Success,
                result
            )
        } catch (e: Exception) {
            updateTestStatus(
                index,
                TestStatus.Failed,
                e.message ?: "DNS resolution failed"
            )
        }
    }

    private fun updateTestStatus(index: Int, status: TestStatus, message: String) {
        if (index < _testResults.size) {
            _testResults[index] = _testResults[index].copy(
                status = status,
                message = message
            )
        }
    }
}

expect fun getSystemInfo(): Map<String, String>
expect suspend fun resolveDns(domain: String, dnsServer: String?): String

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    onBackClick: () -> Unit
) {
    val httpClient: HttpClient = koinInject()
    val powerSyncDao: PowerSyncDao = koinInject()

    val viewModel: DebugViewModel = viewModel {
        DebugViewModel(httpClient, powerSyncDao)
    }

    val powerSyncStatus by viewModel.powerSyncStatus.collectAsState()
    val isTestingAll by viewModel.isTestingAll

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug Info") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_sm_back),
                            contentDescription = "btn_back".i18n(),
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF5F5F3)
                )
            )
        },
        containerColor = Color(0xFFF5F5F3)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // System Info Section
            item {
                SectionCard(title = "System Info") {
                    viewModel.systemInfo.forEach { (key, value) ->
                        InfoRow(label = key, value = value)
                    }
                }
            }

            // PowerSync Status Section
            item {
                SectionCard(title = "PowerSync Status") {
                    powerSyncStatus?.let { status ->
                        InfoRow("Connected", if (status.connected) "✓ Yes" else "✗ No")
                        InfoRow("Connecting", if (status.connecting) "Yes" else "No")
                        InfoRow("Downloading", if (status.downloading) "Yes" else "No")
                        InfoRow("Uploading", if (status.uploading) "Yes" else "No")

                        if (status.anyError != null) {
                            InfoRow("Error", status.anyError.toString(), isError = true)
                        }

                        if (status.downloading) {
                            val progress = if (status.downloading && status.downloadProgress != null) {
                                "${status.downloadProgress!!.downloadedOperations}/${status.downloadProgress!!.totalOperations}"
                            } else {
                                "Preparing..."
                            }
                            InfoRow("Progress", progress)
                        }
                    } ?: run {
                        InfoRow("Status", "Not initialized")
                    }
                }
            }

            // Network Tests Section
            item {
                SectionCard(title = "Network Tests") {
                    Button(
                        onClick = { viewModel.runAllTests() },
                        enabled = !isTestingAll,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F1419)
                        )
                    ) {
                        Text(if (isTestingAll) "Testing..." else "Run All Tests")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    viewModel.testResults.forEach { result ->
                        TestResultRow(result)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0F1419),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        content()
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    isError: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF666666),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            color = if (isError) Color(0xFFE53935) else Color(0xFF0F1419),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun TestResultRow(result: NetworkTestResult) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status Icon
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    color = when (result.status) {
                        TestStatus.Success -> Color(0xFF4CAF50)
                        TestStatus.Failed -> Color(0xFFE53935)
                        TestStatus.Testing -> Color(0xFFFFA726)
                        TestStatus.Idle -> Color(0xFFBDBDBD)
                    },
                    shape = RoundedCornerShape(6.dp)
                )
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF0F1419)
            )
            if (result.message.isNotEmpty()) {
                Text(
                    text = result.message,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF666666)
                )
            }
        }

        if (result.status == TestStatus.Testing) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = Color(0xFF0F1419)
            )
        }
    }
}
