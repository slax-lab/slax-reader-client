# 测试范围与执行方式

## 测试分层

- `composeApp/src/commonTest`：`kotlin.test`、协程虚拟时间、Turbine。测试生产 ViewModel、Delegate、模型及工具方法；Fake 只作为外部依赖，不把 Fake 自测计入数据库覆盖。
- `composeApp/src/androidInstrumentedTest/.../database/PowerSyncDaoTest.kt`：生产 DAO + PowerSync 1.13.0 原生 SQLite 驱动 + 生产 `AppSchema`。每个用例创建独立文件，关闭后仅删除该测试文件；不连接服务器、不使用应用 `powersync.db`。
- `composeApp/src/androidInstrumentedTest/.../ui`：Compose 框架实际渲染、输入、点击、Semantics 断言。`DatabaseBackedInboxUiTest` 将生产 DAO、ViewModel 和 `ArticleList` 连起来。

## 执行

```sh
make test
make test-ui
```

`make test` 包含 Android Debug/Release 单测和配置的 Kotlin 测试目标；iOS Simulator 需要 macOS/Xcode。`make test-ui` 需要已启动的 Android 模拟器/真机，包含 UI 和数据库 instrumented 测试。

单独运行数据库测试：

```sh
./gradlew :composeApp:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.slax.reader.database.PowerSyncDaoTest
```

报告：

- `composeApp/build/reports/tests/testDebugUnitTest/index.html`
- `composeApp/build/reports/tests/testReleaseUnitTest/index.html`
- `composeApp/build/reports/tests/iosSimulatorArm64Test/index.html`
- `composeApp/build/reports/androidTests/connected/debug/index.html`

## 新增验证范围

| 层次 | 实际验证 |
| --- | --- |
| PowerSync / BookmarkDao | 离线创建、改名、收藏、归档、软删除；筛选 Flow；标签查找；JSON 元数据更新；CRUD 待上传队列 |
| LocalBookmarkDao | 失败/下载中/完成状态持久化；批量重置仅影响指定条目；其他缓存字段不丢失；本地表不进入上传队列 |
| 数据库恢复 | 关闭并重新打开同一个测试文件，读取缓存、阅读位置、概要、划线用户和待上传队列 |
| 数据库边界 | 事务失败回滚；缓存不存在、非法 JSON、非数字阅读位置 |
| 其他 DAO | 用户可空字段；订阅变化；评论新增/软删除和跨文章隔离 |
| 数据库 → UI | 离线列表新增/改名/删除；下载状态进入真实 Semantics 节点 |
| Feedback 页面 | 空白输入禁用；参数 trim；成功弹窗；失败保留内容、显式重试；提交中禁用 |
| Login 页面 | 协议拒绝不触发登录；协议页切换；同意后导航；SDK 失败/取消；认证中禁用及认证失败恢复 |
| Sidebar 菜单 | 设置/About 导航；登出回调；反馈携带邮箱；用户资料未加载时的反馈点击回归 |
| 方法层 | 离线概要/大纲缓存命中；离线未命中后显式在线加载；流错误不保存部分结果；滚动保存防抖、立即 flush、切换文章隔离 |

## 不要混淆的边界

- 在本地更新下载状态并不证明下载 worker 的 HTTP 请求、图片写盘、重试和并发调度已覆盖。
- 检查 PowerSync CRUD 队列不等于已验证断线重连上传；这些用例没有启动同步服务。
- 数据库 close/reopen 不等于操作系统杀进程后的完整 App 冷启动。
- 非法缓存字段不是损坏数据库文件；完整损坏恢复与历史 schema 迁移仍未覆盖。
- Login 测试替换了原生登录提供者与协议 WebView，不验证 Google/Apple SDK 或 WebView 内容。
- 设备端运行结果目前来自 Android 模拟器，不代表 Android/iOS 真机或 iOS Compose UI 已验证。
- 部分页面内部仍有 Koin/平台依赖，尚未证明全应用可不启动 Koin 运行。
- 测试数量不是行/分支覆盖率；未生成 Kover/JaCoCo 报告，不提供百分比。
- WebView 完整流程和 IAP 按用户要求不扩展。

遇到生产行为不一致或崩溃，保留失败回归及错误证据，先确认产品行为；不通过忽略测试、吞异常或降低断言来制造全绿。
