package com.slax.reader.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPointMake
import platform.Foundation.NSLog
import platform.UIKit.*
import platform.WebKit.WKWebView
import platform.darwin.NSObject

/**
 * 自定义 UIScrollViewDelegate 用于拦截滚动事件
 */
@OptIn(ExperimentalForeignApi::class)
private class ScrollViewDelegateImpl : NSObject(), UIScrollViewDelegateProtocol {
    override fun scrollViewWillBeginDragging(scrollView: UIScrollView) {
        // 阻止滚动开始
        NSLog("scrollViewWillBeginDragging - 阻止滚动")
    }

    override fun scrollViewDidScroll(scrollView: UIScrollView) {
        // 滚动发生时重置回原位
        if (scrollView.contentOffset.useContents { x != 0.0 || y != 0.0 }) {
            scrollView.setContentOffset(CGPointMake(0.0, 0.0), false)
        }
    }
}

/**
 * iOS 平台的 WebView 配置实现
 */
@OptIn(ExperimentalForeignApi::class)
actual fun configureNativeWebView(
    webView: Any,
    disableScrolling: Boolean,
    removeContentInsets: Boolean
) {
    NSLog("configureNativeWebView")
    if (webView !is WKWebView) {
        println("configureNativeWebView: webView 不是 iOS WKWebView 实例")
        return
    }

    NSLog("开始配置 iOS WKWebView")

    // 获取 WKWebView 的 scrollView
    val scrollView: UIScrollView = webView.scrollView

    // 创建自定义背景色 0xfcfcfc
    val customBackgroundColor = UIColor(
        red = 0xfc / 255.0,
        green = 0xfc / 255.0,
        blue = 0xfc / 255.0,
        alpha = 1.0
    )

    webView.opaque = true
    webView.backgroundColor = customBackgroundColor
    scrollView.backgroundColor = customBackgroundColor

    // 设置页面下方背景色，防止系统在夜间模式下添加黑色背景
    webView.underPageBackgroundColor = customBackgroundColor

    NSLog("iOS WKWebView 背景色设置完成 背景色：0xfcfcfc")

    // 去除内部安全距离（Content Insets）
    if (removeContentInsets) {
        // 设置内容 insets 为 0
        scrollView.contentInset = UIEdgeInsetsMake(0.0, 0.0, 0.0, 0.0)

        // 设置 scrollIndicatorInsets 为 0
        scrollView.scrollIndicatorInsets = UIEdgeInsetsMake(0.0, 0.0, 0.0, 0.0)

        // 如果有 safe area insets，也去除它们
        scrollView.insetsLayoutMarginsFromSafeArea = false
    }

    println("iOS WebView 配置完成: disableScrolling=$disableScrolling, removeContentInsets=$removeContentInsets")
}