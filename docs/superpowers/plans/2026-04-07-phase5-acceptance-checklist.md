# Phase 5 最终验收清单单文档 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增一份单文档验收清单，把最终验收步骤、证据清单、截图要求收口到 `phase5-acceptance-checklist.md`，并同步回写状态文档。

**Architecture:** 保持第一包的 README 与联调手册不重写，只在现有后端 worktree 中新增一份验收主文档，并把 `继续执行文档-完整系统.md` 调整到与第二包一致的续跑状态。验收清单按“准备 -> 启动验收 -> 联调验收 -> 证据与截图 -> 通过判定”的顺序组织，继续把最小主链和可选增强链路分开。

**Tech Stack:** Markdown, Maven, Vue 3, Vite, Vitest

---

### Task 1: 新增最终验收清单骨架与必验项结构

**Files:**
- Create: `D:/Graduation project/backend/.worktrees/phase5-docs-runbook/docs/integration/phase5-acceptance-checklist.md`
- Test: `D:/Graduation project/backend/.worktrees/phase5-docs-runbook/README.md`
- Test: `D:/Graduation project/frontend/.worktrees/phase5-docs-runbook/README.md`
- Test: `D:/Graduation project/backend/.worktrees/phase5-docs-runbook/docs/integration/phase5-local-runbook.md`

- [ ] **Step 1: 写入验收清单文档头部与 6 段骨架**

````markdown
# Phase 5 最终验收清单

## 1. 文档用途与适用范围

- 本文档用于最终验收与留证，不替代后端 README、前端 README 和本地联调手册。
- 执行顺序：先按 README 准备环境，再按联调手册完成启动与最小联调，最后按本文档做最终验收和留证。

## 2. 验收前准备

## 3. 启动验收

## 4. 联调验收

## 5. 证据与截图要求

## 6. 最终通过判定与遗留问题记录
````

- [ ] **Step 2: 在第 2-4 段写入必验项结构，不留空标题**

````markdown
## 2. 验收前准备

- 后端工作目录：`D:\Graduation project\backend\.worktrees\phase5-docs-runbook`
- 前端工作目录：`D:\Graduation project\frontend\.worktrees\phase5-docs-runbook`
- 已完成首包文档：后端 README、前端 README、本地联调手册
- 可选增强链路：实时新打卡、地图展示、复杂异常分析、多地点异常

## 3. 启动验收

### 3.1 后端最小测试

- 操作：运行 `mvn "-Dtest=DeviceManagementIntegrationTest,AuthSecurityIntegrationTest,ModuleSkeletonBeansTest" test`
- 预期：`BUILD SUCCESS`

### 3.2 后端编译校验

- 操作：运行 `mvn -DskipTests compile`
- 预期：`BUILD SUCCESS`

### 3.3 前端测试校验

- 操作：运行 `npm test`
- 预期：所有测试通过

### 3.4 前端构建校验

- 操作：运行 `npm run build`
- 预期：构建成功；如只有 chunk 告警，记录为非阻塞项

## 4. 联调验收

- 最小主链只覆盖：登录 -> 异常查询 -> 预警查询 -> 复核提交 -> 统计查询
- 可选增强链路不纳入首个完成门槛
````

- [ ] **Step 3: 校对骨架与首包文档的一致性**

Run: 对照以下文件逐项核对路径、端口、命令、链路口径是否一致

- `D:/Graduation project/backend/.worktrees/phase5-docs-runbook/README.md`
- `D:/Graduation project/frontend/.worktrees/phase5-docs-runbook/README.md`
- `D:/Graduation project/backend/.worktrees/phase5-docs-runbook/docs/integration/phase5-local-runbook.md`

Expected: 新文档中的工作目录、端口 `8080/5173`、最小命令和“seed 优先主链”口径一致

### Task 2: 写入证据清单、截图要求与可选增强验收边界

**Files:**
- Modify: `D:/Graduation project/backend/.worktrees/phase5-docs-runbook/docs/integration/phase5-acceptance-checklist.md`
- Test: `D:/Graduation project/backend/.worktrees/phase5-docs-runbook/docs/integration/phase5-local-runbook.md`

- [ ] **Step 1: 为每个必验项补齐“操作/预期/证据/失败时记录”四行模板**

````markdown
### 3.1 后端最小测试

- 操作：在后端 worktree 根目录运行 `mvn "-Dtest=DeviceManagementIntegrationTest,AuthSecurityIntegrationTest,ModuleSkeletonBeansTest" test`
- 预期：`Tests run: 41, Failures: 0, Errors: 0, Skipped: 0`，且 `BUILD SUCCESS`
- 证据：保留终端输出截图或原始文本
- 失败时记录：失败的测试类名、失败摘要、是否阻塞 Phase 5 完成
````

