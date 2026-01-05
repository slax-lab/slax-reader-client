import { initImageClickHandlers } from '../../src/features/images';
import * as nativeBridge from '../../src/bridge/native-bridge';

describe('图片功能 (Images Feature)', () => {
    beforeEach(() => {
        document.body.innerHTML = '';
        jest.spyOn(nativeBridge, 'postToNativeBridge');
    });

    test('应该处理图片点击', () => {
        const img1 = document.createElement('img');
        img1.src = 'https://example.com/1.jpg';
        document.body.appendChild(img1);

        const img2 = document.createElement('img');
        img2.src = 'https://example.com/2.jpg';
        document.body.appendChild(img2);

        initImageClickHandlers();

        img2.click();

        expect(nativeBridge.postToNativeBridge).toHaveBeenCalledWith({
            type: 'imageClick',
            src: 'https://example.com/2.jpg',
            allImages: ['https://example.com/1.jpg', 'https://example.com/2.jpg'],
            index: 1
        });
    });

    test('应该忽略非 http 图片', () => {
        const img1 = document.createElement('img');
        img1.src = 'data:image/png;base64,...';
        document.body.appendChild(img1);

        const img2 = document.createElement('img');
        img2.src = 'https://example.com/valid.jpg';
        document.body.appendChild(img2);

        initImageClickHandlers();

        img2.click();

        expect(nativeBridge.postToNativeBridge).toHaveBeenCalledWith({
            type: 'imageClick',
            src: 'https://example.com/valid.jpg',
            allImages: ['https://example.com/valid.jpg'], // data uri 被忽略
            index: 0
        });
    });
});
