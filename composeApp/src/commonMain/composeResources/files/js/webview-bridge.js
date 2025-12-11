/**
 * WebView Bridge - Native与WebView通信桥接
 *
 * 功能：
 * 1. 提供统一的Native Bridge通信接口
 * 2. 处理图片点击事件并通知Native
 * 3. 支持Android和iOS平台
 *
 * @author Slax Reader
 */

(function() {
    'use strict';

    /**
     * 向Native发送消息
     * @param {Object} payload - 消息负载
     * @returns {boolean} - 是否成功发送
     */
    function postToNativeBridge(payload) {
        const message = JSON.stringify(payload);

        // Android平台
        if (window.NativeBridge?.postMessage) {
            window.NativeBridge.postMessage(message);
            return true;
        }

        // iOS平台
        if (window.webkit?.messageHandlers?.NativeBridge) {
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
     * 查找匹配的元素
     * @param {string} anchorText - 锚点文本（已解码的中文或英文）
     * @returns {HTMLElement|null} - 匹配的元素
     */
    function findMatchingElement(anchorText) {
        // 标准化锚点文本：去除多余空格，统一小写
        const normalizedAnchor = anchorText.trim().toLowerCase().replace(/\s+/g, ' ');

        // 1. 尝试通过 ID 查找（精确匹配）
        const elementById = document.getElementById(anchorText);
        if (elementById) {
            console.log(`[WebView Bridge] 通过 ID 找到元素: ${elementById.tagName}`);
            return elementById;
        }

        // 2. 遍历所有标题元素（h1-h6）
        const headings = document.querySelectorAll('h1, h2, h3, h4, h5, h6');
        for (const heading of headings) {
            const headingText = heading.textContent.trim().toLowerCase().replace(/\s+/g, ' ');

            // 精确匹配
            if (headingText === normalizedAnchor) {
                console.log(`[WebView Bridge] 通过标题精确匹配找到元素: ${heading.tagName}`);
                return heading;
            }

            // 模糊匹配：标题包含锚点文本
            if (headingText.includes(normalizedAnchor) || normalizedAnchor.includes(headingText)) {
                console.log(`[WebView Bridge] 通过标题模糊匹配找到元素: ${heading.tagName}`);
                return heading;
            }
        }

        // 3. 遍历所有段落和列表项（兜底策略）
        const allElements = document.querySelectorAll('p, li, div[class*="content"]');
        for (const element of allElements) {
            const elementText = element.textContent.trim().toLowerCase().replace(/\s+/g, ' ');
            if (elementText.includes(normalizedAnchor)) {
                console.log(`[WebView Bridge] 通过内容匹配找到元素: ${element.tagName}`);
                return element;
            }
        }

        return null;
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

        // 获取元素在文档中的绝对位置
        const rect = element.getBoundingClientRect();
        const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
        const elementTop = rect.top + scrollTop;

        // 通知 Native 滚动到指定位置（Android 需要）
        postToNativeBridge({
            type: 'scrollToPosition',
            position: Math.round(elementTop)
        });

        // WebView 内部也执行滚动（iOS 需要）
        element.scrollIntoView({
            behavior: 'smooth',
            block: 'center',
            inline: 'nearest'
        });

        console.log(`[WebView Bridge] 已滚动到元素: ${element.tagName}, 位置: ${Math.round(elementTop)}px`);
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