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
    onHeightChange: ((Double) -> Unit)? = null,
    onTap: (() -> Unit)? = null,
)

@Composable
expect fun OpenInBrowserTab(url: String)