package com.slax.reader.utils

private val WHITESPACE_RE = Regex("\\s+")

// 折叠连续空白为单空格并 trim
// 标题对齐 dweb：Compose 不像 HTML 自动折叠换行
fun String.collapseWhitespace(): String = replace(WHITESPACE_RE, " ").trim()
