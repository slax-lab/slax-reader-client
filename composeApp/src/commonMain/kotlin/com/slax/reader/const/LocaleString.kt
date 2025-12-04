package com.slax.reader.const

val localeString: Map<String, Map<String, String>> = mapOf(
    "welcome_message" to mapOf(
        "zh" to "欢迎，{0}！",
        "en" to "Welcome, {0}!"
    ),
    "items_count" to mapOf(
        "zh" to "您有 {0} 个项目。",
        "en" to "You have {0} items."
    ),
    "user_greeting" to mapOf(
        "zh" to "你好，{name}，今天是 {day}。",
        "en" to "Hello, {name}, today is {day}."
    )
)