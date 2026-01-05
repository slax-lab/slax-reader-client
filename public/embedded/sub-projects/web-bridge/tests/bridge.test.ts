import { postToNativeBridge } from '../src/bridge/native-bridge';
import * as platformUtils from '../src/utils/platform';

describe('Native Bridge', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    test('应该向 Android NativeBridge 发送消息', () => {
        // 模拟平台为 Android
        jest.spyOn(platformUtils, 'detectPlatform').mockReturnValue('android');

        const payload = { type: 'test', data: 'hello' };
        const result = postToNativeBridge(payload);

        expect(result).toBe(true);
        expect(window.NativeBridge!.postMessage).toHaveBeenCalledWith(JSON.stringify(payload));
    });

    test('应该向 iOS NativeBridge 发送消息', () => {
        // 模拟平台为 iOS
        jest.spyOn(platformUtils, 'detectPlatform').mockReturnValue('ios');

        const payload = { type: 'test', data: 'hello' };
        const result = postToNativeBridge(payload);

        expect(result).toBe(true);
        expect(window.webkit!.messageHandlers!.NativeBridge!.postMessage).toHaveBeenCalledWith(JSON.stringify(payload));
    });

    test('应该处理未知平台', () => {
        // 模拟平台为未知
        jest.spyOn(platformUtils, 'detectPlatform').mockReturnValue('unknown');

        const payload = { type: 'test', data: 'hello' };
        const result = postToNativeBridge(payload);

        expect(result).toBe(false);
        expect(console.warn).toHaveBeenCalledWith('Native bridge not available');
    });
});
