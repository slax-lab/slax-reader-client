# Event Listener 诊断脚本

## ⚠️ 已知问题和修复

**问题**: 使用 `loadDataWithBaseURL` 时，`DOMContentLoaded` 事件在脚本注入之前触发，导致初始化失败。

**解决方案**: 已将初始化逻辑移至 `script.onload` 回调中。详见 [EVENT_LISTENER_FIX.md](EVENT_LISTENER_FIX.md)

---

## 诊断步骤

请在 Chrome DevTools Console 中运行以下脚本来诊断问题：

```javascript
// 完整的诊断脚本
function diagnoseBridge() {
    console.log('=== 开始诊断 ===\n');

    // 1. 检查 SlaxSelectionBridge 类是否存在
    console.log('1. SlaxSelectionBridge 类:', typeof SlaxSelectionBridge);

    // 2. 检查 bridge 实例是否存在
    console.log('2. selectionBridge 实例:', typeof selectionBridge);
    console.log('   实例详情:', selectionBridge);

    // 3. 检查容器元素
    const container = document.getElementById('article-content');
    console.log('3. 容器元素:', container);
    console.log('   容器存在:', !!container);

    if (container) {
        // 4. 获取所有事件监听器
        console.log('4. 容器元素的 Event Listeners:');

        // Chrome DevTools 专用方法
        if (typeof getEventListeners !== 'undefined') {
            const listeners = getEventListeners(container);
            console.log('   所有监听器:', listeners);
            console.log('   mouseup 监听器数量:', listeners.mouseup?.length || 0);
            console.log('   touchend 监听器数量:', listeners.touchend?.length || 0);
        } else {
            console.log('   (需要在 Chrome DevTools 中运行才能看到监听器详情)');
        }

        // 5. 手动触发监听器注册
        console.log('\n5. 尝试手动初始化桥接...');
        if (typeof SlaxSelectionBridge !== 'undefined') {
            try {
                // 重新创建实例
                window.testBridge = new SlaxSelectionBridge({
                    containerElement: container,
                    currentUserId: 1,
                    debug: true
                });

                window.testBridge.startMonitoring();
                console.log('   ✓ 测试桥接创建成功');

                // 再次检查监听器
                if (typeof getEventListeners !== 'undefined') {
                    const listeners = getEventListeners(container);
                    console.log('   新的 mouseup 监听器数量:', listeners.mouseup?.length || 0);
                    console.log('   新的 touchend 监听器数量:', listeners.touchend?.length || 0);
                }
            } catch (error) {
                console.error('   ✗ 测试桥接创建失败:', error);
            }
        }
    }

    // 6. 检查 SlaxBridge 适配器
    console.log('\n6. SlaxBridge 适配器:');
    console.log('   window.SlaxBridge:', window.SlaxBridge);
    console.log('   onTextSelected 方法:', typeof window.SlaxBridge?.onTextSelected);

    // 7. 检查原生桥接
    console.log('\n7. 原生桥接:');
    console.log('   window.NativeBridge:', typeof window.NativeBridge);
    console.log('   window.webkit:', typeof window.webkit);

    // 8. 测试手动选择
    console.log('\n8. 请尝试在页面上选择一些文本...');
    console.log('   然后运行: testSelection()');

    console.log('\n=== 诊断完成 ===');
}

// 测试选择功能
function testSelection() {
    const selection = window.getSelection();
    console.log('当前选择:');
    console.log('  选择的文本:', selection?.toString());
    console.log('  Range 数量:', selection?.rangeCount);

    if (selection && selection.rangeCount > 0) {
        const range = selection.getRangeAt(0);
        console.log('  Range 详情:', range);
        console.log('  Range collapsed:', range.collapsed);

        // 手动触发回调测试
        if (window.testBridge) {
            console.log('  尝试手动触发选择回调...');
        }
    }
}

// 手动测试事件监听
function testEventListener() {
    const container = document.getElementById('article-content');
    if (!container) {
        console.error('容器不存在');
        return;
    }

    console.log('添加测试监听器...');
    container.addEventListener('mouseup', function testListener(e) {
        console.log('测试监听器触发!', e);
    });

    console.log('请点击容器内的任意位置');

    if (typeof getEventListeners !== 'undefined') {
        const listeners = getEventListeners(container);
        console.log('更新后的监听器:', listeners);
    }
}

// 运行诊断
diagnoseBridge();
```

## 可能的原因和解决方案

### 原因1: 初始化时机问题

如果 `DOMContentLoaded` 在脚本注入之前就已经触发了，那么自动初始化不会执行。

**测试方法**:
```javascript
// 检查 DOM 加载状态
console.log('Document readyState:', document.readyState);

// 手动初始化
window.initializeBridge(1);

// 检查是否成功
console.log('Bridge initialized:', selectionBridge);
```

### 原因2: SlaxSelectionBridge 类未加载

**测试方法**:
```javascript
console.log('SlaxSelectionBridge:', typeof SlaxSelectionBridge);

// 如果是 undefined，检查脚本是否加载
console.log('Scripts:', Array.from(document.scripts).map(s => s.src));
```

### 原因3: 容器元素不存在或错误

**测试方法**:
```javascript
const container = document.getElementById('article-content');
console.log('Container:', container);
console.log('Container children:', container?.children.length);
```

### 原因4: Event Listener 被移除或覆盖

有时候其他代码可能会移除事件监听器。

**测试方法**:
```javascript
// 强制重新启动监听
if (selectionBridge) {
    selectionBridge.stop();
    selectionBridge.startMonitoring();
    console.log('重新启动监听器');
}
```

## 快速修复步骤

如果诊断发现 `selectionBridge` 是 `null` 或 `undefined`，请运行：

```javascript
// 强制初始化
if (typeof SlaxSelectionBridge !== 'undefined') {
    const container = document.getElementById('article-content');
    if (container) {
        window.manualBridge = new SlaxSelectionBridge({
            containerElement: container,
            currentUserId: 1,
            debug: true
        });
        window.manualBridge.startMonitoring();
        console.log('手动桥接已创建和启动');

        // 测试选择
        console.log('现在尝试选择文本，看是否有回调');
    }
}
```
