import { scrollToElement, scrollToAnchor } from '../../src/features/scroll';
import * as platformUtils from '../../src/utils/platform';
import * as nativeBridge from '../../src/bridge/native-bridge';
import * as search from '../../src/features/search';

describe('滚动功能 (Scroll Feature)', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        jest.spyOn(nativeBridge, 'postToNativeBridge');
    });

    describe('scrollToElement', () => {
        test('应该在 Android 上通过 bridge 滚动', () => {
            jest.spyOn(platformUtils, 'detectPlatform').mockReturnValue('android');
            
            const element = document.createElement('div');
            jest.spyOn(element, 'getBoundingClientRect').mockReturnValue({
                top: 100,
                height: 50,
                width: 100,
                left: 0,
                right: 100,
                bottom: 150,
                x: 0,
                y: 100,
                toJSON: () => {}
            });

            Object.defineProperty(document.body, 'scrollHeight', { value: 1000, configurable: true });

            scrollToElement(element);

            expect(nativeBridge.postToNativeBridge).toHaveBeenCalledWith(expect.objectContaining({
                type: 'scrollToPosition',
                percentage: expect.any(Number)
            }));
        });

        test('应该在 iOS 上通过 scrollIntoView 滚动', () => {
            jest.spyOn(platformUtils, 'detectPlatform').mockReturnValue('ios');
            
            const element = document.createElement('div');
            element.scrollIntoView = jest.fn();

            scrollToElement(element);

            expect(element.scrollIntoView).toHaveBeenCalledWith({
                behavior: 'smooth',
                block: 'center',
                inline: 'nearest'
            });
        });
    });

    describe('scrollToAnchor', () => {
        test('应该找到并滚动到元素', () => {
            const element = document.createElement('div');
            const range = document.createRange();
            jest.spyOn(search, 'findMatchingElement').mockReturnValue({ element, range });
            
            // 模拟 scrollToElement 行为（因为我们无法轻易 spy 同一模块内的导出函数）
            // 但我们可以检查是否触发了特定平台的逻辑。
            // 为了简单起见，假设是 iOS 来检查 scrollIntoView
            jest.spyOn(platformUtils, 'detectPlatform').mockReturnValue('ios');
            element.scrollIntoView = jest.fn();

            const result = scrollToAnchor('test-anchor');

            expect(search.findMatchingElement).toHaveBeenCalledWith('test-anchor');
            expect(element.scrollIntoView).toHaveBeenCalled();
            expect(result).toBe(true);
        });

        test('如果未找到则返回 false', () => {
            jest.spyOn(search, 'findMatchingElement').mockReturnValue(null);

            const result = scrollToAnchor('unknown-anchor');

            expect(result).toBe(false);
        });
    });
});
