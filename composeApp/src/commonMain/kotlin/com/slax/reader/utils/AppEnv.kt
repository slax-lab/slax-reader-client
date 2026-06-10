package com.slax.reader.utils

import app.slax.reader.SlaxConfig

enum class AppEnvironment {
    PRODUCTION,
    BETA,
}

object AppEnv {
    private var selected: AppEnvironment? = null

    fun init(saved: String?) {
        selected = saved?.let { runCatching { AppEnvironment.valueOf(it) }.getOrNull() }
    }

    val current: AppEnvironment?
        get() = selected

    val isBeta: Boolean
        get() = selected == AppEnvironment.BETA

    val apiBaseUrl: String
        get() = when (selected) {
            AppEnvironment.PRODUCTION -> SlaxConfig.API_BASE_URL
            AppEnvironment.BETA -> SlaxConfig.API_BETA_URL
            null -> SlaxConfig.API_BASE_URL
        }
}
