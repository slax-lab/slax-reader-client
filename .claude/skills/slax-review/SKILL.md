---
name: slax-review
description: |
  Deep multi-round PR code review for the slax-reader-client Kotlin Multiplatform Compose project.
  Runs visible-quality triage (performance, idiomaticity, elegance), researches third-party deps
  and Kotlin syntax sugar against known issues via parallel WebSearch + WebFetch, holds multi-turn
  AskUserQuestion dialogs to clarify intent, dispatches multiple Explore sub-agents in parallel,
  cross-checks with Codex `codex review` as a second opinion, and emits a structured report under
  .claude/plans/. Use this skill whenever the user asks for a deep / thorough / careful PR review
  on this KMP repo — e.g. "/slax-review", "深度 review", "细致 review", "审查 PR", "review this
  PR", "review my changes", "thorough code review", "deep PR review", "code review this branch",
  even when the user does not say "slax". Prefer this over a quick eyeball pass any time the
  request includes "review" + "PR/branch/diff/这个 PR/这条分支".
---

# slax-review — Deep KMP PR Review

你是为 **Kotlin Multiplatform Compose 项目 slax-reader-client** 做深度 PR 审查的资深工程师。这不是一次性扫描，而是一场**多轮调研 + 对话**的过程。

---

## 核心原则（每一步执行前都默念）

1. **能并发就并发**：任何能拆给子 agent 的任务，**一条消息里同时发起多个 Agent / WebSearch 调用**。串行是浪费。
2. **token 不是问题**：不要为了省 token 跳过步骤、缩减搜索次数、压缩思考。宁可多读多搜。
3. **搜索优先**：看到任何语法糖、任何第三方依赖、任何平台 API —— 先搜了再说。
4. **不确定就问**：意图不明、有多种实现可能、平台差异 —— 先用 `AskUserQuestion` 问开发者，再下结论。瞎猜不可接受。
5. **流程是参考不是清单**：下面 5 个阶段是建议顺序，根据 PR 实际情况可以并行、可以重排、可以跳过明显不适用的步骤，但不要跳过阶段 0。

---

## 主流程

### 阶段 0 — 上下文与规范确认（前置，必做）

**没有上下文 = 瞎审。** 这一步必须在任何审查动作之前完成。

#### 0.1 拿 PR 需求描述（review 的锚点）

按以下顺序尝试，能拿到一份就停：

1. `gh pr view --json title,body,number 2>/dev/null`（本机当前**没装** `gh`，会失败，正常 fallback）
2. `git log --format="%s%n%n%b" <base>..HEAD` —— 看 commit messages 的 body 部分
3. `git branch --show-current` —— 分支名常含 issue 号 / feature 关键词
4. 检查 `CHANGELOG.md` 是否有对应条目
5. 项目根目录的 `docs/`、`Q&A.md` 等可能包含需求记录

**全都拿不到** → 用 `AskUserQuestion` 问开发者：「这次改动的需求/目的是什么？解决什么问题？预期行为变化？」

**这是必问项，瞎猜不可接受。** 没有需求锚点，后面发现的"问题"可能恰恰是用户故意的设计。

#### 0.2 检查项目规范文件

依次查找（按优先级）：
- `CLAUDE.md` / `CLAUDE.local.md`
- `AGENTS.md`
- `CONTRIBUTING.md`
- `CODE_STYLE.md` / `docs/code-style.md` / `docs/STYLE.md`
- `.editorconfig`
- `docs/architecture.md` / `ARCHITECTURE.md`

**找到 ≥ 1 份** → `Read` 进来作为本次 review 的评判标尺。后续所有发现都要标注「按规范第 X 条」。

**全都没找到** → 用 `AskUserQuestion` 问开发者：

```
options:
1. 现在生成一份基于 KMP + Compose 行业最佳实践的规范 (Recommended)
2. 暂不生成，本次 review 按通用标准
3. 跳过，我自己另外写
```

如果选 1，skill 要：
- 并行 WebSearch 这些主题：「kotlin multiplatform best practices」「jetpack compose code style」「ktor coding conventions」「coroutine scope guidelines」
- 基于项目实际栈生成（栈以 `gradle/libs.versions.toml` 为准）
- 覆盖：命名约定、目录组织、异常处理、协程作用域、Compose 状态管理、expect/actual 写法、依赖注入边界
- 写到 `<project>/docs/code-style.md`（若已有 `AGENTS.md`，**追加**而非新建）
- 告诉用户文件已生成，请审阅自行 commit
- 落地后再进入阶段 1

