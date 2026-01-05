// 模拟 scrollIntoView
Element.prototype.scrollIntoView = jest.fn();

// 模拟 window.scrollTo
window.scrollTo = jest.fn();

// 模拟 console 方法以保持测试输出整洁（可选，但便于检查调用）
global.console = {
    ...console,
    // log: jest.fn(), // 如果想屏蔽日志，取消注释
    warn: jest.fn(),
    error: jest.fn(),
};

// 模拟 Native Bridge 接口
Object.defineProperty(window, 'NativeBridge', {
    value: {
        postMessage: jest.fn(),
    },
    writable: true,
});

Object.defineProperty(window, 'webkit', {
    value: {
        messageHandlers: {
            NativeBridge: {
                postMessage: jest.fn(),
            },
        },
    },
    writable: true,
});
