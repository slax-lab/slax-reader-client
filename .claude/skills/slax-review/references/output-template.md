# Output Template — slax-review

## 📂 文件路径

```
./.claude/review/review-<branch>-<YYYYMMDD-HHMMSS>.md
```

- `<branch>` = `git branch --show-current`（slash 替换成 `-`）
- 时间戳 = `date +%Y%m%d-%H%M%S`

---

## 📝 报告骨架

```markdown
# PR Review — <branch>

| 字段 | 值 |
|---|---|
| Base | `<merge-base sha>` |
| Head | `<head sha>` |
| Branch | `<branch>` |
| Changed files | N |
| Total diff | +X / -Y |
| Risk level | 🔴 高 / 🟡 中 / 🟢 低 |
| 生成时间 | YYYY-MM-DD HH:MM:SS |
| Reviewer | Claude Code (slax-review skill) |

---

## 🎯 TL;DR

一两句话总结整个 PR 的核心改动 + review 整体判断。

例：「该 PR 重构了书签同步逻辑，将 Ktor 调用从 callback 迁移到 Flow。整体方向正确，但发现 2 处协程作用域可能泄漏、1 处 PowerSync watch 缺 LIMIT。建议合并前修复 🔴 项。」

---

## 📌 上下文摘要（来自阶段 0）

### 需求描述（阶段 0.1）
- **来源**：gh pr view / commit messages / branch 名 / 用户对话
- **核心需求**：……（一段话描述本 PR 要解决的问题、预期行为）
- **验收点**：……（如果有，列出 PR 应满足的可验证条件）

### 项目规范（阶段 0.2）
- **找到的规范文件**：`CLAUDE.md` / `AGENTS.md` / `docs/code-style.md` / ……
- **本次 review 引用的具体条款**：「按 AGENTS.md 第 X 条……」
- 或：「项目原无规范文件，本次跑 skill 时已生成 `docs/code-style.md`，待用户审阅 commit」
- 或：「项目原无规范文件，用户选择按通用标准 review」

### 跨端范围（阶段 0.3）
- **改动覆盖的 source set**：commonMain / androidMain / iosMain / 构建配置 / ……
- **跨端范围**：单端 / 跨端（commonMain + 一端）/ 完整跨端
- **API contract 联动**：是否涉及后端 / 是否拿到了后端 PR / 是否需要联动 review
- **关联仓库**：是否需要 web 端 / admin 后台 / 共享 SDK 一起看

---

## 🔴 关键发现（高优先级）

### [🔴 高] <简短标题>
- **位置**: `composeApp/src/commonMain/kotlin/.../File.kt:行号`
- **问题**: 详细描述
- **来源**: 肉眼审查 / 调研发现 / sub-agent X / Codex 一致 / Codex 独立发现
- **调研佐证**: [issue#xxxx](https://...) [docs](https://...) （如适用）
- **建议**: 具体改法（最好附小段示例代码）
- **影响范围**: 哪些场景会出问题

（每个 🔴 项一段。如无，写「无 🔴 项」。）

---

## 🟡 中等问题

### [🟡 中] <标题>
- 同上格式，但严重度低一档
- 不一定阻塞 merge，但建议修

---

## 🟢 建议 / 低优先级

### [🟢 低] <标题>
- 风格、可读性、可选优化
- 不阻塞 merge

---

## ✅ 通过项（已审过、无问题）

简短列出已审过且 OK 的部分，证明覆盖度。例：
- `data/repository/BookmarkRepository.kt` 新增的 `syncAll` 函数：协程作用域 / 错误处理 / 复用度 均 OK
- `ui/inbox/InboxScreen.kt` 的 LazyColumn key 用了 stable id ✅
- iosMain 的 actual 实现跟 androidMain 行为一致

---

## 🔬 调研记录（透明度）

把阶段 2 所有 WebSearch 都列出来，证明搜过了：

| # | 研究点 | 查询词 | 命中链接 | 结论 |
|---|---|---|---|---|
| 1 | Ktor 3.5 OkHttp engine | `ktor 3 OkHttp known issues 2026` | [issue#xxxx](url) | 与本 PR 无关 |
| 2 | PowerSync watch 性能 | `powersync watch query performance` | [discussion#yyy](url) | 命中：大 row 需 LIMIT，已纳入🔴1 |
| 3 | `runCatching` + 协程 | `kotlin runCatching coroutine cancellation` | [Kotlin docs](url) | 命中：会吞 CancellationException，已纳入🟡2 |
| ... | | | | |

如果某次搜索没命中有效信息，也要列出来（用「无相关问题」），不要省略。

---

## 🤖 Sub-Agent 报告摘要

按阶段 4 (A) 实际启动的 sub-agent 各写一节；没启动的维度直接省略。

### 最小化扫描 (Explore agent)
- 对照阶段 0.1 拿到的需求描述，判断本 PR 实现是否最小化
- 如发现过度工程化：列出哪些抽象/文件/接口超出需求范围、建议如何瘦身、估算可删减的代码量
- 如确认最小化：明确写「实现紧贴需求，无 YAGNI 违反」
- 末尾标注本扫描已升级为哪些 🔴/🟡/🟢（写编号）

### 复用扫描 (Explore agent)
- 列出 diff 中新增的函数/类，与既有 `utils/` / `extension/` / `domain/` 实现高度相似的点
- 每条给出"建议合并到 `<path>:<line>`"或"另起合理，理由是 ……"的判断
- 给出整体重复率粗估（百分比即可）
- 末尾标注本扫描已升级为哪些 🔴/🟡/🟢（写编号）

### 边界扫描 (Explore agent)
- 按风险类别归纳（null 解引用 / 空集合 / 协程取消 / `when` 缺 else / `try` 吞异常 / 可空链滥用 / 数值溢出 / 并发竞争 等）
- 每类先报命中条目数，再给出最严重的 1–2 条作为代表，必须带 `file:line`
- 用一句话总结整体风险密度（如「主路径多、边角少」/「集中在 X 模块」）
- 末尾标注本扫描已升级为哪些 🔴/🟡/🟢（写编号）

### 平台对照 (Explore agent)
- 列出本 PR 命中的 expect/actual 行为差异 / 单端独有逻辑 / 单位或语义对齐脆弱点
- 每条判断属于：预期差异（可接受） / 需修正 / 待开发者确认
- 单端改动但应跨端的，必须明确点出"另一端没实现"
- 末尾标注本扫描已升级为哪些 🔴/🟡/🟢（写编号）

### Compose 风险 (Explore agent)
- 按 API 分类列出误用：`remember` 缺 key / `LaunchedEffect` key 不完整 / `mutableStateOf` 持有位置 / `collectAsState` 缺初值 / `derivedStateOf` 该用没用 / 重组热点
- 每条说明可能造成的运行时表现（漏重组 / 多余重组 / 状态丢失 / 内存泄漏）
- 末尾标注本扫描已升级为哪些 🔴/🟡/🟢（写编号）

### 数据层风险 (Explore agent)
- 分维度列出：PowerSync 同步语义 / Ktor 客户端配置 / kotlinx-serialization schema 兼容性 / 数据库迁移
- 每条判断属于：向后兼容 / 破坏性 / 需迁移脚本
- 涉及 API contract 的，呼应阶段 0.3 的跨仓联动结论
- 末尾标注本扫描已升级为哪些 🔴/🟡/🟢（写编号）

---

## 🔄 Codex /review 意见

来源：`/tmp/slax-review-codex.txt`

### Codex 的关键发现
1. （摘抄关键发现 1）
2. （摘抄关键发现 2）
...

### 与 Codex 的对照

| Codex 发现 | 我的判断 | 是否纳入报告 |
|---|---|---|
| 建议把 BookmarkRepository 拆分 | 同意，但 scope 超出本 PR 范围 | 列入「未来改进」 |
| 担心 ktor timeout 没设置 | 已通过对话确认全局配置够用 | 否 |
| 没发现 BookmarkCard 的 LaunchedEffect 问题 | Codex 漏了，我们的 sub-agent 抓到了 | 是（来源标 sub-agent） |

### 与 Codex 的分歧
- 我认为 X，Codex 认为 Y —— 倾向 X，因为 ……
- 如果无分歧，写「与 Codex 判断完全一致」

---

## 💬 用户对话记录

按阶段 3 的多轮对话顺序记录：

### Round 1
- **Q**: 这块改用 Flow 而非 suspend fun，是要持续观察还是单次？
  - 选项: 持续观察 / 单次返回 / 两者都需要
- **A**: 持续观察
- **影响**: 排除了「应该改回 suspend」的发现

### Round 2
- **Q**: `runCatching` 包住 suspend 是有意的吗（会吞 CancellationException）？
  - 选项: 无意要修 / 有意 / 不知道这个坑
- **A**: 不知道这个坑
- **影响**: 已在 🟡2 中详细解释 + 给修法

（如果一轮对话都没问，写「本次 PR 无需澄清，未发起对话」并解释为什么 —— 不要默默跳过。）

---

## 📌 未来改进（出本 PR 范围）

不影响本 PR merge 但值得后续做的：
- Repository 拆分（Codex 建议，需要单独立项）
- 给 commonMain 加 detekt 静态分析
- ...

---

## 📊 Review 元数据

| 指标 | 实际值 |
|---|---|
| 阶段 2 搜索次数 | N |
| 阶段 3 对话轮数 | N |
| 阶段 4(A) 启动 sub-agent 数 | N |
| 阶段 4(B) Codex 是否执行 | 是 / 否（如否，原因） |
| 总耗时（估算） | X 分钟 |

---

## 🛠️ 怎么把这份 review 同步到 PR

由于本机未装 `gh`，请手动操作：
1. 复制本文件「TL;DR + 🔴 + 🟡 + 🟢」部分
2. 粘贴到 GitHub PR 的 Conversation tab
3. 完整调研记录留作本地审计

或者如果安装了 `gh`：
```bash
gh pr review <PR号> --body-file <本文件> -c
```
```

---

## 📐 写作要点

- **位置必须精确到 file:line**，不要写「在某个文件里」
- **建议必须给具体改法**，不要写「应该改进」
- **来源必须标清**，让人能追溯是肉眼/搜索/agent/Codex 哪条线发现的
- **没发现就明说**（"无 🔴 项"），不要省略章节
- **调研记录要全**，搜过的都列上，证明覆盖度

---

## ❌ 反模式

- ❌ 报告里只有结论，没有调研记录（不透明）
- ❌ 把 Codex 输出原文照搬（要做交叉验证）
- ❌ 章节为空就删掉（应该明确写"无"，让人知道审过了）
- ❌ 没有「未来改进」一节（review 应该看到当下 + 看到未来）
