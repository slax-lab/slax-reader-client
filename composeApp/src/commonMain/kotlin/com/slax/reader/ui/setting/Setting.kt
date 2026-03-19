package com.slax.reader.ui.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.slax.reader.const.DeleteAccountRoutes
import com.slax.reader.utils.LocaleString
import com.slax.reader.utils.i18n
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_sm_back
import slax_reader_client.composeapp.generated.resources.ic_xs_tick_gray_outline_icon

private const val UNLIMIT = -1
private val CacheCountSteps = listOf(10, 30, 50, 100, 200, UNLIMIT)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    onBackClick: () -> Unit,
    navController: NavHostController
) {
    val coroutineScope = rememberCoroutineScope()
    val viewModel: SettingViewModel = koinViewModel()
    var showLanguageDialog by remember { mutableStateOf(false) }
    val selectedCacheCount by viewModel.cacheCount.collectAsState()
    val isDownloadImages by viewModel.downloadImages.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "setting_title".i18n(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF0F1419)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_sm_back),
                            contentDescription = "返回",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 离线缓存设置卡片
            OfflineCacheCard(
                selectedCacheCount = selectedCacheCount,
                onCacheCountChange = { viewModel.updateCacheCount(it) },
                downloadImages = isDownloadImages,
                onDownloadImagesChange = { viewModel.updateDownloadImages(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 语言设置卡片
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                SettingItem(
                    title = "setting_language".i18n(),
                    rightText = if (LocaleString.currentLocale == "zh") "language_chinese".i18n() else "language_english".i18n(),
                    onClick = {
                        showLanguageDialog = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 注销账号按钮
            Button(
                onClick = {
                    navController.navigate(DeleteAccountRoutes)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x141A1A1A)
                )
            ) {
                Text(
                    text = "setting_delete_account".i18n(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFF45454),
                    lineHeight = 22.5.sp
                )
            }
        }
    }

    // 语言选择对话框
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            containerColor = Color.White,
            title = { Text("setting_language".i18n()) },
            text = {
                Column {
                    RadioButtonItem(
                        text = "language_chinese".i18n(),
                        selected = LocaleString.currentLocale == "zh",
                        onClick = {
                            coroutineScope.launch {
                                LocaleString.changeLocale("zh")
                            }
                            showLanguageDialog = false
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    RadioButtonItem(
                        text = "language_english".i18n(),
                        selected = LocaleString.currentLocale == "en",
                        onClick = {
                            coroutineScope.launch {
                                LocaleString.changeLocale("en")
                            }
                            showLanguageDialog = false
                        }
                    )
                }
            },
            confirmButton = { }
        )
    }
}

@Composable
private fun OfflineCacheCard(
    selectedCacheCount: Int,
    onCacheCountChange: (Int) -> Unit,
    downloadImages: Boolean,
    onDownloadImagesChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题
            Text(
                text = "setting_offline_cache_title".i18n(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF0F1419),
                lineHeight = 22.5.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            CacheCountStepper(
                value = selectedCacheCount,
                onValueChange = onCacheCountChange
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "setting_offline_cache_desc".i18n(),
                fontSize = 14.sp,
                color = Color(0xCC333333),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 0.5.dp,
                color = Color(0x14333333)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 图片下载开关
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onDownloadImagesChange(!downloadImages)
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .border(
                                width = 0.5.dp,
                                color = Color(0x291a1a1a),
                                shape = RoundedCornerShape(2.dp)
                            )
                            .background(
                                color = Color.White,
                                shape = RoundedCornerShape(2.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (downloadImages) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_xs_tick_gray_outline_icon),
                                contentDescription = null,
                                modifier = Modifier.size(8.dp),
                                tint = Color.Unspecified
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "setting_download_images".i18n(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF0F1419),
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "setting_download_images_desc".i18n(),
                    fontSize = 14.sp,
                    color = Color(0xCC333333),
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun CacheCountStepper(
    value: Int,
    onValueChange: (Int) -> Unit
) {
    val currentIndex = CacheCountSteps.indexOf(value).coerceAtLeast(0)
    val canDecrease = currentIndex > 0
    val canIncrease = currentIndex < CacheCountSteps.lastIndex

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 减号按钮
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(8.dp),
            color = if (canDecrease) Color(0xFFF5F5F3) else Color(0xFFFAFAF9),
            onClick = {
                if (canDecrease) {
                    onValueChange(CacheCountSteps[currentIndex - 1])
                }
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "−",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (canDecrease) Color(0xFF333333) else Color(0xFFCCCCCC)
                )
            }
        }

        // 中间数字 / 无限符号（加大间隔）
        Box(
            modifier = Modifier
                .widthIn(min = 120.dp)
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (value == UNLIMIT) {
                Text(
                    text = "∞",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F1419),
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "$value",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F1419),
                    textAlign = TextAlign.Center
                )
            }
        }

        // 加号按钮
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(8.dp),
            color = if (canIncrease) Color(0xFFF5F5F3) else Color(0xFFFAFAF9),
            onClick = {
                if (canIncrease) {
                    onValueChange(CacheCountSteps[currentIndex + 1])
                }
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "+",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (canIncrease) Color(0xFF333333) else Color(0xFFCCCCCC)
                )
            }
        }
    }
}

@Composable
private fun RadioButtonItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.Gray)
            ) { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFF16b998)
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 16.sp,
            color = Color(0xFF0F1419)
        )
    }
}

@Composable
private fun SettingItem(
    title: String,
    color: Color = Color(0xFF0F1419),
    rightText: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.Gray)
            ) {
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            lineHeight = 22.5.sp
        )

        if (rightText != null) {
            Text(
                text = rightText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF999999),
                lineHeight = 20.sp
            )
        }
    }
}