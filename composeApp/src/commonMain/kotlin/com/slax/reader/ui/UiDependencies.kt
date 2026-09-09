package com.slax.reader.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.LifecycleOwner
import com.slax.reader.utils.AppLifecycleState
import com.slax.reader.utils.AppWebViewState
import com.slax.reader.utils.AppWebView
import com.slax.reader.utils.WebView
import kotlinx.coroutines.flow.StateFlow

/** Lifecycle boundary used by screens and replaceable by deterministic tests. */
interface AppLifecycle {
    val state: StateFlow<AppLifecycleState>
    fun attach(owner: LifecycleOwner)
    fun detach(owner: LifecycleOwner)
}

/** Platform WebView boundary. Tests can provide a semantics-only host. */
interface WebViewHost {
    @Composable
    fun Url(
        url: String,
        modifier: Modifier,
        webState: AppWebViewState,
        contentInsets: PaddingValues? = null,
    )

    @Composable
    fun Html(
        htmlContent: String,
        modifier: Modifier,
        webState: AppWebViewState,
        bookmarkId: String,
    )
}

object PlatformWebViewHost : WebViewHost {
    @Composable
    override fun Url(
        url: String,
        modifier: Modifier,
        webState: AppWebViewState,
        contentInsets: PaddingValues?,
    ) = WebView(url, modifier, webState, contentInsets)

    @Composable
    override fun Html(
        htmlContent: String,
        modifier: Modifier,
        webState: AppWebViewState,
        bookmarkId: String,
    ) = AppWebView(htmlContent, modifier, webState, bookmarkId)
}
