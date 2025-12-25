package com.slax.reader.utils

import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

actual fun getSystemLanguageCode() = NSLocale.preferredLanguages().firstOrNull()?.toString() ?: "en"