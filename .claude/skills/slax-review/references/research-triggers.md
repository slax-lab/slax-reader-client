# Research Triggers — slax-review

**原则：能搜就搜。** 下面列出 slax-reader-client 项目当前栈的搜索模板，命中即触发。
**这个清单不是封顶 —— 看到任何不在表里的陌生 API、新语法、第三方调用，都要补搜。**

> ⚠️ **本文件不写死版本号。** 每次跑 skill 时先 `Read ./gradle/libs.versions.toml` 拿当前真实版本，再把版本号注入到下面的搜索 query 模板里。
> 例：toml 里写 `ktorClientOkhttp = "3.5.0"` → 搜索词用 `ktor 3.5.0 known issues OR pitfall`，不要再搜虚的 `ktor 3.x`。

---

## 网络层

### Ktor（OkHttp + Darwin engine）
- 触发：HttpClient 配置、拦截器、插件、序列化集成、各 engine 调用
- androidMain 用 `OkHttp` engine、iosMain 用 `Darwin` engine
- 搜索模板（`<v>` 替换为 toml 里读到的版本）：
  ```
  "ktor <v> <场景> known issues OR pitfall OR gotcha"
  "ktor <v> migration <场景>"
  "ktor <v> OkHttp engine bug site:github.com"
  "ktor <v> Darwin engine bug site:github.com"
  ```
- 必搜场景：
  - 重构 HttpClient 配置
  - 自定义 plugin / interceptor
  - SSE / WebSocket
  - 文件上传/下载

---

## 数据层

### PowerSync（小众，重点搜）
- 触发：schema 改动、watch 查询、write 操作、同步配置
- 搜索模板：
  ```
  "powersync kotlin <v> issue"
  "powersync sync conflict gotcha"
  "powersync schema migration kotlin"
  "powersync watch query performance"
  ```
- 必搜场景：
  - schema 定义变化（破坏性变更尤其要搜）
  - 大量 row 的 watch
  - 离线写入冲突处理
  - transaction 边界

### kotlinx-serialization
- 触发：`@Serializable` 注解、polymorphic、sealed、自定义 serializer、`@SerialName`
- 搜索模板：
  ```
  "kotlinx serialization <v> polymorphic <场景>"
  "@Serializable sealed class gotcha"
  "kotlinx serialization <v> bug site:github.com"
  ```
- 必搜场景：
  - 多态序列化
  - 自定义 `KSerializer`
  - 跨平台序列化（特别是 JS / Native）

### Datastore (commonMain)
- 触发：Preferences DataStore、Proto DataStore 调用
- 搜索模板：
  ```
  "androidx datastore <v> multiplatform issue"
  "datastore preferences <场景> gotcha"
  ```

---

## UI 层

### Compose Multiplatform
- 触发：任何 `@Composable` 函数改动
- 搜索模板：
  ```
  "compose multiplatform <v> issue"
  "compose multiplatform <api> bug site:github.com"
  "jetpack compose <api> pitfall"
  ```
- 必搜的 Compose API：
  - `remember` / `rememberSaveable` —— key 不完整、生命周期误用
  - `LaunchedEffect` —— key 选择、重组时取消重启
  - `derivedStateOf` —— 误用导致重组
  - `SideEffect` —— 跟 `LaunchedEffect` 的区别
  - `collectAsState` / `collectAsStateWithLifecycle` —— 初值、生命周期
  - `CompositionLocal` —— 提供位置、值变化导致大范围重组
  - `LazyColumn` / `LazyRow` —— item key、stable 类型、contentType
  - `animateContentSize` / 各种 animation —— 性能开销
  - `Modifier` 顺序 —— 顺序敏感的 Modifier 出错
  - `interop`（与平台原生 View / UIView 互通）

### Sketch（图片库）
- 触发：图片加载、缓存、占位符、变换
- 搜索模板：
  ```
  "sketch <v> image loader kotlin <场景>"
  "sketch <v> bug site:github.com"
  ```

