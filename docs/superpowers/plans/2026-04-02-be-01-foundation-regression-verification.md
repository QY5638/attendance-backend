# BE-01 基础分支回归验证 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 `feature/be-01-user-auth-foundation` 补做更高强度的回归验证，确认认证、部门管理、用户管理三条主线在当前 worktree 中整体稳定。

**Architecture:** 本计划不新增功能，也不主动修改生产代码；先跑三组核心集成测试建立分支级信心，再在结果允许时提升到 `mvn test` 做更高强度验证。若任一验证失败，停止扩展验证并转入基于失败证据的最小定位，而不是直接盲改。

**Tech Stack:** Spring Boot 2.7、Maven Surefire、JUnit 5、H2 测试环境

> 当前用户要求：本计划执行阶段不包含 `git commit`、`git push`、创建 PR 或清理 worktree。

---

## 文件结构与职责

- Read: `target/surefire-reports/*.txt`
  - 在测试失败或需要汇总时读取 Surefire 报告，确认失败测试类、失败数量和错误摘要。
- Read: `docs/superpowers/specs/2026-04-02-be-01-foundation-regression-verification-design.md`
  - 作为本计划的设计依据。
- Modify: 无预期代码改动
  - 本计划以验证为主；只有在后续单独确认修复设计时，才会进入代码修改。

### Task 1: 运行核心三件套回归

**Files:**
- Read: `target/surefire-reports/*.txt`

- [x] **Step 1: 运行认证、部门、用户三组核心回归**

Run: `mvn "-Dtest=UserManagementIntegrationTest,AuthSecurityIntegrationTest,DepartmentManagementIntegrationTest" test`

Expected:
- Maven 退出码为 0
- 输出包含 `Failures: 0`
- 输出包含 `Errors: 0`
- 三个测试类全部执行完成

- [x] **Step 2: 记录核心回归结果并判断是否允许进入全量验证**

Expected:
- 若 Step 1 通过，记录通过数量并进入 Task 2
- 若 Step 1 失败，停止执行后续任务，读取 `target/surefire-reports/*.txt` 记录失败测试类与摘要，并准备转入 `systematic-debugging`

#### Task 1 执行记录

- 命令：`mvn "-Dtest=UserManagementIntegrationTest,AuthSecurityIntegrationTest,DepartmentManagementIntegrationTest" test`
- `AuthSecurityIntegrationTest`：16 / 0 / 0
- `DepartmentManagementIntegrationTest`：14 / 0 / 0
- `UserManagementIntegrationTest`：25 / 0 / 0
- 汇总：55 / 0 / 0 / 0
- 结果：`BUILD SUCCESS`

### Task 2: 在核心回归通过后提升到全量验证

**Files:**
- Read: `target/surefire-reports/*.txt`

- [x] **Step 1: 运行全量测试**

Run: `mvn test`

Expected:
- Maven 退出码为 0
- 输出包含 `Failures: 0`
- 输出包含 `Errors: 0`

- [x] **Step 2: 记录全量验证结果**

Expected:
- 若 Step 1 通过，记录总测试数、失败数、错误数，进入 Task 3
- 若 Step 1 失败，停止继续扩展动作，读取 `target/surefire-reports/*.txt` 记录失败测试类与摘要，并准备转入 `systematic-debugging`

#### Task 2 执行记录

- 命令：`mvn test`
- `AttendanceApplicationTests`：1 / 0 / 0
- `AuthSecurityIntegrationTest`：16 / 0 / 0
- `DatabaseIntegrationBaselineTest`：1 / 0 / 0
- `DepartmentManagementIntegrationTest`：14 / 0 / 0
- `DevProfileContextStartupTest`：1 / 0 / 0
- `HealthControllerTest`：1 / 0 / 0
- `RedisTokenStoreTest`：1 / 0 / 0
- `ModuleSkeletonBeansTest`：1 / 0 / 0
- `UserManagementIntegrationTest`：25 / 0 / 0
- 汇总：61 / 0 / 0 / 0
- 结果：`BUILD SUCCESS`

### Task 3: 汇总剩余风险与后续建议

**Files:**
- Read: `target/surefire-reports/*.txt`

- [x] **Step 1: 汇总验证覆盖范围与结果**

Expected:
- 明确本轮已执行的命令
- 明确每轮验证的测试数量与通过情况
- 区分“核心回归通过”和“全量测试通过”两个层级

- [x] **Step 2: 输出剩余风险与下一步建议**

Expected:
- 若全部通过，说明当前剩余风险主要在 worktree 脏状态和未提交内容管理，而不是已知功能缺口
- 若出现失败，说明失败点、影响范围，以及下一步需先进入 `systematic-debugging` 再决定是否修复

#### Task 3 执行记录

- 已执行命令：`mvn "-Dtest=UserManagementIntegrationTest,AuthSecurityIntegrationTest,DepartmentManagementIntegrationTest" test`、`mvn test`
- 核心三件套结果：55 / 0 / 0 / 0
- 全量测试结果：61 / 0 / 0 / 0
- 结论：从本轮已验证范围看，未发现已验证范围内的功能缺口；当前剩余风险主要集中在 worktree 脏状态、未提交内容管理以及测试覆盖边界。
- 下一步建议：进入提交/评审/合并前，基于最终候选内容再执行一次 `mvn test`。
