(function() {
    const logArea = document.getElementById('log-area');

    function log(message, type = 'info') {
        if (!logArea) return;
        const entry = document.createElement('div');
        entry.className = `log-entry log-${type}`;
        entry.textContent = `[${new Date().toLocaleTimeString()}] ${message}`;
        logArea.appendChild(entry);
        logArea.scrollTop = logArea.scrollHeight;
    }

    window.logToConsole = log;

    function handleNativeMessage(message, platform) {
        log(`${platform} NativeBridge 收到消息: ${message}`, 'success');
        
        try {
            const payload = JSON.parse(message);
            if (payload.type === 'scrollToPosition') {
                const percentage = payload.percentage;
                log(`[Mock Native] 执行滚动到位置: ${(percentage * 100).toFixed(2)}%`, 'info');
                
                // Simulate native scrolling behavior
                const docHeight = Math.max(
                    document.body.scrollHeight,
                    document.documentElement.scrollHeight
                );
                const targetTop = docHeight * percentage;
                
                window.scrollTo({
                    top: targetTop,
                    behavior: 'smooth'
                });
            }
        } catch (e) {
            console.error('Error parsing native message:', e);
        }
    }

    // 模拟 Android 接口
    window.NativeBridge = {
        postMessage: function(message) {
            handleNativeMessage(message, 'Android');
        }
    };

    // 模拟 iOS 接口
    window.webkit = {
        messageHandlers: {
            NativeBridge: {
                postMessage: function(message) {
                    handleNativeMessage(message, 'iOS');
                }
            }
        }
    };

    log('Native Bridge Mock 初始化完成。支持 Android (window.NativeBridge) 和 iOS (window.webkit.messageHandlers.NativeBridge)。');
})();