> **slax-reader-client 现状**：项目根目录已有 `AGENTS.md` 与 `CLAUDE.local.md`。本项目跑该 skill 时 0.2 应直接落到「已有规范」分支：把这两份 `Read` 进来作为评判标尺，本节"生成规范"分支不会触发。其他 KMP 项目复用本 skill 时分支才会生效。

#### 0.3 跨端工作区与联动确认

1. 跑 `git diff --name-only <base>...HEAD` 统计改动覆盖的 source set
2. 标识跨端范围：
   - 只动 `commonMain` → 共享逻辑改动
   - 只动 `androidMain` 或 `iosMain` → 单端改动
   - 动了 `commonMain` + 任一平台 → 跨端改动（**重点关注 expect/actual 完整性**）
   - 全部都动 → 完整跨端改动
3. 如果改动涉及 API 调用层（Ktor 请求 path / serialization 模型 / 错误码处理），用 `AskUserQuestion` 问：
   - 「这次改动是否涉及后端 API contract 变更？」
   - 如果是 → 「后端 PR 在哪里？是否需要拉到工作区一起对照？」
4. 如果项目可能有关联 client repo（web 端、admin 后台、共享 SDK），询问是否需要联动审查
5. **跨端改动必须两端都审** —— 不要只看 androidMain 就出报告

完成 0.1–0.3 后才进入阶段 1。**绝不跳过阶段 0。**

---

### 阶段 1 — 肉眼可见的快速点评（性能 → 写法 → 优雅）

目标：靠模型直觉对 diff 做第一遍审查，**不查文档、不开子 agent**，先建立整体印象。

```bash
BASE="$(git merge-base HEAD main 2>/dev/null || git merge-base HEAD origin/main)"
git diff --stat "$BASE"...HEAD
git diff "$BASE"...HEAD
```

按以下优先级看 diff：

1. **性能**
   - 循环里的 IO / 重复分配 / 反射 / 字符串拼接
   - 协程作用域用错（`GlobalScope`、不必要的 Dispatchers 切换）
   - Compose 重组开销（无 key 的 LazyColumn item、composable 里创建 Flow）
   - PowerSync 查询频率、大查询不分页

2. **写法**
   - 命名是否清晰
   - API 选用是否恰当（该用 Flow 用了 suspend？该用 sealed class 用了 enum？）
   - 错误处理（`try` 吞异常、`Result` 误用、可空链滥用）
   - 可读性（嵌套层级、行长度、神秘数字）

3. **优雅**
   - 复用度（这块逻辑是不是应该是 extension function / utility）
   - 抽象层次（UI 层混了数据层逻辑？）
   - 代码组织符不符合项目 layer-first 风格（`ui/ data/ domain/ di/ extension/ utils/ const/`）

产出物：**第一印象笔记**，分三类：
- 直接能下结论的发现（性能慢、写法差、不优雅）
- 需要进一步调研的（用了陌生语法/依赖）→ 进阶段 2
- 意图不明、需要问开发者的 → 进阶段 3

---

### 阶段 2 — 识别语法糖与依赖，针对性调研

**先 Read 项目特定的搜索清单**：

```
./.claude/skills/slax-review/research-triggers.md
```

**再实时 Read 项目依赖版本**（清单文件里不再写死版本号，每次跑 skill 都要拿最新值）：

```
./gradle/libs.versions.toml
```

把读到的版本号注入到每一条搜索 query 里，例如读到 `ktorClientOkhttp = "3.5.0"` 就搜 `"ktor 3.5.0 OkHttp engine known issues"`，不要再搜虚的 `"ktor 3.x"`。

扫 diff 找以下目标：

- **Kotlin 新语法 / 语法糖**：context receivers/parameters、value class、`@OptIn`、`@Serializable` 注解、`Result<T>`、`buildList`/`buildMap`、typealias、委托属性、`sealed interface`、`@JvmInline`
- **第三方依赖调用**：Ktor / PowerSync / Sketch / Koin / kotlinx-serialization / Firebase / Compose Multiplatform / connectivity / markdown-renderer / ksoup
- **Compose 关键 API**：`remember`、`LaunchedEffect`、`derivedStateOf`、`SideEffect`、`collectAsState`、`rememberSaveable`、`CompositionLocal`
- **平台特定 API**：Android Context、Firebase、Credentials Manager、iOS UIKit、Darwin / GCD、`expect`/`actual`

**关键：并行发起 WebSearch**

把所有要搜的目标列出来，**一条消息里同时发起所有 WebSearch 调用**。每个目标至少搜：

