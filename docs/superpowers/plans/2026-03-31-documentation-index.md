# 项目文档索引实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a single-entry documentation workspace in `D:\Graduation project\backend\docs\README.md` and remove the last stale external path reference so all project guidance points to files inside the repository workspace.

**Architecture:** Keep the existing `docs/` and `sql/` folder structure unchanged and add one focused Markdown index file that explains what each document is for, when to read it, and what must be updated when requirements change. Apply one small consistency fix in the existing test document so the README and the referenced SQL seed file use the same in-project path.

**Tech Stack:** Markdown, PowerShell verification commands, existing project documentation files

---

## File Structure

- Create: `D:\Graduation project\backend\docs\README.md`
  - Responsibility: Serve as the documentation hub for development, testing, and thesis preparation.
- Modify: `D:\Graduation project\backend\docs\test\测试用例文档.md`
  - Responsibility: Replace the stale external SQL seed path with the in-project SQL seed path.
- Reference: `D:\Graduation project\backend\AGENTS.md`
  - Responsibility: Remains the rule source that the new README should align with for document sync rules.
- Reference: `D:\Graduation project\backend\docs\requirements\需求规格说明书.md`
  - Responsibility: Provides the product goal and should appear in the reading order.
- Reference: `D:\Graduation project\backend\docs\architecture\系统架构设计文档.md`
  - Responsibility: Provides top-level architecture context for the index.
- Reference: `D:\Graduation project\backend\docs\architecture\考勤异常检测与预警系统设计文档.md`
  - Responsibility: Provides broader system design context for the index.
- Reference: `D:\Graduation project\backend\docs\architecture\模块划分说明书.md`
  - Responsibility: Provides module boundary guidance for the index.
- Reference: `D:\Graduation project\backend\docs\architecture\用例图与流程图文字说明.md`
  - Responsibility: Provides business flow context for the index.
- Reference: `D:\Graduation project\backend\docs\database\数据库设计文档.md`
  - Responsibility: Provides database design guidance for the index.
- Reference: `D:\Graduation project\backend\docs\database\数据库E-R关系说明.md`
  - Responsibility: Provides table relation guidance for the index.
- Reference: `D:\Graduation project\backend\docs\api\API接口设计文档.md`
  - Responsibility: Provides API contract guidance for the index.
- Reference: `D:\Graduation project\backend\docs\test\测试用例文档.md`
  - Responsibility: Provides testing scope guidance for the index and receives the path fix.
- Reference: `D:\Graduation project\backend\sql\schema\数据库建表SQL.sql`
  - Responsibility: Schema entry file linked from the index.
- Reference: `D:\Graduation project\backend\sql\seed\测试数据插入SQL.sql`
  - Responsibility: Seed data entry file linked from the index and the updated test document.

## Implementation Notes

- This workspace is currently not a git repository, so the execution plan does not include `git commit` steps.
- Verification is command-based because this task adds and edits Markdown rather than application code.
- Keep all content in Chinese to match `D:\Graduation project\backend\AGENTS.md`.

### Task 1: 创建文档工作台首页

**Files:**
- Create: `D:\Graduation project\backend\docs\README.md`
- Reference: `D:\Graduation project\backend\AGENTS.md`
- Reference: `D:\Graduation project\backend\docs\requirements\需求规格说明书.md`
- Reference: `D:\Graduation project\backend\docs\architecture\系统架构设计文档.md`
- Reference: `D:\Graduation project\backend\docs\architecture\考勤异常检测与预警系统设计文档.md`
- Reference: `D:\Graduation project\backend\docs\architecture\模块划分说明书.md`
- Reference: `D:\Graduation project\backend\docs\architecture\用例图与流程图文字说明.md`
- Reference: `D:\Graduation project\backend\docs\database\数据库设计文档.md`
- Reference: `D:\Graduation project\backend\docs\database\数据库E-R关系说明.md`
- Reference: `D:\Graduation project\backend\docs\api\API接口设计文档.md`
- Reference: `D:\Graduation project\backend\docs\test\测试用例文档.md`
- Reference: `D:\Graduation project\backend\docs\reference\222226209229-屈勇-基于 SpringBoot 与大模型 API 的考勤异常检测与预警系统设计与实现-任务书.docx`
- Reference: `D:\Graduation project\backend\docs\reference\开题报告 （最终版）.docx`
- Reference: `D:\Graduation project\backend\sql\schema\数据库建表SQL.sql`
- Reference: `D:\Graduation project\backend\sql\seed\测试数据插入SQL.sql`

- [ ] **Step 1: Run the failing existence check**

Run: `powershell -NoProfile -Command "Test-Path 'D:\Graduation project\backend\docs\README.md'"`
Expected: `False`

- [ ] **Step 2: Write the documentation hub file**

