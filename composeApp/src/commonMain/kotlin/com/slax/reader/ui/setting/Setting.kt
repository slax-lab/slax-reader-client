package com.slax.reader.ui.setting

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.slax.reader.const.DeleteAccountRoutes
import com.slax.reader.domain.cache.CacheCategory
import com.slax.reader.domain.cache.CacheUsage
import com.slax.reader.utils.LocaleString
import com.slax.reader.utils.i18n
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_sm_back
import slax_reader_client.composeapp.generated.resources.ic_xs_tick_gray_outline_icon

private const val UNLIMIT = -1
private val CacheCountSteps = listOf(30, 50, 100, 200, UNLIMIT)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    onBackClick: () -> Unit,
    navController: NavHostController
) {
    val coroutineScope = rememberCoroutineScope()
    val viewModel: SettingViewModel = koinViewModel()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showClearCacheSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val selectedCacheCount by viewModel.cacheCount.collectAsState()
    val isDownloadImages by viewModel.downloadImages.collectAsState()
    val cacheUsage by viewModel.cacheUsage.collectAsState()
    val clearCacheState by viewModel.clearCacheState.collectAsState()

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                onDownloadImagesChange = { viewModel.updateDownloadImages(it) },
                cacheBytes = cacheUsage.totalBytes,
                isClearing = clearCacheState is ClearCacheState.Clearing,
                onClearCacheClick = {
                    viewModel.refreshCacheSize()
                    showClearCacheSheet = true
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 语言设置卡片
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
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
            val deleteButtonInteractionSource = remember { MutableInteractionSource() }
            val isDeleteButtonPressed by deleteButtonInteractionSource.collectIsPressedAsState()
            Button(
                onClick = {
                    navController.navigate(DeleteAccountRoutes)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDeleteButtonPressed) Color(0x141A1A1A) else Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                interactionSource = deleteButtonInteractionSource,
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

    if (showClearCacheSheet) {
        CacheCleanupSheet(
            cacheUsage = cacheUsage,
            clearCacheState = clearCacheState,
            onDismiss = { showClearCacheSheet = false },
            onClear = viewModel::clearCache,
            onClearFinished = { freedBytes ->
                showClearCacheSheet = false
                viewModel.acknowledgeClearCache()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        "setting_clear_cache_done_desc".i18n(formatCacheSize(freedBytes))
                    )
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CacheCleanupSheet(
    cacheUsage: CacheUsage,
    clearCacheState: ClearCacheState,
    onDismiss: () -> Unit,
    onClear: (Set<CacheCategory>) -> Unit,
    onClearFinished: (Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isClearing = clearCacheState is ClearCacheState.Clearing
    var selectedCategories by remember { mutableStateOf(cacheUsage.nonEmptyCategories) }
    var hasChangedSelection by remember { mutableStateOf(false) }
    val selectedBytes = cacheUsage.bytes(selectedCategories)
    val canClear = selectedBytes > 0L && !isClearing

    LaunchedEffect(cacheUsage) {
        selectedCategories = if (hasChangedSelection) {
            selectedCategories.intersect(cacheUsage.nonEmptyCategories)
        } else {
            cacheUsage.nonEmptyCategories
        }
    }

    LaunchedEffect(clearCacheState) {
        val done = clearCacheState as? ClearCacheState.Done ?: return@LaunchedEffect
        if (sheetState.isVisible) sheetState.hide()
        onClearFinished(done.freedBytes)
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isClearing) onDismiss() },
        sheetState = sheetState,
        sheetGesturesEnabled = !isClearing,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0x29333333)) },
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = !isClearing,
            shouldDismissOnClickOutside = !isClearing,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "setting_cache_sheet_title".i18n(),
                fontSize = 20.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F1419),
            )

            Spacer(modifier = Modifier.height(20.dp))

            CacheSizeRing(cacheUsage = cacheUsage)

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider(thickness = 0.5.dp, color = Color(0x14333333))

            CacheCategory.entries.forEach { category ->
                CacheSelectionRow(
                    category = category,
                    selected = category in selectedCategories,
                    enabled = cacheUsage.bytes(category) > 0L && !isClearing,
                    cacheBytes = cacheUsage.bytes(category),
                    onSelectedChange = { selected ->
                        hasChangedSelection = true
                        selectedCategories = if (selected) {
                            selectedCategories + category
                        } else {
                            selectedCategories - category
                        }
                    },
                )
            }

            HorizontalDivider(thickness = 0.5.dp, color = Color(0x14333333))

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "setting_cache_sheet_notice".i18n(),
                modifier = Modifier.fillMaxWidth(),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = Color(0xFF777777),
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onClear(selectedCategories) },
                enabled = canClear,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFB45837),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFE8E6E3),
                    disabledContentColor = Color(0xFF999999),
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                if (isClearing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = if (selectedBytes > 0L) {
                            "setting_cache_clear_amount".i18n(formatCacheSize(selectedBytes))
                        } else {
                            "setting_cache_nothing_to_clear".i18n()
                        },
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun CacheSizeRing(cacheUsage: CacheUsage) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(cacheUsage) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, animationSpec = tween(durationMillis = 420))
    }

    Box(
        modifier = Modifier.size(156.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val ringWidth = 12.dp.toPx()
            drawCircle(
                color = Color(0xFFE8E6E3),
                style = Stroke(width = ringWidth),
            )
            if (cacheUsage.totalBytes > 0L) {
                var startAngle = -90f
                CacheCategory.entries.forEach { category ->
                    val categorySweep = 360f * cacheUsage.bytes(category) / cacheUsage.totalBytes
                    if (categorySweep > 0f) {
                        val gap = minOf(2f, categorySweep / 4f)
                        drawArc(
                            color = cacheCategoryColor(category),
                            startAngle = startAngle + gap / 2f,
                            sweepAngle = ((categorySweep - gap) * reveal.value).coerceAtLeast(0f),
                            useCenter = false,
                            style = Stroke(width = ringWidth, cap = StrokeCap.Butt),
                        )
                    }
                    startAngle += categorySweep
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatCacheSize(cacheUsage.totalBytes),
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F1419),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "setting_cache_used".i18n(),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = Color(0xFF999999),
            )
        }
    }
}