```
"<target> <实际版本号> known issues OR pitfall OR gotcha 2026"
"<target> <实际版本号> bug site:github.com"
"<target> kotlin multiplatform problem"   (KMP 相关时)
```

命中关键讨论（特别是 GitHub Issue）→ **立刻 WebFetch 进去**读：复现条件、影响版本、修复版本、workaround。

记录每个搜索：查询词 / 命中链接 / 结论。这些会进入最终报告的「调研记录」一节。

---

### 阶段 3 — 多轮对话澄清需求

**先 Read 典型问题模板**：

```
./.claude/skills/slax-review/question-patterns.md
```

把阶段 1 留下的「意图不明」+ 阶段 2 调研出的「行为存疑」点汇总。

**用 `AskUserQuestion` 工具问开发者**，遵循：

- **分轮问**：每轮 2–3 个最关键的问题，等回答完再问下一轮。不要一次甩 10 个问题给用户。
- **给选项**：用 `AskUserQuestion` 的 options 数组（2-4 个），不要开放式提问。
- **附 Recommended**：自己判断更可能的选项放第一个，label 末尾标 `(Recommended)`。
- **优先级**：影响结论的问题优先（不问就给不出准确 review）；次要疑惑可以归到「报告里附带」。

不要害怕来回问。一次深度 review 至少应该有 **1 轮**对话（除非 PR 极简单到 5 行内）。

---

### 阶段 4 — 并发深度扫描 + Codex /review

**这一步是整个 skill 的核心。两件事必须在同一条消息里并行启动：**

#### (A) 多个 Sub-Agent 并发扫描

根据 PR 实际改动范围，**用一条消息发起多个 Agent 调用**（`subagent_type` 选 `Explore` 或 `general-purpose`）。下面是维度模板，按 PR 命中情况选发：

| Agent 任务 | 何时发 | Prompt 要点 |
|---|---|---|
| **最小化扫描** | 阶段 0.1 拿到需求描述后总是发 | 对照需求描述，判断 PR 实现是否最小化 —— 有没有过度工程化、有没有 YAGNI 违反、有没有更简洁的实现路径、新增文件/抽象层是否合理。**prompt 里必须附上阶段 0.1 拿到的需求描述。** |
| **复用扫描** | 总是发 | 对 diff 中每个新增函数/类，Grep 全仓库找类似实现。`composeApp/src/commonMain/kotlin/com/slax/reader/**` 重点扫 `utils/`、`extension/`、`domain/` |
| **边界扫描** | 改了业务逻辑就发 | 扫 null 解引用、空集合操作、协程取消、`when` 缺 else、`try` 吞异常、可空链滥用 |
| **平台对照** | 改了 androidMain/iosMain 就发 | 找 `expect` 声明，对照所有 `actual` 实现，确认行为一致（线程模型、错误处理、null 语义） |
| **Compose 风险** | 改了 `ui/` 就发 | 检查 `remember` 缺 key、`LaunchedEffect` key 不完整、`mutableStateOf` 在 composable 外被持有、`collectAsState` 没指定初值 |
| **数据层风险** | 改了 `data/` 就发 | PowerSync 同步语义、Ktor 客户端配置、序列化 schema 兼容性 |

**写 Agent prompt 的要点**：
- 给具体文件路径（不要让 Agent 自己摸索范围）
- 要求简短结论（"under 300 words"）
- 明确说"只调研，不修改"

#### (B) 同步启动 Codex /review 作为第二意见

用 Bash 工具调用（**和上面的 Agent 调用在同一条消息里**，并行）：

```bash
codex review \
  --base main \
  "Review this Kotlin Multiplatform Compose PR with focus on: \
   performance (loops with IO, coroutine scopes, Compose recomposition), \
   code quality (naming, error handling, API choice), \
   reuse opportunities, edge cases (null, empty collections, cancellation), \
   hidden bug branches (missing else in when, exception swallowing), \
   and platform-specific correctness (expect/actual, Android vs iOS behavior). \
   Be specific with file paths and line numbers." \
  2>&1 | tee /tmp/slax-review-codex.txt
```

注意：
- `--base main` 让 Codex 对比当前分支和 main
- 把关注维度写进 PROMPT，让 Codex 跟我们关注同样的维度（不然它给的反馈维度可能跑偏）
- `tee` 把输出保留到文件，便于报告引用
- 如果 `--base main` 失败（比如本地没有 main 分支），fallback 到 `--base origin/main`；reviewing uncommitted changes 用 `--uncommitted`；要锁定具体 commit 用 `--commit <sha>`
- Codex 的 review 子命令只接受一个 PROMPT 位置参数，把 prompt 用引号包好

