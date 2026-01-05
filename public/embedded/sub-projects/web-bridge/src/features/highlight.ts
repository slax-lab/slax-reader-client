/**
 * 高亮目标元素
 */
export function highlightElement(target: { element?: HTMLElement | null, range?: Range | null } | HTMLElement | null) {
    let element: HTMLElement | null = null;
    let range: Range | null = null;

    if (target instanceof HTMLElement || target === null) {
        element = target;
    } else {
        element = target.element || null;
        range = target.range || null;
    }

    if (!element && !range) {
        console.warn('[WebView Bridge] Target element/range does not exist, cannot highlight');
        return;
    }

    try {
        const selection = window.getSelection();

        if (!range && element) {
            range = document.createRange();
            range.selectNodeContents(element);
        }
        
        if (selection && range) {
            selection.removeAllRanges();
            selection.addRange(range);
        }
    } catch (error) {
        console.warn('[WebView Bridge] Failed to select element:', error);
    }
}