@Composable
private fun CacheSelectionRow(
    category: CacheCategory,
    selected: Boolean,
    enabled: Boolean,
    cacheBytes: Long,
    onSelectedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onSelectedChange(!selected) }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(cacheCategoryColor(category), CircleShape),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cacheCategoryTitle(category),
                fontSize = 15.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF0F1419),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = cacheCategoryDescription(category),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = Color(0xFF999999),
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = formatCacheSize(cacheBytes),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = Color(0xFF777777),
        )

        Checkbox(
            checked = selected,
            onCheckedChange = if (enabled) onSelectedChange else null,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFFB45837),
                uncheckedColor = Color(0xFFB8B3AC),
                checkmarkColor = Color.White,
            ),
        )
    }
}

private fun cacheCategoryColor(category: CacheCategory): Color = when (category) {
    CacheCategory.ARTICLE -> Color(0xFFB45837)
    CacheCategory.IMAGES -> Color(0xFF16A085)
    CacheCategory.OTHER -> Color(0xFFD39B35)
}

private fun cacheCategoryTitle(category: CacheCategory): String = when (category) {
    CacheCategory.ARTICLE -> "setting_cache_articles".i18n()
    CacheCategory.IMAGES -> "setting_cache_images".i18n()
    CacheCategory.OTHER -> "setting_cache_other".i18n()
}

private fun cacheCategoryDescription(category: CacheCategory): String = when (category) {
    CacheCategory.ARTICLE -> "setting_cache_articles_desc".i18n()
    CacheCategory.IMAGES -> "setting_cache_images_desc".i18n()
    CacheCategory.OTHER -> "setting_cache_other_desc".i18n()
}

@Composable
private fun OfflineCacheCard(
    selectedCacheCount: Int,
    onCacheCountChange: (Int) -> Unit,
    downloadImages: Boolean,
    onDownloadImagesChange: (Boolean) -> Unit,
    cacheBytes: Long,
    isClearing: Boolean,
    onClearCacheClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "setting_offline_cache_title".i18n(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0F1419),
                    lineHeight = 22.5.sp
                )

                Text(
                    text = "${if (selectedCacheCount == UNLIMIT) '∞' else selectedCacheCount}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF999999),
                    lineHeight = 20.sp
                )
            }


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

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 0.5.dp,
                color = Color(0x14333333)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 手动清理缓存
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !isClearing
                    ) {
                        onClearCacheClick()
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "setting_clear_cache".i18n(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF0F1419),
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "setting_clear_cache_desc".i18n(),
                        fontSize = 14.sp,
                        color = Color(0xCC333333),
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                if (isClearing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF999999)
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatCacheSize(cacheBytes),
                            fontSize = 14.sp,
                            color = Color(0xFF999999),
                            lineHeight = 20.sp,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            painter = painterResource(Res.drawable.ic_sm_back),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp).graphicsLayer { rotationZ = 180f },
                            tint = Color.Unspecified,
                        )
                    }
                }
            }
        }
    }
}

private fun formatCacheSize(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024.0) {
        val gb = mb / 1024.0
        "${((gb * 10).toLong() / 10.0)} GB"
    } else if (mb >= 0.1) {
        "${((mb * 10).toLong() / 10.0)} MB"
    } else {
        "< 0.1 MB"
    }
}

@Composable
private fun CacheCountStepper(
    value: Int,
    onValueChange: (Int) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CacheCountSteps.forEach { step ->
            Button(
                onClick = { onValueChange(step) },
                modifier = Modifier
                    .width(50.dp)
                    .height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (value == step) Color(0xFF333333) else Color(0xFFF5F5F3),
                    contentColor = if (value == step) Color.White else Color(0xFF333333)
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = if (step == UNLIMIT) "∞" else "$step",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 21.sp
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
