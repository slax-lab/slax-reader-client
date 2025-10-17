package com.slax.reader.utils

import android.view.MotionEvent
import android.view.View
import android.webkit.WebView

/**
 * Android 平台的 WebView 配置实现
 */
actual fun configureNativeWebView(
    webView: Any,
    disableScrolling: Boolean,
    removeContentInsets: Boolean
) {
    if (webView !is WebView) {
        println("configureNativeWebView: webView 不是 Android WebView 实例")
        return
    }

    // 禁用滚动
    if (disableScrolling) {
        // 禁用垂直和水平滚动
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false

        // 禁用滚动条的淡入淡出效果
        webView.scrollBarStyle = View.SCROLLBARS_OUTSIDE_OVERLAY

        // 拦截触摸事件，阻止滚动手势
        webView.setOnTouchListener { _, event ->
            // 只允许点击事件，不允许滚动
            event.action == MotionEvent.ACTION_MOVE
        }

        // 禁用 overscroll 效果
        webView.overScrollMode = View.OVER_SCROLL_NEVER
    }

    // 去除内部安全距离（Content Insets）
    if (removeContentInsets) {
        // 移除 WebView 的内边距
        webView.setPadding(0, 0, 0, 0)

        // 禁用 WebView 的内容 insets
        webView.setOnApplyWindowInsetsListener { _, insets ->
            insets.consumeSystemWindowInsets()
        }
    }

    println("Android WebView 配置完成: disableScrolling=$disableScrolling, removeContentInsets=$removeContentInsets")
}