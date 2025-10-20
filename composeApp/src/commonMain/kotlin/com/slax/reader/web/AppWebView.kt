package com.slax.reader.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun AppWebView(
    url: String? = null,
    htmlContent: String? = null,
    updateKey: String,
    modifier: Modifier = Modifier,
    onHeightChange: ((Double) -> Unit)? = null,
)

const val JS_BRIDGE_NAME = "NativeBridge"

const val HEIGHT_MONITOR_SCRIPT: String = """
    (function() {
        function getContentHeight() {
            return Math.max(
                document.body.scrollHeight,
                document.body.offsetHeight,
                document.documentElement.clientHeight,
                document.documentElement.scrollHeight,
                document.documentElement.offsetHeight
            );
        }
        function postHeight(h) {
            var payload = JSON.stringify({ type: 'height', height: h });
            if (window.NativeBridge && window.NativeBridge.postMessage) {
                window.NativeBridge.postMessage(payload);
            } else if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.NativeBridge) {
                window.webkit.messageHandlers.NativeBridge.postMessage(payload);
            }
        }
        var lastHeight = getContentHeight();
        postHeight(lastHeight);
        var observer = new MutationObserver(function() {
            var currentHeight = getContentHeight();
            if (currentHeight !== lastHeight) {
                lastHeight = currentHeight;
                postHeight(currentHeight);
            }
        });
        observer.observe(document.body, { childList: true, subtree: true, attributes: true, characterData: true });
        window.addEventListener('resize', function() {
            var currentHeight = getContentHeight();
            if (currentHeight !== lastHeight) {
                lastHeight = currentHeight;
                postHeight(currentHeight);
            }
        });
        var images = document.getElementsByTagName('img');
        for (var i = 0; i < images.length; i++) {
            images[i].addEventListener('load', function() {
                var currentHeight = getContentHeight();
                if (currentHeight !== lastHeight) {
                    lastHeight = currentHeight;
                    postHeight(currentHeight);
                }
            });
        }
    })();
"""
