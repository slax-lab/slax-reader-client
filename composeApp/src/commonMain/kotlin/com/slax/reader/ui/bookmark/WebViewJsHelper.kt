package com.slax.reader.ui.bookmark

import com.slax.reader.data.model.MarkDetail
import com.slax.reader.data.model.MarkPathItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * WebView JavaScript 调用助手类
 * 用于调用 slax-selection-bridge 的 JavaScript 方法
 */
object WebViewJsHelper {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * 初始化选择桥接
     */
    fun getInitBridgeScript(userId: Int, containerId: String = "article"): String {
        return """
            (function() {
                if (window.initializeBridge) {
                    window.initializeBridge($userId, '$containerId');
                    console.log('[WebView] Bridge initialized for user: $userId');
                } else {
                    console.error('[WebView] initializeBridge not found');
                }
            })();
        """.trimIndent()
    }

    /**
     * 设置内容到容器
     */
    fun getSetContentScript(htmlContent: String, containerId: String = "article"): String {
        val escapedContent = htmlContent
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")

        return """
            (function() {
                if (window.setContent) {
                    window.setContent('$escapedContent', '$containerId');
                    console.log('[WebView] Content set to container');
                } else {
                    console.error('[WebView] setContent not found');
                }
            })();
        """.trimIndent()
    }

    /**
     * 绘制所有标记
     */
    fun getDrawMarksScript(markDetail: MarkDetail): String {
        val markDetailJson = json.encodeToString(markDetail)
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")

        return """
            (function() {
                if (window.drawMarks) {
                    var markDetail = JSON.parse('$markDetailJson');
                    var result = window.drawMarks(markDetail);
                    console.log('[WebView] Marks drawn:', result);
                    return result;
                } else {
                    console.error('[WebView] drawMarks not found');
                    return null;
                }
            })();
        """.trimIndent()
    }

    /**
     * 绘制单个标记
     */
    fun getDrawMarkScript(
        markId: String?,
        paths: List<MarkPathItem>,
        markType: Int,
        userId: Int,
        comment: String = ""
    ): String {
        val pathsJson = json.encodeToString(paths)
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")

        val escapedComment = comment
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")

        val markIdParam = if (markId != null) "'$markId'" else "null"

        return """
            (function() {
                if (window.drawMark) {
                    var paths = JSON.parse('$pathsJson');
                    var result = window.drawMark($markIdParam, paths, $markType, $userId, '$escapedComment');
                    console.log('[WebView] Mark drawn:', result);
                    return result;
                } else {
                    console.error('[WebView] drawMark not found');
                    return null;
                }
            })();
        """.trimIndent()
    }

    /**
     * 根据 UUID 删除标记
     */
    fun getRemoveMarkScript(uuid: String): String {
        return """
            (function() {
                if (window.removeMarkByUuid) {
                    window.removeMarkByUuid('$uuid');
                    console.log('[WebView] Mark removed: $uuid');
                } else {
                    console.error('[WebView] removeMarkByUuid not found');
                }
            })();
        """.trimIndent()
    }

    /**
     * 清除所有标记
     */
    fun getClearAllMarksScript(): String {
        return """
            (function() {
                if (window.clearAllMarks) {
                    window.clearAllMarks();
                    console.log('[WebView] All marks cleared');
                } else {
                    console.error('[WebView] clearAllMarks not found');
                }
            })();
        """.trimIndent()
    }

    /**
     * 启动选择监听
     */
    fun getStartMonitoringScript(): String {
        return """
            (function() {
                if (window.startMonitoring) {
                    window.startMonitoring();
                    console.log('[WebView] Monitoring started');
                } else {
                    console.error('[WebView] startMonitoring not found');
                }
            })();
        """.trimIndent()
    }

    /**
     * 停止选择监听
     */
    fun getStopMonitoringScript(): String {
        return """
            (function() {
                if (window.stopMonitoring) {
                    window.stopMonitoring();
                    console.log('[WebView] Monitoring stopped');
                } else {
                    console.error('[WebView] stopMonitoring not found');
                }
            })();
        """.trimIndent()
    }
}
