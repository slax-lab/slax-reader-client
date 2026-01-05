import { postToNativeBridge } from '../bridge/native-bridge';
import { getContentHeight } from '../features/content';
import { initImageClickHandlers } from '../features/images';
import { highlightElement } from '../features/highlight';
import { findMatchingElement } from '../features/search';
import { scrollToAnchor, scrollToElement } from '../features/scroll';
import { applyPolyfills } from '../utils/polyfill';

export class SlaxWebViewBridge {
    constructor() {
        this.init();
    }

    private init() {
        applyPolyfills();
        
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => {
                initImageClickHandlers();
            });
        } else {
            initImageClickHandlers();
        }

        console.log('[WebView Bridge] Bridge initialized successfully');
    }

    public postMessage = postToNativeBridge;
    public getContentHeight = getContentHeight;
    public scrollToAnchor = scrollToAnchor;
    public highlightElement = highlightElement;
    public findMatchingElement = findMatchingElement;
    public scrollToElement = scrollToElement;
}
