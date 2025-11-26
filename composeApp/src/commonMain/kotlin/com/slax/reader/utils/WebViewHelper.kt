package com.slax.reader.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slax.reader.const.articleStyle
import com.slax.reader.const.bottomLineStyle
import com.slax.reader.const.resetStyle

fun wrapHtmlWithCSS(htmlContent: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <meta name="color-scheme" content="light only">
            <meta name="supported-color-schemes" content="light">
            <style>
                $articleStyle
                $resetStyle
                $bottomLineStyle

                body {
                    padding-top: 0px !important;
                    padding-left: 20px !important;
                    padding-right: 20px !important;
                    padding-bottom: 150px !important;
                }

                /* 标记样式 */
                slax-mark {
                    background: transparent;
                    cursor: pointer;
                    transition: all 0.2s;
                }

                slax-mark.stroke {
                    background: rgba(255, 235, 59, 0.4);
                    border-bottom: 2px solid #FBC02D;
                }

                slax-mark.comment {
                    background: rgba(76, 175, 80, 0.3);
                    border-left: 3px solid #4CAF50;
                    padding-left: 2px;
                }

                slax-mark.stroke.comment {
                    background: rgba(255, 152, 0, 0.3);
                    border-bottom: 2px solid #F57C00;
                    border-left: 3px solid #FF9800;
                }

                slax-mark.self-stroke {
                    background: rgba(33, 150, 243, 0.3);
                    border-bottom: 2px solid #1976D2;
                }

                slax-mark.highlighted {
                    background: rgba(255, 87, 34, 0.5) !important;
                    animation: pulse 0.5s ease-in-out;
                }

                @keyframes pulse {
                    0%, 100% { background: rgba(255, 87, 34, 0.5); }
                    50% { background: rgba(255, 87, 34, 0.8); }
                }
            </style>
        </head>
        <body lang="en">
            <div id="article-content">
                $htmlContent
            </div>
            <div class="bottom-seperator-line">
              <div class="seperator-line"></div>
            </div>

            <script>
                // 全局 Bridge 实例
                let selectionBridge = null;

                // 初始化函数 - 在 JS 加载后自动调用
                window.initializeBridge = function(currentUserId) {
                    try {
                        if (typeof SlaxSelectionBridge === 'undefined') {
                            console.error('[Bridge] SlaxSelectionBridge not loaded');
                            return false;
                        }

                        const container = document.getElementById('article-content');
                        if (!container) {
                            console.error('[Bridge] Container not found');
                            return false;
                        }

                        selectionBridge = new SlaxSelectionBridge({
                            containerElement: container,
                            currentUserId: currentUserId || 1,
                            debug: true
                        });

                        // 开始监听文本选择
                        selectionBridge.startMonitoring();

                        console.log('[Bridge] Initialized successfully');

                        // 通知原生端初始化完成
                        if (window.SlaxBridge && window.SlaxBridge.onBridgeInitialized) {
                            window.SlaxBridge.onBridgeInitialized();
                        }

                        return true;
                    } catch (error) {
                        console.error('[Bridge] Initialization failed:', error);
                        if (window.SlaxBridge && window.SlaxBridge.onError) {
                            window.SlaxBridge.onError('Bridge initialization failed: ' + error.message);
                        }
                        return false;
                    }
                };

                // 绘制单个标记
                window.drawMark = function(markId, paths, markType, userId, comment) {
                    try {
                        if (!selectionBridge) {
                            console.error('[Bridge] Bridge not initialized');
                            return null;
                        }

                        // markType: 1 = LINE (划线), 2 = COMMENT (评论)
                        const isStroke = markType === 1;
                        const hasComment = markType === 2 || (comment && comment.length > 0);

                        const id = selectionBridge.drawMark(markId, paths, isStroke, hasComment, userId);
                        console.log('[Bridge] Mark drawn:', id);

                        if (window.SlaxBridge && window.SlaxBridge.onMarkRendered) {
                            window.SlaxBridge.onMarkRendered(id, true);
                        }

                        return id;
                    } catch (error) {
                        console.error('[Bridge] Failed to draw mark:', error);
                        if (window.SlaxBridge && window.SlaxBridge.onError) {
                            window.SlaxBridge.onError('Failed to draw mark: ' + error.message);
                        }
                        return null;
                    }
                };

                // 删除标记
                window.removeMarkByUuid = function(uuid) {
                    try {
                        if (!selectionBridge) {
                            console.error('[Bridge] Bridge not initialized');
                            return false;
                        }

                        selectionBridge.removeMarkByUuid(uuid);
                        console.log('[Bridge] Mark removed:', uuid);
                        return true;
                    } catch (error) {
                        console.error('[Bridge] Failed to remove mark:', error);
                        return false;
                    }
                };

                // 绘制所有标记
                window.drawMarks = function(markDetail) {
                    try {
                        if (!selectionBridge) {
                            console.error('[Bridge] Bridge not initialized');
                            return {};
                        }

                        const result = selectionBridge.drawMarks(markDetail);
                        console.log('[Bridge] Marks drawn:', result);
                        return result;
                    } catch (error) {
                        console.error('[Bridge] Failed to draw marks:', error);
                        return {};
                    }
                };

                // Note: Bridge is initialized from LOAD_SELECTION_BRIDGE_SCRIPT after the script loads
                // Do not auto-initialize here as DOMContentLoaded may fire before scripts are injected
            </script>
        </body>
        </html>
    """.trimIndent()
}

@Composable
expect fun AppWebView(
    url: String? = null,
    htmlContent: String? = null,
    modifier: Modifier = Modifier,
    topContentInsetPx: Float = 0f,
    onTap: (() -> Unit)? = null,
    onScrollChange: ((scrollY: Float, contentHeight: Float, visibleHeight: Float) -> Unit)? = null,
    onJsMessage: ((message: String) -> Unit)? = null,
)

@Composable
expect fun OpenInBrowserTab(url: String)

@Composable
expect fun WebView(
    url: String? = null,
    htmlContent: String? = null,
    modifier: Modifier,
    contentInsets: PaddingValues? = null,
    onScroll: ((x: Double, y: Double) -> Unit)? = null,
)
