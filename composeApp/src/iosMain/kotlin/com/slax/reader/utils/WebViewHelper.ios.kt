package com.slax.reader.utils

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIScrollView
import platform.WebKit.WKWebView

@OptIn(ExperimentalForeignApi::class)
actual fun configureIOSWebView(webView: Any) {
    if (webView !is WKWebView) return

    val scrollView: UIScrollView = webView.scrollView
    webView.opaque = false
    removePanGestures(scrollView)
}

@OptIn(ExperimentalForeignApi::class)
private fun removePanGestures(scrollView: UIScrollView) {
    scrollView.gestureRecognizers?.toList()?.forEach { recognizer ->
        (recognizer as? platform.UIKit.UIPanGestureRecognizer)?.let {
            scrollView.removeGestureRecognizer(it)
        }
    }
}
