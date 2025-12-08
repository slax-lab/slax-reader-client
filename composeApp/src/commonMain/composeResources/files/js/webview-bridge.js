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

    // 暴露给全局作用域（供调试使用）
    window.SlaxWebViewBridge = {
        postMessage: postToNativeBridge,
        getContentHeight: getContentHeight
    };

    console.log('[WebView Bridge] Bridge initialized successfully');
})();

//# sourceURL=webview-bridge.js