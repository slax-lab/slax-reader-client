package com.slax.reader.ui.bookmark

/**
 * 阅读位置相关的常量定义
 */
object ReadPositionConstants {
    /**
     * 保存阅读位置的防抖延迟时间（毫秒）
     * 避免频繁写入，提升性能
     */
    const val SAVE_DEBOUNCE_MS = 500L

    /**
     * 视觉间距（dp）
     * iOS 平台用于计算 contentInset
     */
    const val VISUAL_SPACING_DP = 16f
}