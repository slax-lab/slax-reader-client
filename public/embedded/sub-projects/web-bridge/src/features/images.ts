import { postToNativeBridge } from '../bridge/native-bridge';

/**
 * 获取图片元素的 URL
 */
function getImageUrl(element: Element): string {
    const tagName = element.tagName.toLowerCase();

    if (tagName === 'img') {
        const img = element as HTMLImageElement;
        return img.currentSrc || img.src || '';
    }

    if (tagName === 'image') {
        const svgImage = element as SVGImageElement;
        return svgImage.href?.baseVal ||
               element.getAttribute('href') ||
               element.getAttribute('xlink:href') ||
               '';
    }

    return '';
}

/**
 * 初始化图片点击处理程序
 */
export function initImageClickHandlers() {
    const images = document.querySelectorAll('img, image');

    images.forEach(img => {
        // 清除可能影响显示的样式
        (img as HTMLElement).style.cssText = '';

        // 添加点击事件监听器
        img.addEventListener('click', (event) => {
            const allImageUrls = Array.from(images)
                .map(getImageUrl)
                .filter(url => url && (url.startsWith("https://") || url.startsWith("http://")));

            const currentTarget = event.currentTarget as Element;
            const clickedImageUrl = getImageUrl(currentTarget);

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