### Markdown 渲染 / ksoup
- 触发：内容解析改动
- 搜索模板：
  ```
  "ksoup <v> parse <场景> issue"
  "compose markdown <v> <场景> bug"
  ```

---

## DI / 架构

### Koin
- 触发：模块定义、scope、`get()` / `inject()` / `getKoin()`
- 搜索模板：
  ```
  "koin <v> scope kotlin <场景> issue"
  "koin <v> multiplatform gotcha"
  ```
- 必搜场景：
  - 自定义 scope
  - factory vs single 误用
  - 跨平台模块定义

---

## 平台特定

### androidMain
- **Firebase Analytics / Crashlytics** —— 异常上报、event 上报
  - `"firebase analytics kotlin <v> issue"`
  - `"crashlytics non-fatal report best practice"`
- **Google Sign-In + Credentials API** —— Android 13+ Credential Manager
  - `"android credential manager <v> <场景> gotcha"`
  - `"google sign-in credential manager bug"`
- **Android Context / Lifecycle** —— Activity vs Application context、leak
- **OkHttp** —— interceptor、cache、超时

### iosMain
- **Darwin Ktor engine** —— iOS 特定网络行为
- **NSURLSession 配置**
- **GCD / Dispatch_queue** —— 跟 coroutine 的桥接
- **UIKit interop** —— Compose UIViewController 集成
  ```
  "compose multiplatform <v> UIViewController <场景>"
  "kotlin native swift interop <场景> gotcha"
  ```

### expect/actual
- 触发：任何 `expect` 声明改动
- 搜索模板：
  ```
  "kotlin multiplatform expect actual <场景> issue"
  "<api> ios actual implementation problem"
  ```

---

## Kotlin 语法糖 / 新特性（看到就搜）

| 语法 / 特性 | 搜索词 | 重点关注 |
|---|---|---|
| context receivers / parameters | `"kotlin context receivers gotcha"` | 解析顺序、迁移成 context parameters |
| value class（`@JvmInline`） | `"kotlin value class limitation"` | 数组、平台互操作 |
| `Result<T>` API | `"kotlin Result API pitfall"` | 协程里 `runCatching` 吞了 `CancellationException` |
| `@OptIn` | `"<被 opt-in 的 API> stability"` | 用了实验 API 要查清状态 |
| `buildList { }` / `buildMap { }` | `"buildList performance kotlin"` | 性能敏感场景的开销 |
| `sealed interface` | `"kotlin sealed interface gotcha"` | 跨模块实现限制 |
| `typealias` | （上下文判断） | 滥用导致语义模糊 |
| 委托属性 / `by` | `"kotlin property delegate <场景> issue"` | 自定义 delegate 的性能开销 |
| `inline` / `crossinline` / `noinline` | `"kotlin inline function gotcha"` | 错用导致字节码膨胀 |
| `suspend fun` + 异步选择 | `"kotlin coroutine <场景> structured concurrency"` | scope 选择、取消传播 |
| Flow 操作符 | `"kotlin Flow <operator> behavior"` | conflate / debounce / sample 行为差异 |

---

## 通用搜索查询模板

按这些模板组合搜，比直接搜「如何用 X」更能找到问题：

```
<target> <实际版本号> known issues OR pitfall OR gotcha
<target> <实际版本号> bug site:github.com
<target> kotlin multiplatform problem
<target> <实际版本号> changelog OR migration
<target> <实际版本号> performance regression
```

命中 GitHub Issue 后**必 WebFetch 进去**看：
- 复现条件（是否跟当前 PR 场景匹配）
- 影响版本（项目用的版本是否中招）
- 修复版本（应该升到哪个版本）
- workaround（如果还没修复）

---

## 调研态度

- 搜索 ≥ 5 次不算多，搜索 ≥ 10 次更稳
- 每个外部依赖的调用，**至少搜一轮**它的 known issues
- 看到陌生符号就搜，不要假装认识
- 搜索的查询词要具体（带项目当前版本号、带场景），不要太泛
- 版本号永远从 `gradle/libs.versions.toml` 实时取，本文件里**没有版本号** —— 这是有意设计，避免漂移
