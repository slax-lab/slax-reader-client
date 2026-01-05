# 单元测试文档

本项目使用 [Jest](https://jestjs.io/) 作为测试框架，配合 [ts-jest](https://kulshekhar.github.io/ts-jest/) 直接运行 TypeScript 测试代码。测试环境配置为 `jsdom`，用于在 Node.js 环境中模拟浏览器 DOM API。

## 运行测试

```bash
# 运行所有测试
npm test

# 监听模式（开发时使用）
npm test -- --watch
```

## 目录结构

```
tests/
├── setup.ts                  # 全局测试配置和 Mock 初始化
├── bridge.test.ts            # 基础 Bridge 通信测试
├── slax-webview-bridge.test.ts # 主类初始化逻辑测试
└── features/                 # 各个功能模块的测试
    ├── content.test.ts       # 内容高度计算
    ├── highlight.test.ts     # 文本高亮
    ├── images.test.ts        # 图片点击处理
    ├── scroll.test.ts        # 滚动定位
    └── search.test.ts        # 搜索与模糊匹配
```

## 测试详情

### 1. 全局配置 (`setup.ts`)
- **Mock DOM API**: 模拟了 JSDOM 不支持或行为不一致的 API，如 `Element.prototype.scrollIntoView` 和 `window.scrollTo`。
- **Mock Native Bridge**: 在 `window` 对象上挂载了模拟的 `NativeBridge` (Android) 和 `webkit.messageHandlers.NativeBridge` (iOS) 对象，用于验证消息发送。
- **Console**: 拦截了 `console.warn` 和 `console.error` 以保持测试输出整洁，同时便于断言错误日志。

### 2. 基础通信 (`bridge.test.ts`)
- **目标**: 验证 `postToNativeBridge` 函数能否正确识别平台并发送消息。
- **覆盖场景**:
  - Android 平台消息发送 (`window.NativeBridge.postMessage`)。
  - iOS 平台消息发送 (`window.webkit.messageHandlers.NativeBridge.postMessage`)。
  - 未知平台或 Bridge 未注入时的错误处理。

### 3. 主类初始化 (`slax-webview-bridge.test.ts`)
- **目标**: 验证 `SlaxWebViewBridge` 类的实例化过程。
- **覆盖场景**:
  - 验证 Polyfill 和图片点击监听器是否被调用。
  - 验证核心方法 (`postMessage`, `getContentHeight` 等) 是否正确暴露。
  - **生命周期**: 验证当 `document.readyState` 为 `loading` 时，是否正确等待 `DOMContentLoaded` 事件触发后再初始化。

### 4. 功能模块测试 (`features/`)

#### 内容高度 (`content.test.ts`)
- **测试点**: `getContentHeight`
- **逻辑**: 验证是否能从 `body` 和 `documentElement` 的 `scrollHeight`, `offsetHeight`, `clientHeight` 中取最大值作为页面高度。

#### 文本高亮 (`highlight.test.ts`)
- **测试点**: `highlightElement`
- **逻辑**:
  - 验证是否调用 `window.getSelection` 和 `Range` API 选中指定元素。
  - 验证空元素输入的容错处理。
  - 验证定时器逻辑：高亮是否在指定时间后自动取消（使用 Jest Fake Timers）。

#### 图片处理 (`images.test.ts`)
- **测试点**: `initImageClickHandlers`
- **逻辑**:
  - 模拟 DOM 中的图片元素。
  - 触发 `click` 事件，验证是否向 Native 发送了正确的 `imageClick` 消息。
  - 验证消息载荷：包含当前图片 URL、所有图片 URL 列表以及当前索引。
  - 验证过滤逻辑：确保非 HTTP/HTTPS 图片（如 Base64）被正确忽略。

#### 滚动定位 (`scroll.test.ts`)
- **测试点**: `scrollToElement`, `scrollToAnchor`
- **逻辑**:
  - **Android**: 验证是否计算了元素位置百分比并通过 Bridge 发送 `scrollToPosition` 消息。
  - **iOS**: 验证是否调用了原生 `scrollIntoView` 方法。
  - **锚点查找**: 结合搜索模块，验证通过锚点文本查找元素并触发滚动的流程。

#### 搜索算法 (`search.test.ts`)
- **测试点**: `findMatchingElement`, `findAllMatchingElements`, `findBestFuzzyMatch`
- **逻辑**:
  - **精确查找**: 验证 ID 查找 > Name 属性查找 > 文本内容查找的优先级。
  - **遍历逻辑**: 验证递归 DOM 遍历能否正确找到包含特定文本的元素。
  - **可见性过滤**: 验证 `display: none` 或尺寸为 0 的隐藏元素是否被忽略。
  - **模糊匹配**: 验证当精确匹配失败时，是否能通过 Levenshtein 距离算法找到最接近的文本节点（容错匹配）。

## 编写新测试

如果您添加了新的 Feature，请遵循以下步骤：
1. 在 `tests/features/` 下创建对应的 `.test.ts` 文件。
2. 使用 `describe` 和 `test` 描述测试用例。
3. 如果需要模拟 Native 交互，请 spy `src/bridge/native-bridge.ts` 中的 `postToNativeBridge` 或直接检查 `window.NativeBridge` 的 mock 调用。
4. 运行 `npm test` 确保测试通过。
