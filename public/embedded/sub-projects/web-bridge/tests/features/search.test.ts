import { findMatchingElement } from '../../src/features/search';

describe('搜索功能 (Search Feature)', () => {
    beforeEach(() => {
        document.body.innerHTML = '';
    });

    describe('findMatchingElement', () => {
        test('应该通过文本内容查找', () => {
            document.body.innerHTML = `<p>Some unique text</p>`;
            const result = findMatchingElement('unique text');
            expect(result).not.toBeNull();
            expect(result!.element.tagName).toBe('P');
            expect(result!.range).not.toBeNull();
            expect(result!.range!.toString()).toContain('unique text');
        });
    });
});