```md
# 项目文档索引

## 1. 文档说明

本目录用于集中维护“基于 SpringBoot 与大模型 API 的考勤异常检测与预警系统”项目资料。

- 开发入口：先看需求、架构、模块边界，再看数据库、接口、测试文档。
- 联调入口：优先查看 API 文档、测试文档、建表 SQL、测试数据 SQL。
- 论文与答辩入口：优先查看任务书、开题报告、需求文档、系统设计文档、统计与测试相关材料。

## 2. 推荐阅读顺序

1. `requirements/需求规格说明书.md`：明确系统目标、角色和核心功能。
2. `architecture/系统架构设计文档.md`：了解系统分层、总体架构和技术选型。
3. `architecture/考勤异常检测与预警系统设计文档.md`：了解整体系统设计与核心实现思路。
4. `architecture/模块划分说明书.md`：明确模块职责边界和模块关系。
5. `architecture/用例图与流程图文字说明.md`：了解主要业务流程与交互场景。
6. `database/数据库设计文档.md`：查看数据库对象设计与字段说明。
7. `database/数据库E-R关系说明.md`：查看实体关系与表之间的关联。
8. `api/API接口设计文档.md`：查看接口路径、请求参数和统一返回结构。
9. `test/测试用例文档.md`：查看测试目标、范围、环境与核心用例。
10. `../sql/schema/数据库建表SQL.sql`：初始化数据库结构。
11. `../sql/seed/测试数据插入SQL.sql`：导入测试数据。

## 3. 分类索引

### 3.1 需求文档

- `requirements/需求规格说明书.md`
  - 用途：定义项目背景、目标、角色、功能需求和非功能需求。
  - 何时查看：开始开发新模块、核对需求边界、撰写论文需求分析章节时。

### 3.2 架构与设计文档

- `architecture/系统架构设计文档.md`
  - 用途：说明系统总体架构、分层设计和技术选型。
  - 何时查看：确定前后端边界、服务职责、缓存与数据库关系时。
- `architecture/考勤异常检测与预警系统设计文档.md`
  - 用途：补充系统级设计说明，便于整体把握方案。
  - 何时查看：整理系统设计章节、统一实现思路时。
- `architecture/模块划分说明书.md`
  - 用途：定义核心模块、子功能和职责边界。
  - 何时查看：拆分任务、设计 controller/service/mapper 分工时。
- `architecture/用例图与流程图文字说明.md`
  - 用途：补充业务流程和典型交互说明。
  - 何时查看：梳理登录、打卡、异常检测、复核流程时。

### 3.3 数据库文档

- `database/数据库设计文档.md`
  - 用途：说明核心表、字段和数据库选型。
  - 何时查看：新增实体、调整字段、实现 mapper 或 SQL 时。
- `database/数据库E-R关系说明.md`
  - 用途：说明表之间的实体关系。
  - 何时查看：处理关联查询、外键关系、模块间数据流时。

### 3.4 接口文档

- `api/API接口设计文档.md`
  - 用途：定义 RESTful 接口路径、请求参数和响应结构。
  - 何时查看：新增接口、前后端联调、接口验收时。

### 3.5 测试文档

- `test/测试用例文档.md`
  - 用途：说明测试目标、测试范围、测试环境与核心测试用例。
  - 何时查看：编写测试、联调验证、整理测试章节时。

### 3.6 参考资料

- `reference/222226209229-屈勇-基于 SpringBoot 与大模型 API 的考勤异常检测与预警系统设计与实现-任务书.docx`
  - 用途：记录课题来源、任务要求和阶段目标。
  - 何时查看：核对毕设范围、答辩材料准备时。
- `reference/开题报告 （最终版）.docx`
  - 用途：记录研究背景、研究内容、技术路线和进度安排。
  - 何时查看：整理研究思路、答辩陈述和论文绪论时。

## 4. SQL 入口

- `../sql/schema/数据库建表SQL.sql`：数据库结构初始化脚本。
- `../sql/seed/测试数据插入SQL.sql`：测试数据初始化脚本。

## 5. 文档同步规则

- 修改数据库结构时，必须同步更新：
  - `database/数据库设计文档.md`
  - `database/数据库E-R关系说明.md`
  - `../sql/schema/数据库建表SQL.sql`
  - `../sql/seed/测试数据插入SQL.sql`
- 修改接口时，必须同步更新：
  - `api/API接口设计文档.md`
- 修改核心模块边界或系统结构时，必须同步更新：
  - `architecture/系统架构设计文档.md`
  - `architecture/模块划分说明书.md`
- 修改测试范围、测试环境或验证方式时，必须同步更新：
  - `test/测试用例文档.md`

## 6. 使用建议

- 开始新模块前，先确认需求文档、模块划分说明书和数据库文档是否一致。
- 改动代码、SQL、接口后，按本页同步规则检查对应文档是否需要更新。
- 如果后续新增文档目录或关键资料，应先补充本索引，再开始扩展实现。
```

