/**
 * WebView Bridge - Native与WebView通信桥接
 *
 * 功能：
 * 1. 提供统一的Native Bridge通信接口
 * 2. 处理图片点击事件并通知Native
 * 3. 支持锚点跳转和元素高亮显示
 * 4. 递归查找DOM元素（支持嵌套结构的精确定位）
 * 5. 支持Android和iOS平台
 *
 * 元素查找策略：
 * - 优先级1：通过 ID 和 name 属性精确匹配
 * - 优先级2：递归遍历 DOM 树，从文本节点开始查找
 * - 优先级3：优先返回标题元素（h1-h6）
 * - 特性：自动过滤隐藏元素，支持多结果去重
 *
 * @author Slax Reader
 * @version 2.0
 */

(function() {
    'use strict';

    // CSS.escape polyfill（兼容旧浏览器）
    if (!window.CSS || !CSS.escape) {
        window.CSS = window.CSS || {};
        CSS.escape = function(value) {
            if (arguments.length === 0) {
                throw new TypeError('`CSS.escape` 需要一个参数。');
            }
            var string = String(value);
            var length = string.length;
            var index = -1;
            var codeUnit;
            var result = '';
            var firstCodeUnit = string.charCodeAt(0);
            while (++index < length) {
                codeUnit = string.charCodeAt(index);
                // 注意：处理代理对等复杂情况
                if (codeUnit === 0x0000) {
                    result += '\uFFFD';
                    continue;
                }
                if (
                    // 如果字符是 [0-9A-Za-z_-]
                    (codeUnit >= 0x0030 && codeUnit <= 0x0039) ||
                    (codeUnit >= 0x0041 && codeUnit <= 0x005A) ||
                    (codeUnit >= 0x0061 && codeUnit <= 0x007A) ||
                    codeUnit === 0x005F ||
                    codeUnit === 0x002D
                ) {
                    result += string.charAt(index);
                    continue;
                }
                // 转义其他字符
                result += '\\' + string.charAt(index);
            }
            return result;
        };
    }

    /**
     * 检测当前运行的平台
     * @returns {'android'|'ios'|'unknown'} - 平台类型
     */
    function detectPlatform() {
        // Android平台：检查是否存在 NativeBridge.postMessage
        if (window.NativeBridge?.postMessage) {
            return 'android';
        }

        // iOS平台：检查是否存在 webkit.messageHandlers.NativeBridge
        if (window.webkit?.messageHandlers?.NativeBridge) {
            return 'ios';
        }

        return 'unknown';
    }

    /**
     * 向Native发送消息
     * @param {Object} payload - 消息负载
     * @returns {boolean} - 是否成功发送
     */
    function postToNativeBridge(payload) {
        const message = JSON.stringify(payload);
        const platform = detectPlatform();

        // Android平台
        if (platform === 'android') {
            window.NativeBridge.postMessage(message);
            return true;
        }

        // iOS平台
        if (platform === 'ios') {
            window.webkit.messageHandlers.NativeBridge.postMessage(message);
            return true;
        }

        console.warn('Native bridge not available');
        return false;
    }

    /**
     * 获取页面内容高度
     * @returns {number} - 页面高度（像素）
     */
    function getContentHeight() {
        return Math.max(
            document.body.scrollHeight,
            document.body.offsetHeight,
            document.documentElement.clientHeight,
            document.documentElement.scrollHeight,
            document.documentElement.offsetHeight
        );
    }

    /**
     * 获取图片元素的URL
     * @param {HTMLElement} element - 图片元素
     * @returns {string} - 图片URL
     */
    function getImageUrl(element) {
        const tagName = element.tagName.toLowerCase();

        if (tagName === 'img') {
            return element.currentSrc || element.src || '';
        }

        if (tagName === 'image') {
            return element.href?.baseVal ||
                   element.getAttribute('href') ||
                   element.getAttribute('xlink:href') ||
                   '';
        }

        return '';
    }

    /**
     * 初始化图片点击事件
     */
    function initImageClickHandlers() {
        const images = document.querySelectorAll('img, image');

        images.forEach(img => {
            // 清除可能影响显示的样式
            img.style.cssText = '';

            // 添加点击事件监听
            img.addEventListener('click', (event) => {
                const allImageUrls = Array.from(images)
                    .map(getImageUrl)
                    .filter(url => url);

                const clickedImageUrl = getImageUrl(event.currentTarget);

                postToNativeBridge({
                    type: 'imageClick',
                    src: clickedImageUrl,
                    allImages: allImageUrls,
                    index: allImageUrls.indexOf(clickedImageUrl)
                });
            });
        });

        console.log(`[WebView Bridge] Initialized ${images.length} image click handlers`);
    }

    // DOM加载完成后初始化
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initImageClickHandlers);
    } else {
        initImageClickHandlers();
    }

    /**
     * 查找所有匹配的元素（递归遍历 DOM 树）
     * @param {string} text - 搜索文本
     * @param {Element} [dom] - 搜索根节点，默认 document.body
     * @returns {HTMLElement[]} - 匹配的元素数组
     */
    function findAllMatchingElements(text, dom) {
        const normalizedText = text.trim().replace(/\s+/g, ' ');
        const matchedElements = [];
        const visited = new Set(); // 使用 Set 防止重复添加（兼容性更好）

        /**
         * 递归搜索节点
         * @param {Node} node - 当前节点
         * @returns {HTMLElement[]|null} - 匹配的元素数组
         */
        function searchNodes(node) {
            // 文本节点：直接匹配内容
            if (node.nodeType === Node.TEXT_NODE) {
                if (node.nodeValue && node.nodeValue.includes(normalizedText) && node.parentElement) {
                    const parent = node.parentElement;
                    // 过滤隐藏元素
                    if (parent.offsetHeight > 0 && parent.offsetWidth > 0) {
                        return [parent];
                    }
                }
                return null;
            }

            // 元素节点：递归遍历子节点
            if (node.nodeType === Node.ELEMENT_NODE) {
                const foundChildren = [];

                // 遍历所有子节点
                node.childNodes.forEach(child => {
                    const result = searchNodes(child);
                    if (result && result.length > 0) {
                        foundChildren.push(...result);
                    }
                });

                // 如果在子节点中找到匹配，返回子节点结果
                if (foundChildren.length > 0) {
                    return foundChildren;
                }

                // 子节点未找到，检查当前元素的文本内容
                const textContent = node.textContent;
                const innerText = node instanceof HTMLElement ? node.innerText : null;

                if (textContent && textContent.includes(normalizedText)) {
                    return [node];
                } else if (innerText && innerText.includes(normalizedText)) {
                    return [node];
                }
            }

            return null;
        }

        // 执行搜索
        const foundElements = searchNodes(dom || document.body);

        // 去重并添加到结果数组
        if (foundElements) {
            foundElements.forEach(element => {
                if (element && !visited.has(element)) {
                    matchedElements.push(element);
                    visited.add(element);
                }
            });
        }

        return matchedElements;
    }

    /**
     * 查找匹配的元素（返回单个最佳匹配）
     * @param {string} anchorText - 锚点文本（已解码的中文或英文）
     * @returns {HTMLElement|null} - 匹配的元素
     */
    function findMatchingElement(anchorText) {
        const normalizedAnchor = anchorText.trim().replace(/\s+/g, ' ');

        // 1. 优先通过 ID 查找（精确匹配）
        const elementById = document.getElementById(anchorText);
        if (elementById) {
            console.log(`[WebView Bridge] 通过 ID 找到元素: ${elementById.tagName}`);
            return elementById;
        }

        // 2. 优先通过 name 属性查找
        const elementByName = document.querySelector(`[name="${CSS.escape(anchorText)}"]`);
        if (elementByName) {
            console.log(`[WebView Bridge] 通过 name 属性找到元素: ${elementByName.tagName}`);
            return elementByName;
        }

        // 3. 使用递归搜索查找所有匹配
        const matches = findAllMatchingElements(normalizedAnchor);

        if (matches.length === 0) {
            console.warn(`[WebView Bridge] 未找到匹配元素: ${anchorText}`);
            return null;
        }

        console.log(`[WebView Bridge] 找到 ${matches.length} 个匹配元素`);

        // 4. 优先返回标题元素（h1-h6）
        const heading = matches.find(el => /^H[1-6]$/.test(el.tagName));
        if (heading) {
            console.log(`[WebView Bridge] 优先返回标题元素: ${heading.tagName}`);
            return heading;
        }

        // 5. 返回第一个匹配的元素
        console.log(`[WebView Bridge] 返回第一个匹配元素: ${matches[0].tagName}`);
        return matches[0];
    }

    /**
     * 高亮显示目标元素（文本选中效果）
     * @param {HTMLElement} element - 目标元素
     * @param {number} duration - 高亮持续时间（毫秒），默认3000ms
     */
    function highlightElement(element, duration = 3000) {
        if (!element) {
            console.warn('[WebView Bridge] 目标元素不存在，无法高亮');
            return;
        }

        try {
            const selection = window.getSelection();
            const range = document.createRange();

            // 选择元素的全部内容
            range.selectNodeContents(element);

            // 清除之前的选择并应用新选择
            selection.removeAllRanges();
            selection.addRange(range);

            console.log(`[WebView Bridge] 已选中目标元素文本，${duration}ms 后自动取消`);

            // 指定时间后自动取消选择
            if (duration > 0) {
                setTimeout(() => {
                    if (window.getSelection) {
                        window.getSelection().removeAllRanges();
                        console.log('[WebView Bridge] 已取消文本选择');
                    }
                }, duration);
            }
        } catch (error) {
            console.warn('[WebView Bridge] 选中元素失败:', error);
        }
    }

    /**
     * 滚动到指定元素
     * @param {HTMLElement} element - 目标元素
     */
    function scrollToElement(element) {
        if (!element) {
            console.warn('[WebView Bridge] 目标元素不存在，无法滚动');
            return;
        }

        const platform = detectPlatform();

        // 获取元素在文档中的绝对位置
        const rect = element.getBoundingClientRect();
        const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
        const elementTop = rect.top + scrollTop;

        if (platform === 'android') {
            postToNativeBridge({
                type: 'scrollToPosition',
                position: Math.round(elementTop)
            });
        } else if (platform === 'ios') {
            element.scrollIntoView({
                behavior: 'smooth',
                block: 'center',
                inline: 'nearest'
            });
        }
    }

    /**
     * 根据锚点文本滚动到对应内容
     * @param {string} anchorText - 锚点文本
     * @returns {boolean} - 是否成功滚动
     */
    function scrollToAnchor(anchorText) {
        console.log(`[WebView Bridge] 开始查找锚点: ${anchorText}`);

        // URL 解码（处理中文等特殊字符）
        const decodedAnchor = decodeURIComponent(anchorText);

        const targetElement = findMatchingElement(decodedAnchor);
        if (targetElement) {
            scrollToElement(targetElement);
            highlightElement(targetElement, 3000);
            return true;
        } else {
            console.warn(`[WebView Bridge] 未找到匹配元素: ${anchorText}`);
            return false;
        }
    }

    // 暴露给全局作用域（供调试使用和 Native 调用）
    window.SlaxWebViewBridge = {
        postMessage: postToNativeBridge,
        getContentHeight: getContentHeight,
        scrollToAnchor: scrollToAnchor,
        highlightElement: highlightElement  // 新增：暴露高亮方法供外部调用
    };

    console.log('[WebView Bridge] Bridge initialized successfully');
})();

//# sourceURL=webview-bridge.js