- [ ] **Step 2: 写入联调验收项的截图和证据要求**

````markdown
### 4.1 登录成功

- 操作：按首包联调手册使用 seed 基线完成登录
- 预期：成功进入业务页面
- 证据：登录后页面截图 + 至少一条成功接口响应证据
- 失败时记录：账号、接口状态码、代理目标、页面报错信息

### 4.2 异常查询

- 操作：查询 seed 中的 `3002` 或 `3003`
- 预期：页面可见异常记录
- 证据：异常列表或详情截图，能看出异常编号/类型
- 失败时记录：当前数据库是否已导入 schema + seed、查询条件、页面返回信息

### 4.3 预警查询与复核

- 操作：定位 `5002` 或 `5003`，完成一次复核提交
- 预期：预警状态从 `UNPROCESSED` 变为 `PROCESSED`
- 证据：复核提交成功截图 + 状态变化截图
- 失败时记录：异常编号、预警编号、复核返回信息、状态未流转现象

### 4.4 统计查询

- 操作：进入统计页查询当前异常汇总
- 预期：能看到已导入/已复核数据形成的统计结果
- 证据：统计页截图，能看出异常汇总或分布结果
- 失败时记录：查询条件、页面报错、是否缺少前置数据
````

- [ ] **Step 3: 单独列出可选增强验收项，不与必验项混排**

````markdown
## 4.5 可选增强验收

以下项目不阻塞首个 Phase 5 完成判定，但如果已验证，应补证据：

- 实时新打卡链路
- 地图展示
- 复杂异常分析
- 多地点异常
- 导出类页面演示

每项都必须写明：
- 是否已验证
- 未验证原因
- 若已验证，证据放在哪里
````

- [ ] **Step 4: 交叉检查必验项与可选增强项边界**

Run: 通读 `phase5-acceptance-checklist.md` 与 `phase5-local-runbook.md`
Expected: “seed 优先最小主链”仍在必验项；实时新打卡、地图、复杂异常、多地点异常只出现在可选增强区

### Task 3: 写入最终通过判定并回写状态文档

**Files:**
- Modify: `D:/Graduation project/backend/.worktrees/phase5-docs-runbook/docs/integration/phase5-acceptance-checklist.md`
- Modify: `D:/Graduation project/继续执行文档-完整系统.md`

- [ ] **Step 1: 在验收清单中补齐最终通过判定与遗留问题模板**

````markdown
## 6. 最终通过判定与遗留问题记录

### 6.1 允许标记 Phase 5 完成的条件

- 启动验收全部通过
- 联调验收全部通过
- 证据与截图已留存
- README、联调手册、验收清单三者口径一致

### 6.2 不允许标记完成的情况

- 任何一个必验项失败
- 缺少证据或截图
- 文档之间路径、端口、命令、口径冲突

### 6.3 已知问题记录

| 编号 | 现象 | 是否阻塞 | 说明 |
| --- | --- | --- | --- |
| 1 | 前端 build 存在 chunk 大小告警 | 否 | 当前不阻塞 Phase 5 首次完成 |
````

- [ ] **Step 2: 回写状态文档，切到第二包进行中状态**

```markdown
- 当前阶段仍为 `Phase 5 - 文档、部署、联调、验收闭环`
- 本轮完成：第二包主文档 `phase5-acceptance-checklist.md`
- 本轮未完成：只有在验收清单与状态文档都收口并确认一致后，才允许整体标记 Phase 5 完成
- 下一步第一动作：按验收清单完成一次最终一致性复核，并决定是否进入分支收尾
```

- [ ] **Step 3: 回写第二包最小验证与证据校对口径**

```markdown
在 `继续执行文档-完整系统.md` 中补充：
- 第二包最小读取清单新增 `phase5-acceptance-checklist.md`
- 第二包最小验证包括：README、联调手册、验收清单三者一致性校对
- 如前端 `npm run build` 仍有 chunk 告警，继续记为非阻塞已知项
```

- [ ] **Step 4: 最终人工检查三份 Phase 5 主文档与状态文档都已落地**

```powershell
Get-Item "D:\Graduation project\backend\.worktrees\phase5-docs-runbook\README.md"
Get-Item "D:\Graduation project\frontend\.worktrees\phase5-docs-runbook\README.md"
Get-Item "D:\Graduation project\backend\.worktrees\phase5-docs-runbook\docs\integration\phase5-local-runbook.md"
Get-Item "D:\Graduation project\backend\.worktrees\phase5-docs-runbook\docs\integration\phase5-acceptance-checklist.md"
Get-Item "D:\Graduation project\继续执行文档-完整系统.md"
```

Expected: 5 个路径全部存在，且第二包主文档与状态文档已被本轮修改
