package com.slax.reader.ui.debug

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slax.reader.domain.cache.CacheCategory
import com.slax.reader.domain.cache.CacheUsage
import com.slax.reader.utils.i18n

@Composable
internal fun CacheMaintenanceSection(
    cacheUsage: CacheUsage,
    state: CacheMaintenanceState,
    onClick: () -> Unit,
) {
    val isBusy = state is CacheMaintenanceState.Refreshing || state is CacheMaintenanceState.Clearing
    SectionCard(title = "Cache Maintenance") {
        Text(
            text = "setting_clear_cache_desc".i18n(),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = Color(0xFF666666),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onClick,
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F1419)),
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
            } else {
                Text("${"setting_clear_cache".i18n()} - ${formatCacheSize(cacheUsage.totalBytes)}")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CacheCleanupSheet(
    cacheUsage: CacheUsage,
    state: CacheMaintenanceState,
    onDismiss: () -> Unit,
    onClear: (Set<CacheCategory>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isRefreshing = state is CacheMaintenanceState.Refreshing
    val isClearing = state is CacheMaintenanceState.Clearing
    var selectedCategories by remember { mutableStateOf(cacheUsage.nonEmptyCategories) }
    var hasChangedSelection by remember { mutableStateOf(false) }
    val selectedBytes = cacheUsage.bytes(selectedCategories)
    val canClear = selectedBytes > 0L && !isRefreshing && !isClearing

    LaunchedEffect(cacheUsage) {
        selectedCategories = if (hasChangedSelection) {
            selectedCategories.intersect(cacheUsage.nonEmptyCategories)
        } else {
            cacheUsage.nonEmptyCategories
        }
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
            CacheSizeRing(cacheUsage)
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color(0x14333333))

            CacheCategory.entries.forEach { category ->
                CacheSelectionRow(
                    category = category,
                    selected = category in selectedCategories,
                    enabled = cacheUsage.bytes(category) > 0L && !isRefreshing && !isClearing,
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
                if (isRefreshing || isClearing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = if (isClearing) Color.White else Color(0xFF999999),
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

internal fun formatCacheSize(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val megabytes = bytes / (1024.0 * 1024.0)
    return if (megabytes >= 1024.0) {
        val gigabytes = megabytes / 1024.0
        "${(gigabytes * 10).toLong() / 10.0} GB"
    } else if (megabytes >= 0.1) {
        "${(megabytes * 10).toLong() / 10.0} MB"
    } else {
        "< 0.1 MB"
    }
}
