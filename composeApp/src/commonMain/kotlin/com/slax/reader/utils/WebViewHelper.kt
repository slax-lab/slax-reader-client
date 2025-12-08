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
 * 使用内联CSS的方式包装HTML内容（旧方案，保留作为fallback）
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
 * 使用外部资源文件的方式生成HTML（新方案）
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
        println("[WebViewHelper] 读取HTML模板失败，使用fallback方案: ${e.message}")
        // 如果读取失败，回退到旧方案
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
)

@Composable
expect fun WebView(
    url: String,
    modifier: Modifier,
    contentInsets: PaddingValues? = null,
    onScroll: ((x: Double, y: Double) -> Unit)? = null,
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