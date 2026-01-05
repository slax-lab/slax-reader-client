import { getContentHeight } from '../../src/features/content';

describe('内容功能 (Content Feature)', () => {
    test('应该正确计算最大高度', () => {
        // 模拟文档属性
        Object.defineProperty(document.body, 'scrollHeight', { value: 1000, configurable: true });
        Object.defineProperty(document.body, 'offsetHeight', { value: 900, configurable: true });
        Object.defineProperty(document.documentElement, 'clientHeight', { value: 800, configurable: true });
        Object.defineProperty(document.documentElement, 'scrollHeight', { value: 1200, configurable: true });
        Object.defineProperty(document.documentElement, 'offsetHeight', { value: 1100, configurable: true });

        const height = getContentHeight();
        expect(height).toBe(1200);
    });
});
