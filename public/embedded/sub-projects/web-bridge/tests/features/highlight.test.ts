import { highlightElement } from '../../src/features/highlight';

describe('高亮功能 (Highlight Feature)', () => {
    let mockSelection: any;
    let mockRange: any;

    beforeEach(() => {
        mockRange = {
            selectNodeContents: jest.fn(),
        };
        document.createRange = jest.fn(() => mockRange);

        mockSelection = {
            removeAllRanges: jest.fn(),
            addRange: jest.fn(),
        };
        window.getSelection = jest.fn(() => mockSelection);
        
        jest.useFakeTimers();
        jest.spyOn(console, 'warn').mockImplementation(() => {});
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    test('应该高亮元素 (Legacy)', () => {
        const element = document.createElement('div');
        highlightElement(element);

        expect(document.createRange).toHaveBeenCalled();
        expect(mockRange.selectNodeContents).toHaveBeenCalledWith(element);
        expect(window.getSelection).toHaveBeenCalled();
        expect(mockSelection.removeAllRanges).toHaveBeenCalled();
        expect(mockSelection.addRange).toHaveBeenCalledWith(mockRange);
    });

    test('应该优先使用 Range 高亮', () => {
        const element = document.createElement('div');
        const customRange = { ...mockRange, custom: true } as any;
        
        highlightElement({ element, range: customRange });

        expect(document.createRange).not.toHaveBeenCalled(); // Should use provided range
        expect(mockSelection.addRange).toHaveBeenCalledWith(customRange);
    });

    test('如果没有 Range 但有 Element，应该创建 Range 高亮', () => {
        const element = document.createElement('div');
        
        highlightElement({ element, range: null });

        expect(document.createRange).toHaveBeenCalled();
        expect(mockRange.selectNodeContents).toHaveBeenCalledWith(element);
        expect(mockSelection.addRange).toHaveBeenCalledWith(mockRange);
    });

    test('如果元素和 Range 都为空应该发出警告', () => {
        highlightElement({ element: null, range: null });
        expect(console.warn).toHaveBeenCalledWith(expect.stringContaining('Target element/range does not exist'));
    });
});