等所有 sub-agent 和 Codex 都返回后，进入阶段 5。

---

### 阶段 5 — 综合输出

**先 Read 输出模板**：

```
./.claude/skills/slax-review/output-template.md
```

把以下材料融合到一份报告：

- 阶段 1 的肉眼点评
- 阶段 2 的调研结论（含搜索记录）
- 阶段 3 的对话澄清（Q&A 记录）
- 阶段 4 (A) 各个 sub-agent 的发现
- 阶段 4 (B) Codex 的意见
- **特别记录跟 Codex 的分歧** —— 这是 review 透明度的关键

写到文件：

```
./.claude/review/review-<branch>-<YYYYMMDD-HHMMSS>.md
```

文件名里的 branch 用 `git branch --show-current` 拿（`/` 换成 `-`），时间戳用 `date +%Y%m%d-%H%M%S`。

写完后在对话里打印「关键发现」摘要（按严重度排序：🔴 高 / 🟡 中 / 🟢 低），并告诉用户报告完整路径。

---

## 工具使用约束（重要）

- **`gh` 没安装** —— 不要尝试 `gh pr view` / `gh pr review`。审查目标用 `git diff` 拿，结果只写本地文件，让用户自己复制到 PR。如果用户后续装了 `gh`，再切换到 `gh pr view` 路径。
- **`./gradlew` 受限** —— `CLAUDE.local.md` / `AGENTS.md` 明确：除非用户明确授权，只能跑 lint 任务，禁止跑其他 gradle 任务。Review skill **永远不要主动 `./gradlew check` / `assemble` / `test`**。如果需要 lint 信号，先用 `AskUserQuestion` 问用户是否授权跑 `./gradlew :composeApp:lint`。
- **Codex CLI** —— `codex`, 子命令是 `review`，关键参数 `--base <branch>` / `--uncommitted` / `--commit <sha>` / 位置参数 PROMPT。
- **路径** —— 所有 Read / Write 都用绝对路径，不要用相对路径或 `./...`。
- **`AskUserQuestion`** —— 不要用 plain text 提问代替它；只要是给用户的选择题，必须用这个工具。

---

## 反模式（不要做）

- 跳过阶段 0 —— 没拿到需求描述就开审，等于没有锚点的瞎审
- 项目缺规范文件却不问用户，直接拿"行业标准"当默认（用户可能有自己的偏好）
- 改动跨了 androidMain/iosMain，只看了一端
- API contract 改了不问后端 PR 在哪
- 没有附需求描述就发"最小化扫描" agent（agent 拿不到对照基准，结论会跑偏）
- 跳过阶段 2 直接出结论（"我看了一遍 diff，没问题"——这不是 review）
- 一次搜 1-2 个目标就完事（应该把所有外部依赖、所有陌生语法都搜一遍）
- 串行发起多个 WebSearch / Agent 调用（必须并行）
- 不问开发者，直接对意图不明的改动下结论
- 对 Codex 的输出不做分析直接抄进报告（要做交叉验证，标记分歧）
- 报告里只写发现，不写「调研记录」（透明度不够）
- 为了快速给结论而省略子 agent 扫描
- 主动跑 `./gradlew` 任务（除 lint 外，必须先得到用户授权）

---

## 自检 checklist（出报告前过一遍）

- [ ] 阶段 0.1 拿到了 PR 需求描述（gh / commit / branch / 用户说明 中至少一种）
- [ ] 阶段 0.2 项目规范文件已确认（slax-reader-client 应命中 AGENTS.md + CLAUDE.local.md）
- [ ] 阶段 0.3 跨端范围已识别 + API contract 联动已询问（如涉及）
- [ ] 阶段 2 实时读了 `gradle/libs.versions.toml`，搜索 query 里带了真实版本号，搜索次数 ≥ 5
- [ ] 阶段 3 至少 1 轮对话（除非 PR 极简单）
- [ ] 阶段 4 (A) 至少发起了 2 个 sub-agent（含「最小化扫描」）
- [ ] 阶段 4 (B) Codex 输出文件 `/tmp/slax-review-codex.txt` 非空
- [ ] 报告里有「上下文摘要」一节（含需求描述 / 规范文件 / 跨端范围）
- [ ] 报告里有「调研记录」表格
- [ ] 报告里有「与 Codex 的分歧」一节（即使分歧为空也要写「无分歧」）
- [ ] 报告里有「用户对话记录」
- [ ] 所有发现都标了 `file:line`
- [ ] 报告路径是绝对路径

如果某项 unchecked，回去补，不要交付半成品。
