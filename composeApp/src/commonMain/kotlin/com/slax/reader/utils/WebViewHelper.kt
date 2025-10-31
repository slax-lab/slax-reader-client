package com.slax.reader.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slax.reader.const.articleStyle
import com.slax.reader.const.resetStyle

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

                body {
                    padding-top: 0px !important;
                    padding-left: 20px !important;
                    padding-right: 20px !important;
                    padding-bottom: 20px !important;
                }
            </style>
        </head>
        <body>
            $htmlContent
        </body>
        </html>
    """.trimIndent()
}

@Composable
expect fun AppWebView(
    url: String? = null,
    htmlContent: String? = null,
    modifier: Modifier = Modifier,
    topContentInsetPx: Float = 0f,
    onTap: (() -> Unit)? = null,
    onScrollChange: ((scrollY: Float) -> Unit)? = null,
)

@Composable
expect fun OpenInBrowserTab(url: String)

@Composable
expect fun WebView(
    url: String? = null,
    htmlContent: String? = null,
    modifier: Modifier,
    onScroll: ((x: Double, y: Double) -> Unit)? = null,
)
