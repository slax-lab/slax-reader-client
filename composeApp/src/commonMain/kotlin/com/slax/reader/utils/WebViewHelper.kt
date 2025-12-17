package com.slax.reader.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.slax.reader.SlaxConfig
import com.slax.reader.const.articleStyle
import com.slax.reader.const.bottomLineStyle
import com.slax.reader.const.resetStyle
import com.slax.reader.data.preferences.AppPreferences
import kotlinx.coroutines.runBlocking

fun wrapBookmarkDetailHtml(htmlContent: String): String {
    return SlaxConfig.WEBVIEW_TEMPLATE.replace("{{CONTENT}}", htmlContent)
}

@Composable
expect fun AppWebView(
    htmlContent: String,
    modifier: Modifier = Modifier,
    webState: AppWebViewState
)

@Composable
expect fun WebView(
    url: String,
    modifier: Modifier,
    contentInsets: PaddingValues? = null,
    onScroll: ((scrollX: Double, scrollY: Double, contentHeight: Double, visibleHeight: Double) -> Unit)? = null,
    onPageLoaded: (() -> Unit)? = null,
    injectUser: Boolean = false,
)

@Composable
expect fun OpenInBrowser(url: String)

fun getDoNotAlertSetting(appPreference: AppPreferences): Boolean {
    return runBlocking {
        appPreference.getUserSettingDetailDoNotAlert()
    }
}

fun setDoNotAlertSetting(appPreference: AppPreferences) {
    runBlocking {
        appPreference.setUserSettingDetailDoNotAlert(true)
    }
}
