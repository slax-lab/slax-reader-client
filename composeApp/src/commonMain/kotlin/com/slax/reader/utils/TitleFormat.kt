package com.slax.reader.utils

private val WHITESPACE_RE = Regex("\\s+")

// 折叠连续空白为单空格并 trim
fun String.collapseWhitespace(): String = replace(WHITESPACE_RE, " ").trim()
