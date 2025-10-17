package com.slax.reader.ui.bookmark

fun getOptimizedHtml(htmlData: String): String {
    return """
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
                    -webkit-user-select: text;
                    -moz-user-select: text;
                    -ms-user-select: text;
                    user-select: text;
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
                
                // 文本选择和标注功能
                let annotations = [];
                let annotationId = 0;
                
                function addAnnotationStyles() {
                    const style = document.createElement('style');
                    style.textContent = `
                        .highlight {
                            background: linear-gradient(120deg, #a8e6cf 0%, #88d8a3 100%);
                            padding: 2px 0;
                            border-radius: 2px;
                            transition: all 0.2s ease;
                            cursor: pointer;
                            position: relative;
                        }
                        .highlight:hover {
                            box-shadow: 0 2px 8px rgba(168, 230, 207, 0.4);
                            transform: translateY(-1px);
                        }
                        .comment {
                            background: linear-gradient(120deg, #ffd3a5 0%, #fd9853 100%);
                            padding: 2px 0;
                            border-radius: 2px;
                            cursor: pointer;
                            position: relative;
                        }
                        .comment:hover {
                            box-shadow: 0 2px 8px rgba(255, 211, 165, 0.4);
                            transform: translateY(-1px);
                        }
                        .annotation-tooltip {
                            position: absolute;
                            background: var(--bg-color);
                            border: 1px solid var(--border-color);
                            border-radius: 8px;
                            padding: 8px 12px;
                            font-size: 14px;
                            box-shadow: 0 4px 12px var(--shadow);
                            z-index: 1000;
                            max-width: 200px;
                            word-wrap: break-word;
                            display: none;
                        }
                        .selection-menu {
                            position: absolute;
                            background: var(--bg-color);
                            border: 1px solid var(--border-color);
                            border-radius: 8px;
                            box-shadow: 0 4px 12px var(--shadow);
                            z-index: 1001;
                            display: none;
                            flex-direction: row;
                            gap: 2px;
                            padding: 4px;
                        }
                        .selection-btn {
                            background: var(--accent-color);
                            color: white;
                            border: none;
                            padding: 6px 10px;
                            border-radius: 4px;
                            cursor: pointer;
                            font-size: 12px;
                            transition: all 0.2s ease;
                        }
                        .selection-btn:hover {
                            transform: scale(1.05);
                            opacity: 0.9;
                        }
                        @media (prefers-color-scheme: dark) {
                            .highlight {
                                background: linear-gradient(120deg, #2d5a3d 0%, #1e3a2a 100%);
                            }
                            .comment {
                                background: linear-gradient(120deg, #5a4a2d 0%, #3a2a1e 100%);
                            }
                        }
                    `;
                    document.head.appendChild(style);
                }
                
                function wrapTextNode(textNode, start, end, className, annotationId) {
                    try {
                        const span = document.createElement('span');
                        span.className = className;
                        span.dataset.annotationId = annotationId;
                        
                        const beforeText = textNode.textContent.substring(0, start);
                        const selectedText = textNode.textContent.substring(start, end);
                        const afterText = textNode.textContent.substring(end);
                        
                        const parentNode = textNode.parentNode;
                        
                        if (beforeText) {
                            parentNode.insertBefore(document.createTextNode(beforeText), textNode);
                        }
                        
                        span.textContent = selectedText;
                        parentNode.insertBefore(span, textNode);
                        
                        if (afterText) {
                            parentNode.insertBefore(document.createTextNode(afterText), textNode);
                        }
                        
                        parentNode.removeChild(textNode);
                        return span;
                    } catch (error) {
                        console.error('Error in wrapTextNode:', error);
                        return null;
                    }
                }
                
                function addAnnotation(selectedText, range, type, comment = '') {
                    const id = ++annotationId;
                    const annotation = {
                        id: id,
                        text: selectedText,
                        type: type,
                        comment: comment,
                        range: range
                    };
                    
                    annotations.push(annotation);
                    
                    try {
                        const className = type === 'highlight' ? 'highlight' : 'comment';
                        
                        // 使用更简单直接的方法
                        const span = document.createElement('span');
                        span.className = className;
                        span.dataset.annotationId = id;
                        span.title = comment || ('高亮文本: ' + selectedText);
                        
                        // 直接包围选中的内容
                        range.surroundContents(span);
                        
                        if (type === 'comment' && comment) {
                            span.addEventListener('click', function(e) {
                                e.stopPropagation();
                                showTooltip(e.target, comment);
                            });
                        }
                        
                        console.log('Annotation added successfully:', annotation);
                    } catch (error) {
                        console.error('Error adding annotation:', error);
                        
                        // 如果 surroundContents 失败，使用备用方法
                        try {
                            const span = document.createElement('span');
                            span.className = type === 'highlight' ? 'highlight' : 'comment';
                            span.dataset.annotationId = id;
                            span.innerHTML = range.extractContents();
                            range.insertNode(span);
                            
                            if (type === 'comment' && comment) {
                                span.addEventListener('click', function(e) {
                                    e.stopPropagation();
                                    showTooltip(e.target, comment);
                                });
                            }
                        } catch (fallbackError) {
                            console.error('Fallback method also failed:', fallbackError);
                        }
                    }
                    
                    return annotation;
                }
                
                function showTooltip(element, text) {
                    hideTooltip();
                    
                    const tooltip = document.createElement('div');
                    tooltip.className = 'annotation-tooltip';
                    tooltip.textContent = text;
                    tooltip.style.display = 'block';
                    
                    document.body.appendChild(tooltip);
                    
                    const rect = element.getBoundingClientRect();
                    tooltip.style.left = rect.left + 'px';
                    tooltip.style.top = (rect.bottom + 5) + 'px';
                    
                    setTimeout(hideTooltip, 3000);
                }
                
                function hideTooltip() {
                    const tooltip = document.querySelector('.annotation-tooltip');
                    if (tooltip) {
                        tooltip.remove();
                    }
                }
                
                function showSelectionMenu(x, y, selectedText, range) {
                    hideSelectionMenu();
                    
                    const menu = document.createElement('div');
                    menu.className = 'selection-menu';
                    menu.style.display = 'flex';
                    menu.style.left = x + 'px';
                    menu.style.top = y + 'px';
                    
                    const highlightBtn = document.createElement('button');
                    highlightBtn.className = 'selection-btn';
                    highlightBtn.textContent = '🖍️ 高亮';
                    highlightBtn.onclick = function() {
                        addAnnotation(selectedText, range, 'highlight');
                        hideSelectionMenu();
                        window.getSelection().removeAllRanges();
                    };
                    
                    const commentBtn = document.createElement('button');
                    commentBtn.className = 'selection-btn';
                    commentBtn.textContent = '💬 评论';
                    commentBtn.onclick = function() {
                        const comment = prompt('请输入评论内容:');
                        if (comment) {
                            addAnnotation(selectedText, range, 'comment', comment);
                        }
                        hideSelectionMenu();
                        window.getSelection().removeAllRanges();
                    };
                    
                    menu.appendChild(highlightBtn);
                    menu.appendChild(commentBtn);
                    document.body.appendChild(menu);
                }
                
                function hideSelectionMenu() {
                    const menu = document.querySelector('.selection-menu');
                    if (menu) {
                        menu.remove();
                    }
                }
                
                function handleTextSelection() {
                    document.addEventListener('mouseup', function(e) {
                        // 增加延迟，确保选择操作完成
                        setTimeout(function() {
                            const selection = window.getSelection();
                            const selectedText = selection.toString().trim();
                            
                            console.log('Selection detected:', selectedText); // 调试日志
                            
                            if (selectedText.length > 0) {
                                const range = selection.getRangeAt(0);
                                // 移除节点类型检查限制，允许更多类型的选择
                                console.log('Showing selection menu at:', e.pageX, e.pageY); // 调试日志
                                showSelectionMenu(e.pageX, e.pageY - 40, selectedText, range);
                            } else {
                                hideSelectionMenu();
                            }
                        }, 100); // 增加延迟时间
                    });
                    
                    // 添加触摸事件支持（移动端）
                    document.addEventListener('touchend', function(e) {
                        setTimeout(function() {
                            const selection = window.getSelection();
                            const selectedText = selection.toString().trim();
                            
                            if (selectedText.length > 0) {
                                const range = selection.getRangeAt(0);
                                const touch = e.changedTouches[0];
                                if (touch) {
                                    showSelectionMenu(touch.pageX, touch.pageY - 40, selectedText, range);
                                }
                            }
                        }, 100);
                    });
                    
                    document.addEventListener('mousedown', function() {
                        hideSelectionMenu();
                        hideTooltip();
                    });
                }

                // 初始化所有增强功能
                document.addEventListener('DOMContentLoaded', function() {
                    enhanceImages();
                    enhanceTables();
                    enhanceCodeBlocks();
                    enableSmoothScrolling();
                    addReadingProgress();
                    
                    // 初始化标注功能
                    addAnnotationStyles();
                    handleTextSelection();
                    
                    // 页面加载动画
                    document.querySelector('.container').style.opacity = '1';
                    
                    console.log('Reader with annotation support enhanced successfully');
                });
            </script>
        </body>
        </html>
    """.trimIndent()
}