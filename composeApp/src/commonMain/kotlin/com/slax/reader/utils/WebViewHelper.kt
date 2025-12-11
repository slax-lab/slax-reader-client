package com.slax.reader.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slax.reader.const.WebViewAssets
import com.slax.reader.const.articleStyle
import com.slax.reader.const.bottomLineStyle
import com.slax.reader.const.resetStyle
import com.slax.reader.data.preferences.AppPreferences
import kotlinx.coroutines.runBlocking

/**
 * 使用内联CSS的方式包装HTML内容（fallback）
 */
fun wrapHtmlWithCSS(htmlContent: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <meta name="color-scheme" content="light only">
            <meta name="supported-color-schemes" content="light">
            <style>
                $articleStyle
                $resetStyle
                $bottomLineStyle

                body {
                    padding-top: 0px !important;
                    padding-left: 20px !important;
                    padding-right: 20px !important;
                    padding-bottom: 150px !important;
                }
            </style>
        </head>
        <body lang="en">
            $htmlContent
            <div class="bottom-seperator-line">
              <div class="seperator-line"></div>
            </div>
        </body>
        </html>
    """.trimIndent()
}

/**
 * 使用外部资源文件的方式生成HTML
 *
 * @param contentHtml 文章内容HTML
 * @return 完整的HTML字符串，包含外部CSS和JS引用
 */
suspend fun generateHtmlWithExternalResources(contentHtml: String): String {
    return try {
        // 读取HTML模板
        val template = WebViewResourceLoader.readResource(WebViewAssets.Paths.HTML_TEMPLATE)

        // 替换内容占位符
        template.replace("{{CONTENT}}", contentHtml)
    } catch (e: Exception) {
        println("[WebViewHelper] 读取HTML模板失败，使用fallback: ${e.message}")
        // fallback
        wrapHtmlWithCSS(contentHtml)
    }
}

@Composable
expect fun AppWebView(
    htmlContent: String,
    modifier: Modifier = Modifier,
    topContentInsetPx: Float = 0f,
    onTap: (() -> Unit)? = null,
    onScrollChange: ((scrollY: Float, contentHeight: Float, visibleHeight: Float) -> Unit)? = null,
    onJsMessage: ((message: String) -> Unit)? = null,
    evaluateJsCommand: String? = null,  // 新增：JS 执行命令
)

@Composable
expect fun WebView(
    url: String,
    modifier: Modifier,
    contentInsets: PaddingValues? = null,
    onScroll: ((scrollX: Double, scrollY: Double, contentHeight: Double, visibleHeight: Double) -> Unit)? = null,
)

@Composable
expect fun OpenInBrowser(url: String)

fun getDoNotAlertSetting(appPreference: AppPreferences): Boolean {
    return runBlocking {
        appPreference.getUserSettingDetailDoNotAlert()
    }
}

fun setDoNotAlertSetting(appPreference: AppPreferences) {
    runBlocking {
        appPreference.setUserSettingDetailDoNotAlert(true)
    }
}

/**
 * 转义 JavaScript 模板字符串中的特殊字符
 *
 * 用于将任意字符串安全地嵌入到 JavaScript 模板字符串（反引号包围的字符串）中，
 * 防止注入攻击和语法错误。
 *
 * @param input 需要转义的原始字符串
 * @return 转义后的安全字符串
 *
 * @example
 * ```kotlin
 * val anchorText = "Section `1.0` test"
 * val escaped = escapeJsTemplateString(anchorText)  // "Section \`1.0\` test"
 * val jsCommand = "window.SlaxWebViewBridge.scrollToAnchor(`$escaped`)"
 * ```
 */
fun escapeJsTemplateString(input: String): String {
    return input
        .replace("\\", "\\\\")  // 反斜杠必须最先处理，避免二次转义
        .replace("`", "\\`")     // 反引号（模板字符串分隔符）
        .replace("$", "\\$")     // 美元符号（模板字符串插值）
        .replace("\n", "\\n")    // 换行符
        .replace("\r", "\\r")    // 回车符
        .replace("\t", "\\t")    // 制表符
}