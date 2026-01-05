import { detectPlatform } from '../utils/platform';

/**
 * 发送消息给 Native
 */
export function postToNativeBridge(payload: any): boolean {
    const message = JSON.stringify(payload);
    const platform = detectPlatform();

    // Android 平台
    if (platform === 'android') {
        window.NativeBridge!.postMessage(message);
        return true;
    }

    // iOS 平台
    if (platform === 'ios') {
        window.webkit!.messageHandlers!.NativeBridge!.postMessage(message);
        return true;
    }

    console.warn('Native bridge not available');
    return false;
}