- [ ] **Step 3: Run the content verification command**

Run: `powershell -NoProfile -Command "Select-String -Path 'D:\Graduation project\backend\docs\README.md' -SimpleMatch '项目文档索引','推荐阅读顺序','文档同步规则','../sql/seed/测试数据插入SQL.sql' | ForEach-Object { $_.Line }"`
Expected: Output includes the four matched lines from the new README.

### Task 2: 修正测试文档中的旧路径

**Files:**
- Modify: `D:\Graduation project\backend\docs\test\测试用例文档.md:38`
- Reference: `D:\Graduation project\backend\sql\seed\测试数据插入SQL.sql`

- [ ] **Step 1: Run the failing reference check**

Run: `powershell -NoProfile -Command "Get-Content 'D:\Graduation project\backend\docs\test\测试用例文档.md' | Select-Object -Index 37"`
Expected: Output shows line 38 and confirms it still points to the old external seed SQL source before replacement.

- [ ] **Step 2: Replace the stale path with the in-project path**

```md
测试数据建议通过 `D:\Graduation project\backend\sql\seed\测试数据插入SQL.sql` 导入。
```

- [ ] **Step 3: Run the pass verification for the updated line**

Run: `powershell -NoProfile -Command "Select-String -Path 'D:\Graduation project\backend\docs\test\测试用例文档.md' -SimpleMatch 'D:\Graduation project\backend\sql\seed\测试数据插入SQL.sql' | ForEach-Object { $_.Line }"`
Expected: Output contains the new in-project path line.

### Task 3: 校验文档体系内部一致性

**Files:**
- Verify: `D:\Graduation project\backend\docs\README.md`
- Verify: `D:\Graduation project\backend\docs\test\测试用例文档.md`
- Verify: `D:\Graduation project\backend\docs\requirements\需求规格说明书.md`
- Verify: `D:\Graduation project\backend\docs\architecture\系统架构设计文档.md`
- Verify: `D:\Graduation project\backend\docs\architecture\考勤异常检测与预警系统设计文档.md`
- Verify: `D:\Graduation project\backend\docs\architecture\模块划分说明书.md`
- Verify: `D:\Graduation project\backend\docs\architecture\用例图与流程图文字说明.md`
- Verify: `D:\Graduation project\backend\docs\database\数据库设计文档.md`
- Verify: `D:\Graduation project\backend\docs\database\数据库E-R关系说明.md`
- Verify: `D:\Graduation project\backend\docs\api\API接口设计文档.md`
- Verify: `D:\Graduation project\backend\sql\schema\数据库建表SQL.sql`
- Verify: `D:\Graduation project\backend\sql\seed\测试数据插入SQL.sql`

- [ ] **Step 1: Verify every indexed file exists**

Run: `powershell -NoProfile -Command "$paths = @('D:\Graduation project\backend\docs\README.md','D:\Graduation project\backend\docs\requirements\需求规格说明书.md','D:\Graduation project\backend\docs\architecture\系统架构设计文档.md','D:\Graduation project\backend\docs\architecture\考勤异常检测与预警系统设计文档.md','D:\Graduation project\backend\docs\architecture\模块划分说明书.md','D:\Graduation project\backend\docs\architecture\用例图与流程图文字说明.md','D:\Graduation project\backend\docs\database\数据库设计文档.md','D:\Graduation project\backend\docs\database\数据库E-R关系说明.md','D:\Graduation project\backend\docs\api\API接口设计文档.md','D:\Graduation project\backend\docs\test\测试用例文档.md','D:\Graduation project\backend\sql\schema\数据库建表SQL.sql','D:\Graduation project\backend\sql\seed\测试数据插入SQL.sql'); $paths | ForEach-Object { '{0} => {1}' -f $_, (Test-Path $_) }"`
Expected: Every listed path ends with `=> True`.

- [ ] **Step 2: Verify no stale external path remains in project docs**

Run: `python -c "from pathlib import Path; import re; pattern = re.compile(r'[A-Za-z]:\\(?!Graduation project\\)'); matches = []; [matches.append(str(path)) for path in Path(r'D:\Graduation project').rglob('*') if path.suffix in {'.md', '.sql'} and pattern.search(path.read_text(encoding='utf-8'))]; print('\n'.join(matches))"`
Expected: No output.

- [ ] **Step 3: Verify the README reading order and sync rules are present together**

Run: `powershell -NoProfile -Command "Select-String -Path 'D:\Graduation project\backend\docs\README.md' -SimpleMatch '推荐阅读顺序','分类索引','SQL 入口','文档同步规则','使用建议' | ForEach-Object { $_.Line }"`
Expected: Output contains lines for all five sections.
