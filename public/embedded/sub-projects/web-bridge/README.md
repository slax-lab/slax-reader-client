# Slax Reader Web Bridge

`slax-reader-web-bridge` 是一个用于 Slax Reader 客户端应用集成的 Web 桥接库。它提供了 WebView 与 Native App 之间的通信机制，以及一系列用于操作和查询 Web 内容的功能。

## ✨ 功能特性

*   **Native 通信**: 提供统一的接口与 iOS 和 Android 原生层进行通信。
*   **内容管理**: 获取 Web 内容的高度，适配原生容器。
*   **图片处理**: 自动接管图片点击事件，通知原生层进行图片预览等操作。
*   **元素高亮**: 支持通过 ID 高亮指定的 DOM 元素。
*   **滚动控制**: 支持滚动到指定的锚点位置。
*   **搜索功能**: (内部功能) 支持在 DOM 树中查找匹配文本的元素。

## 📂 项目结构

```
slax-reader-web-bridge/
├── demo/                   # 演示页面，用于测试各项功能
├── src/
│   ├── bridge/            # Native 通信核心逻辑
│   │   └── native-bridge.ts
│   ├── core/              # 核心桥接类
│   │   └── slax-webview-bridge.ts
│   ├── features/          # 具体功能实现
│   │   ├── content.ts     # 内容高度计算
│   │   ├── highlight.ts   # 元素高亮
│   │   ├── images.ts      # 图片点击处理
│   │   ├── scroll.ts      # 滚动控制
│   │   └── search.ts      # 文本搜索
│   ├── types/             # 类型定义
│   ├── utils/             # 工具函数 (平台检测、Polyfill 等)
│   └── index.ts           # 入口文件
├── tests/                 # 单元测试
├── rollup.config.js       # Rollup 打包配置
└── package.json
```

## 🚀 快速开始

### 安装依赖

```bash
pnpm install
```

### 开发与构建

*   **启动开发模式 (监听文件变化)**:
    ```bash
    pnpm dev
    ```

*   **启动演示页面 (Demo)**:
    ```bash
    pnpm demo
    ```
    这将启动一个本地服务器，并在浏览器中打开演示页面，方便调试各项功能。

*   **构建生产版本**:
    ```bash
    pnpm build
    ```

*   **运行测试**:
    ```bash
    pnpm test
    ```

## 📖 使用指南

### 初始化

在 Web 页面中引入打包后的脚本，并实例化 `SlaxWebViewBridge`：

```typescript
import { SlaxWebViewBridge } from 'slax-reader-web-bridge';

const bridge = new SlaxWebViewBridge();
```

初始化时，Bridge 会自动：
1. 应用必要的 Polyfills。
2. 绑定图片点击事件监听器。

### API 参考

#### 1. 发送消息给 Native (postMessage)

```typescript
bridge.postMessage({
    type: 'some-action',
    payload: { /* data */ }
});
```
*   **Android**: 调用 `window.NativeBridge.postMessage`
*   **iOS**: 调用 `window.webkit.messageHandlers.NativeBridge.postMessage`

#### 2. 获取内容高度 (getContentHeight)

```typescript
const height = bridge.getContentHeight();
console.log('Content Height:', height);
```

#### 3. 滚动到锚点 (scrollToAnchor)

```typescript
// 滚动到 id 为 "section-1" 的元素
bridge.scrollToAnchor('section-1');
```

#### 4. 高亮元素 (highlightElement)

```typescript
// 高亮 id 为 "target-element" 的元素
bridge.highlightElement('target-element');
```

## 🔧 Native 适配说明

为了确保 Bridge 正常工作，Native 端需要注入相应的 JavaScript 接口：

*   **Android**: 需要在 WebView 中注入名为 `NativeBridge` 的对象。
*   **iOS**: 需要在 `WKWebView` 的 `userContentController` 中注册名为 `NativeBridge` 的 ScriptMessageHandler。

## 📄 License

MIT
