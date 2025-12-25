package com.slax.reader.utils

import java.util.Locale

actual fun getSystemLanguageCode() = Locale.getDefault().toLanguageTag() ?: "en"