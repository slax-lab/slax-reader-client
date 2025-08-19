package com.slax.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.multiplatform.webview.setting.PlatformWebSettings
import com.multiplatform.webview.util.KLogSeverity
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData

@Composable
fun ChromeReaderView(nav: NavController) {
    val optimizedHtml = """
        <!DOCTYPE html>
        <html lang="zh-CN">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.5, minimum-scale=0.8, user-scalable=yes">
            <meta name="format-detection" content="telephone=no">
            <meta name="theme-color" content="#ffffff">
            <meta name="theme-color" content="#121212" media="(prefers-color-scheme: dark)">
            <title>Reader</title>
            <style>
                :root {
                    --bg-color: #ffffff;
                    --text-color: #333333;
                    --text-secondary: #666666;
                    --border-color: #e1e4e8;
                    --link-color: #0066cc;
                    --accent-color: #0066cc;
                    --table-header-bg: #f6f8fa;
                    --code-bg: #f6f8fa;
                    --quote-bg: #f8f9fa;
                    --shadow: rgba(0,0,0,0.1);
                }
                
                @media (prefers-color-scheme: dark) {
                    :root {
                        --bg-color: #121212;
                        --text-color: #e1e1e1;
                        --text-secondary: #b3b3b3;
                        --border-color: #404040;
                        --link-color: #8ab4f8;
                        --accent-color: #8ab4f8;
                        --table-header-bg: #2d2d2d;
                        --code-bg: #2d2d2d;
                        --quote-bg: #1e1e1e;
                        --shadow: rgba(255,255,255,0.1);
                    }
                }
                
                * {
                    box-sizing: border-box;
                    -webkit-tap-highlight-color: transparent;
                }
                
                html, body {
                    margin: 0;
                    padding: 0;
                    width: 100%;
                    overflow-x: hidden;
                    -webkit-text-size-adjust: none;
                    -ms-text-size-adjust: none;
                    text-size-adjust: none;
                }
                
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Noto Sans CJK SC', Roboto, sans-serif;
                    font-size: 16px;
                    line-height: 1.6;
                    color: var(--text-color);
                    background-color: var(--bg-color);
                    word-wrap: break-word;
                    word-break: break-word;
                    -webkit-font-smoothing: antialiased;
                    -moz-osx-font-smoothing: grayscale;
                    transition: background-color 0.3s ease, color 0.3s ease;
                }
                
                .container {
                    width: 100%;
                    max-width: 100%;
                    padding: 20px;
                    animation: fadeIn 0.3s ease-out;
                }
                
                @keyframes fadeIn {
                    from { opacity: 0; transform: translateY(10px); }
                    to { opacity: 1; transform: translateY(0); }
                }
                
                /* 标题优化 */
                h1, h2, h3, h4, h5, h6 {
                    margin: 1.8em 0 0.8em 0;
                    line-height: 1.3;
                    color: var(--text-color);
                    font-weight: 600;
                    scroll-margin-top: 20px;
                }
                
                h1 { font-size: 1.8em; margin-top: 0; }
                h2 { font-size: 1.6em; }
                h3 { font-size: 1.4em; }
                h4 { font-size: 1.2em; }
                h5 { font-size: 1.1em; }
                h6 { font-size: 1em; }
                
                /* 段落优化 */
                p {
                    margin: 1em 0;
                    text-align: justify;
                    text-justify: inter-word;
                    word-wrap: break-word;
                    overflow-wrap: break-word;
                    hyphens: auto;
                    -webkit-hyphens: auto;
                    -ms-hyphens: auto;
                }
                
                /* 图片优化 */
                img {
                    max-width: 100%;
                    height: auto;
                    display: block;
                    margin: 1.5em 0;
                    border-radius: 12px;
                    box-shadow: 0 4px 12px var(--shadow);
                    transition: transform 0.2s ease, box-shadow 0.2s ease;
                    will-change: transform;
                }
                
                img:hover {
                    transform: scale(1.02);
                    box-shadow: 0 6px 20px var(--shadow);
                }
                
                /* 表格优化 */
                table {
                    width: 100%;
                    border-collapse: collapse;
                    margin: 1.5em 0;
                    border-radius: 12px;
                    overflow: hidden;
                    box-shadow: 0 4px 12px var(--shadow);
                    background: var(--bg-color);
                    border: 1px solid var(--border-color);
                }
                
                th, td {
                    padding: 14px 16px;
                    text-align: left;
                    border-bottom: 1px solid var(--border-color);
                    vertical-align: top;
                }
                
                th {
                    background-color: var(--table-header-bg);
                    font-weight: 600;
                    font-size: 0.95em;
                    letter-spacing: 0.5px;
                    text-transform: uppercase;
                    color: var(--text-secondary);
                }
                
                tr:last-child td {
                    border-bottom: none;
                }
                
                tr:hover {
                    background-color: var(--quote-bg);
                }
                
                /* 代码块优化 */
                pre, code {
                    font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, 'Courier New', monospace;
                    background-color: var(--code-bg);
                    border-radius: 8px;
                    font-size: 0.9em;
                }
                
                code {
                    padding: 3px 6px;
                    border: 1px solid var(--border-color);
                    word-break: break-all;
                }
                
                pre {
                    padding: 20px;
                    overflow-x: auto;
                    margin: 1.5em 0;
                    border: 1px solid var(--border-color);
                    line-height: 1.4;
                    position: relative;
                    -webkit-overflow-scrolling: touch;
                    box-shadow: 0 2px 8px var(--shadow);
                }
                
                pre code {
                    background: none;
                    border: none;
                    padding: 0;
                    font-size: inherit;
                    word-break: normal;
                }
                
                /* 引用优化 */
                blockquote {
                    margin: 1.5em 0;
                    padding: 1.2em 1.5em;
                    border-left: 4px solid var(--accent-color);
                    background-color: var(--quote-bg);
                    border-radius: 0 12px 12px 0;
                    font-style: italic;
                    position: relative;
                    box-shadow: 0 2px 8px var(--shadow);
                }
                
                blockquote::before {
                    content: '"';
                    font-size: 4em;
                    color: var(--accent-color);
                    position: absolute;
                    top: -10px;
                    left: 10px;
                    opacity: 0.3;
                    font-family: Georgia, serif;
                }
                
                /* 链接优化 */
                a {
                    color: var(--link-color);
                    text-decoration: none;
                    border-bottom: 1px solid transparent;
                    transition: all 0.2s ease;
                    padding: 2px 4px;
                    border-radius: 4px;
                }
                
                a:hover, a:active {
                    border-bottom-color: var(--link-color);
                    background-color: var(--quote-bg);
                    transform: translateY(-1px);
                }
                
                /* 列表优化 */
                ul, ol {
                    margin: 1em 0;
                    padding-left: 2em;
                }
                
                li {
                    margin: 0.6em 0;
                    line-height: 1.6;
                }
                
                ul li::marker {
                    color: var(--accent-color);
                }
                
                ol li::marker {
                    color: var(--accent-color);
                    font-weight: 600;
                }
                
                /* 分割线 */
                hr {
                    border: none;
                    height: 2px;
                    background: linear-gradient(90deg, transparent, var(--border-color), transparent);
                    margin: 2em 0;
                }
                
                /* 响应式优化 */
                @media screen and (max-width: 768px) {
                    .container {
                        padding: 16px;
                    }
                    body {
                        font-size: 15px;
                    }
                    h1 { font-size: 1.6em; }
                    h2 { font-size: 1.4em; }
                    h3 { font-size: 1.2em; }
                    h4 { font-size: 1.1em; }
                    
                    table {
                        font-size: 0.9em;
                        display: block;
                        overflow-x: auto;
                        white-space: nowrap;
                    }
                    
                    th, td {
                        padding: 10px 12px;
                        min-width: 120px;
                    }
                    
                    pre {
                        padding: 16px;
                        font-size: 0.85em;
                    }
                    
                    blockquote {
                        padding: 1em;
                        margin: 1em 0;
                    }
                }
                
                @media screen and (max-width: 480px) {
                    .container {
                        padding: 12px;
                    }
                    body {
                        font-size: 14px;
                    }
                }
                
                /* 滚动条美化 */
                ::-webkit-scrollbar {
                    width: 6px;
                    height: 6px;
                }
                
                ::-webkit-scrollbar-track {
                    background: transparent;
                }
                
                ::-webkit-scrollbar-thumb {
                    background: var(--border-color);
                    border-radius: 3px;
                    transition: background 0.2s ease;
                }
                
                ::-webkit-scrollbar-thumb:hover {
                    background: var(--text-secondary);
                }
                
                /* 选择文本优化 */
                ::selection {
                    background: var(--accent-color);
                    color: var(--bg-color);
                }
                
                ::-moz-selection {
                    background: var(--accent-color);
                    color: var(--bg-color);
                }
                
                /* 防止水平滚动 */
                * {
                    max-width: 100%;
                }
            </style>
        </head>
        <body>
            <div class="container">
                $htmlData
            </div>
            
            <script>
                // 图片懒加载
                function enhanceImages() {
                    const images = document.querySelectorAll('img');
                    
                    if ('IntersectionObserver' in window) {
                        const imageObserver = new IntersectionObserver((entries) => {
                            entries.forEach(entry => {
                                if (entry.isIntersecting) {
                                    const img = entry.target;
                                    img.style.opacity = '0';
                                    img.style.transition = 'opacity 0.3s ease';
                                    
                                    const tempImg = new Image();
                                    tempImg.onload = () => {
                                        img.style.opacity = '1';
                                    };
                                    tempImg.onerror = () => {
                                        img.style.opacity = '0.5';
                                        img.alt = '图片加载失败';
                                        img.style.background = 'var(--quote-bg)';
                                        img.style.border = '2px dashed var(--border-color)';
                                    };
                                    tempImg.src = img.src;
                                    
                                    imageObserver.unobserve(img);
                                }
                            });
                        }, {
                            rootMargin: '50px'
                        });
                        
                        images.forEach(img => {
                            imageObserver.observe(img);
                        });
                    }
                }
                
                function enhanceTables() {
                    const tables = document.querySelectorAll('table');
                    tables.forEach(table => {
                        const wrapper = document.createElement('div');
                        wrapper.style.cssText = 'overflow-x: auto; -webkit-overflow-scrolling: touch; margin: 1.5em 0; border-radius: 12px; box-shadow: 0 4px 12px var(--shadow);';
                        table.parentNode.insertBefore(wrapper, table);
                        wrapper.appendChild(table);
                        
                        if (table.scrollWidth > table.clientWidth) {
                            const hint = document.createElement('div');
                            hint.textContent = '← Side →';
                            hint.style.cssText = 'text-align: center; font-size: 12px; color: var(--text-secondary); padding: 8px; background: var(--quote-bg); border-top: 1px solid var(--border-color);';
                            wrapper.appendChild(hint);
                        }
                    });
                }
                
                function enhanceCodeBlocks() {
                    const codeBlocks = document.querySelectorAll('pre');
                    codeBlocks.forEach(block => {
                        const copyBtn = document.createElement('button');
                        copyBtn.innerHTML = '📋';
                        copyBtn.title = 'copy';
                        copyBtn.style.cssText = 'position: absolute; top: 12px; right: 12px; background: var(--bg-color); color: var(--text-color); border: 1px solid var(--border-color); border-radius: 6px; padding: 6px 8px; font-size: 12px; cursor: pointer; transition: all 0.2s ease; z-index: 10;';
                        
                        block.style.position = 'relative';
                        block.appendChild(copyBtn);
                        
                        copyBtn.onclick = () => {
                            if (navigator.clipboard) {
                                navigator.clipboard.writeText(block.textContent).then(() => {
                                    copyBtn.innerHTML = '✅';
                                    copyBtn.title = 'copied';
                                    setTimeout(() => {
                                        copyBtn.innerHTML = '📋';
                                        copyBtn.title = 'copy';
                                    }, 2000);
                                });
                            }
                        };
                    });
                }
                
                // 平滑滚动
                function enableSmoothScrolling() {
                    const links = document.querySelectorAll('a[href^="#"]');
                    links.forEach(link => {
                        link.addEventListener('click', (e) => {
                            e.preventDefault();
                            const target = document.querySelector(link.getAttribute('href'));
                            if (target) {
                                target.scrollIntoView({
                                    behavior: 'smooth',
                                    block: 'start'
                                });
                            }
                        });
                    });
                }
                
                // 阅读进度
                function addReadingProgress() {
                    const progressBar = document.createElement('div');
                    progressBar.style.cssText = 'position: fixed; top: 0; left: 0; width: 0%; height: 3px; background: var(--accent-color); z-index: 1000; transition: width 0.1s ease;';
                    document.body.appendChild(progressBar);
                    
                    window.addEventListener('scroll', () => {
                        const scrolled = (window.pageYOffset / (document.body.scrollHeight - window.innerHeight)) * 100;
                        progressBar.style.width = Math.min(scrolled, 100) + '%';
                    }, { passive: true });
                }
                
                // 防止双击缩放（保持你的原有功能）
                document.addEventListener('touchstart', function(event) {
                    if (event.touches.length > 1) {
                        event.preventDefault();
                    }
                }, { passive: false });
                
                let lastTouchEnd = 0;
                document.addEventListener('touchend', function(event) {
                    const now = (new Date()).getTime();
                    if (now - lastTouchEnd <= 300) {
                        event.preventDefault();
                    }
                    lastTouchEnd = now;
                }, { passive: false });
                
                // 限制缩放（保持你的原有功能）
                document.addEventListener('gesturestart', function(e) {
                    e.preventDefault();
                }, { passive: false });
                
                document.addEventListener('gesturechange', function(e) {
                    e.preventDefault();
                }, { passive: false });
                
                document.addEventListener('gestureend', function(e) {
                    e.preventDefault();
                }, { passive: false });
                
                // 初始化所有增强功能
                document.addEventListener('DOMContentLoaded', function() {
                    enhanceImages();
                    enhanceTables();
                    enhanceCodeBlocks();
                    enableSmoothScrolling();
                    addReadingProgress();
                    
                    // 页面加载动画
                    document.querySelector('.container').style.opacity = '1';
                    
                    console.log('Reader enhanced successfully');
                });
            </script>
        </body>
        </html>
    """.trimIndent()

    val webState = rememberWebViewStateWithHTMLData(optimizedHtml)

    LaunchedEffect(Unit) {
        webState.webSettings.apply {
            isJavaScriptEnabled = true
            supportZoom = false
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
            logSeverity = KLogSeverity.Error

            androidWebSettings.apply {
                domStorageEnabled = true
                safeBrowsingEnabled = true
                allowFileAccess = false
                layerType = PlatformWebSettings.AndroidWebSettings.LayerType.HARDWARE
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        WebView(
            state = webState,
            modifier = Modifier.fillMaxSize(),
            captureBackPresses = true
        )
    }
}