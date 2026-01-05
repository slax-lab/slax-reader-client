import { SlaxWebViewBridge } from '../src/core/slax-webview-bridge';
import * as polyfill from '../src/utils/polyfill';
import * as images from '../src/features/images';

describe('SlaxWebViewBridge', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        jest.spyOn(polyfill, 'applyPolyfills');
        jest.spyOn(images, 'initImageClickHandlers');
    });

    test('应该正确初始化', () => {
        const bridge = new SlaxWebViewBridge();

        expect(polyfill.applyPolyfills).toHaveBeenCalled();
        expect(images.initImageClickHandlers).toHaveBeenCalled();
        
        expect(bridge.postMessage).toBeDefined();
        expect(bridge.getContentHeight).toBeDefined();
        expect(bridge.scrollToAnchor).toBeDefined();
        expect(bridge.highlightElement).toBeDefined();
    });

    test('如果文档正在加载，应该等待 DOMContentLoaded', () => {
        const readyStateSpy = jest.spyOn(document, 'readyState', 'get').mockReturnValue('loading');

        new SlaxWebViewBridge();

        // 不应立即调用
        expect(images.initImageClickHandlers).not.toHaveBeenCalled();

        // 触发 DOMContentLoaded
        document.dispatchEvent(new Event('DOMContentLoaded'));

        expect(images.initImageClickHandlers).toHaveBeenCalled();
        
        readyStateSpy.mockRestore();
    });
});
