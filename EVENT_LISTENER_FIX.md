# Event Listener 问题修复说明

## 问题诊断

### 根本原因
当使用 `WebView.loadDataWithBaseURL()` 加载 HTML 内容时，事件顺序如下：

```
1. HTML 内容加载
2. DOMContentLoaded 事件触发 ❌ (此时 SlaxSelectionBridge 尚未加载)
3. onPageFinished 回调触发
4. INJECTED_SCRIPT 注入
5. LOAD_SELECTION_BRIDGE_SCRIPT 注入
6. slax-selection-bridge.js 加载完成 ✅ (但已错过初始化时机)
```

**问题**: HTML 中的 `DOMContentLoaded` 事件监听器在脚本注入之前就已经触发，导致：
- `SlaxSelectionBridge` 类尚未定义
- `window.initializeBridge()` 被调用但失败
- 事件监听器（mouseup/touchend）从未被注册到 `#article-content` 元素上

## 修复方案

### 修改 1: WebViewJS.kt

将初始化逻辑移到 `script.onload` 回调中：

```kotlin
// composeApp/src/commonMain/kotlin/com/slax/reader/const/WebViewJS.kt
const val LOAD_SELECTION_BRIDGE_SCRIPT_ANDROID: String = """
    (function() {
        var script = document.createElement('script');
        script.src = 'file:///android_asset/js/slax-selection-bridge.js';
        script.onload = function() {
            console.log('[SlaxBridge] Selection bridge loaded successfully');

            // 在脚本加载完成后初始化
            setTimeout(function() {
                if (typeof window.initializeBridge === 'function') {
                    console.log('[SlaxBridge] Calling initializeBridge...');
                    var success = window.initializeBridge(1);
                    if (success) {
                        console.log('[SlaxBridge] Bridge initialized successfully from script load');
                    } else {
                        console.error('[SlaxBridge] Bridge initialization failed');
                    }
                } else {
                    console.error('[SlaxBridge] initializeBridge function not found');
                }
            }, 100);
        };
        script.onerror = function() {
            console.error('[SlaxBridge] Failed to load selection bridge');
        };
        document.head.appendChild(script);
    })();
"""
```

### 修改 2: WebViewHelper.kt

移除 HTML 中的 `DOMContentLoaded` 自动初始化：

```kotlin
// composeApp/src/commonMain/kotlin/com/slax/reader/utils/WebViewHelper.kt
// 在 <script> 标签末尾，删除:
document.addEventListener('DOMContentLoaded', function() {
    console.log('[Bridge] Page loaded, auto-initializing with userId=1');
    setTimeout(function() {
        if (typeof SlaxSelectionBridge !== 'undefined') {
            window.initializeBridge(1);
        } else {
            console.error('[Bridge] SlaxSelectionBridge not available after page load');
        }
    }, 100);
});

// 替换为简单的注释:
// Note: Bridge is initialized from LOAD_SELECTION_BRIDGE_SCRIPT after the script loads
// Do not auto-initialize here as DOMContentLoaded may fire before scripts are injected
```

## 修复后的执行流程

```
1. HTML 内容加载
2. DOMContentLoaded 事件触发 (什么都不做)
3. onPageFinished 回调触发
4. INJECTED_SCRIPT 注入 (设置 window.SlaxBridge 适配器)
5. LOAD_SELECTION_BRIDGE_SCRIPT 注入 (动态加载 JS 文件)
6. slax-selection-bridge.js 加载完成
7. script.onload 回调触发 ✅
8. 调用 window.initializeBridge(1)
9. SlaxSelectionBridge 实例创建
10. selectionBridge.startMonitoring() 调用
11. mouseup/touchend 事件监听器注册到 #article-content ✅
```

## 验证修复

### 1. 重新编译并运行应用

```bash
./gradlew :composeApp:assembleDebug
```

### 2. 在 Chrome DevTools 中验证

打开 `chrome://inspect`，连接到 WebView，在 Console 中运行：

```javascript
// 应该看到这些日志（按顺序）:
// [SlaxBridge] Selection bridge loaded successfully
// [SlaxBridge] Calling initializeBridge...
// [Bridge] Initialized successfully
// [SlaxBridge] Bridge initialized successfully from script load

// 验证事件监听器
const container = document.getElementById('article-content');
const listeners = getEventListeners(container);
console.log('mouseup 监听器数量:', listeners.mouseup?.length || 0);
console.log('touchend 监听器数量:', listeners.touchend?.length || 0);

// 预期结果: 每个应该至少有 1 个监听器
```

### 3. 测试文本选择

1. 在文章内容中选择一些文本
2. 应该看到选择菜单出现（划线/评论按钮）
3. Console 应该显示:
   ```
   [SlaxBridge][debug] Selection detected: X items
   [WebView] textSelected message sent
   ```

## 关键要点

1. **不要依赖 DOMContentLoaded** 当脚本是通过 `evaluateJavascript` 在 `onPageFinished` 中注入时
2. **使用 script.onload 回调** 来确保脚本完全加载后再初始化
3. **添加短延迟 (100ms)** 给 DOM 渲染留出时间
4. **添加详细日志** 以便追踪初始化流程

## 后续优化建议

1. **考虑使用 `<script src="">` 标签直接在 HTML 中引用**
   - 这样可以让浏览器自然地加载脚本
   - 但需要确保 `file:///android_asset/` 路径在 `loadDataWithBaseURL` 中可访问

2. **添加初始化状态管理**
   - 在 Kotlin 端监听 `bridgeInitialized` 消息
   - 在初始化完成之前禁用 UI 交互

3. **错误恢复机制**
   - 如果初始化失败，提供重试按钮
   - 向用户显示友好的错误消息
