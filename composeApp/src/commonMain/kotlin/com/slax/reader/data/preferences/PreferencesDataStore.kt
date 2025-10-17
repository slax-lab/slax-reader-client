package com.slax.reader.data.preferences

import org.koin.core.module.Module

expect val preferencesPlatformModule: Module

expect fun getPreferences(): AppPreferences