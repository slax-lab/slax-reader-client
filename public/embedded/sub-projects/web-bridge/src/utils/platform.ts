export type Platform = 'android' | 'ios' | 'unknown';

declare global {
    interface Window {
        CSS?: {
            escape: (value: string) => string;
        };
        NativeBridge?: {
            postMessage: (message: string) => void;
        };
        webkit?: {
            messageHandlers?: {
                NativeBridge?: {
                    postMessage: (message: any) => void;
                };
            };
        };
    }
}

export function detectPlatform(): Platform {
    // Android 平台：检查 NativeBridge.postMessage 是否存在
    if (window.NativeBridge?.postMessage) {
        return 'android';
    }

    // iOS 平台：检查 webkit.messageHandlers.NativeBridge 是否存在
    if (window.webkit?.messageHandlers?.NativeBridge) {
        return 'ios';
    }

    return 'unknown';
}
