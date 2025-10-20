package com.slax.reader.utils

fun wrapHtmlWithCSS(htmlContent: String, cssContent: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <meta name="color-scheme" content="light only">
            <meta name="supported-color-schemes" content="light">
            <style>
                $cssContent
            </style>
        </head>
        <body>
            $htmlContent
        </body>
        </html>
    """.trimIndent()
}
