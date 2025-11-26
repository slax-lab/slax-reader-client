# 选择桥接调试指南

## 问题：选择文本时没有显示菜单

### 调试步骤

#### 1. 启用 WebView 调试

在 Chrome 浏览器中打开 `chrome://inspect`，你应该能看到你的应用的 WebView。

#### 2. 检查 JavaScript 是否加载

在 Chrome DevTools Console 中输入：
```javascript
typeof SlaxSelectionBridge
```

**预期结果**: 应该返回 `"function"`

**如果返回 `"undefined"`**: JavaScript 文件没有加载成功

#### 3. 检查桥接是否初始化

```javascript
typeof selectionBridge
```

**预期结果**: 应该返回 `"object"`

**如果返回 `"undefined"`**: 桥接没有初始化

#### 4. 检查容器元素

```javascript
document.getElementById('article-content')
```

**预期结果**: 应该返回一个 HTMLElement

#### 5. 手动测试选择

在 WebView 中选择一些文本，然后在 Console 中检查：

```javascript
// 检查是否有选择
window.getSelection().toString()
```

#### 6. 检查 SlaxBridge 适配器

```javascript
window.SlaxBridge
```

**预期结果**: 应该返回一个对象，包含 `onTextSelected`, `onMarkClicked` 等方法

#### 7. 查看所有日志

在 Console 中筛选 `[Bridge]` 或 `[SlaxBridge]` 标签的日志。

### 常见问题和解决方案

#### 问题1: JavaScript 文件加载失败

**症状**: Console 显示 404 错误或 `SlaxSelectionBridge is not defined`

**解决方案**:
1. 检查文件是否在正确的位置：`composeApp/src/androidMain/assets/js/slax-selection-bridge.js`
2. 清理并重新构建项目：`./gradlew clean && ./gradlew build`
3. 卸载并重新安装 APK

#### 问题2: 桥接初始化失败

**症状**: Console 显示 `[Bridge] SlaxSelectionBridge not loaded`

**解决方案**:
在 INJECTED_SCRIPT 注入后等待足够的时间，然后再加载 selection bridge。

#### 问题3: 选择事件不触发

**症状**: 能选择文本，但没有回调

**解决方案**:
检查 `startMonitoring()` 是否被调用：

```javascript
// 手动启动监听
if (selectionBridge) {
    selectionBridge.startMonitoring();
}
```

#### 问题4: 消息没有发送到原生端

**症状**: Console 显示选择事件，但 Android 没有收到消息

**解决方案**:
检查 `window.SlaxBridge` 是否存在：

```javascript
console.log('SlaxBridge:', window.SlaxBridge);
console.log('onTextSelected:', window.SlaxBridge?.onTextSelected);
```

### 快速测试脚本

在 Chrome DevTools Console 中运行以下脚本进行全面检查：

```javascript
// 完整诊断脚本
function diagnose() {
    const results = {
        SlaxSelectionBridge: typeof SlaxSelectionBridge,
        selectionBridge: typeof selectionBridge,
        SlaxBridgeAdapter: typeof window.SlaxBridge,
        container: !!document.getElementById('article-content'),
        NativeBridge: typeof window.NativeBridge,
        webkitMessageHandlers: !!window.webkit?.messageHandlers?.NativeBridge
    };

    console.log('=== 诊断结果 ===');
    console.log(JSON.stringify(results, null, 2));

    if (selectionBridge) {
        console.log('桥接实例:', selectionBridge);
    }

    return results;
}

diagnose();
```

### 手动测试选择功能

```javascript
// 手动触发选择回调测试
if (window.SlaxBridge && window.SlaxBridge.onTextSelected) {
    window.SlaxBridge.onTextSelected(
        JSON.stringify({
            paths: [{type: 'text', path: '/html/body/div/p[0]', start: 0, end: 10}],
            selection: [{type: 'text', text: '测试文本', startOffset: 0, endOffset: 10}]
        }),
        JSON.stringify({x: 100, y: 200, width: 100, height: 20})
    );
    console.log('手动触发选择事件');
}
```

### 预期的正常日志

当一切正常工作时，你应该在 Console 中看到类似这样的日志：

```
[Bridge] Page loaded, auto-initializing with userId=1
[SlaxBridge][info] SlaxSelectionBridge initialized
[Bridge] Initialized successfully
[WebView] Selection bridge initialized
[SlaxBridge][debug] Start monitoring selection
[用户选择文本后]
[SlaxBridge][debug] Selection detected: 1 items
[WebView] textSelected message sent
```

### 在 Android Studio 中查看日志

使用 Logcat 筛选器：

```
tag:WebView OR tag:SlaxBridge OR tag:Selection
```

应该看到：
- `[WebView] Selection bridge initialized`
- `[Selection] Highlight clicked` (点击划线按钮时)
- `[Comment] Comment submitted: xxx` (提交评论时)
