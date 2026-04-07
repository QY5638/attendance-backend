# BE-08 统计分析与审计模块 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 `statistics` 模块一次性补齐基于 `BE-04/05/06/07` 的完整闭环统计、CSV 导出与操作审计查询，并以最小必要改动补齐 `operationLog` 数据来源。

**Architecture:** 沿用现有 `controller/service/mapper/entity/dto/vo/support` 分层，在 `module/statistics` 下新增读查询能力，不新增表、不新增中间读模型。统计时间统一锚定 `attendanceRecord.checkTime`，通过聚合串通 `attendanceRecord`、`attendanceException`、`exceptionAnalysis`、`warningRecord`、`reviewRecord`、`user`、`department`，同时把 `operationLog` 作为用户操作审计源表，把 `modelCallLog` 与 `decisionTrace` 纳入审计完备性评估背景。

**Tech Stack:** Spring Boot 2.7、Spring Security、MyBatis-Plus、H2 集成测试、MockMvc、JdbcTemplate、CSV 文本导出

---

## File Map

### Create

- `src/main/java/com/quyong/attendance/module/statistics/controller/StatisticsController.java`
- `src/main/java/com/quyong/attendance/module/statistics/controller/OperationLogController.java`
- `src/main/java/com/quyong/attendance/module/statistics/service/StatisticsService.java`
- `src/main/java/com/quyong/attendance/module/statistics/service/OperationLogService.java`
- `src/main/java/com/quyong/attendance/module/statistics/service/impl/StatisticsServiceImpl.java`
- `src/main/java/com/quyong/attendance/module/statistics/service/impl/OperationLogServiceImpl.java`
- `src/main/java/com/quyong/attendance/module/statistics/mapper/StatisticsMapper.java`
- `src/main/java/com/quyong/attendance/module/statistics/mapper/OperationLogMapper.java`
- `src/main/java/com/quyong/attendance/module/statistics/entity/OperationLog.java`
- `src/main/java/com/quyong/attendance/module/statistics/support/StatisticsValidationSupport.java`
- `src/main/java/com/quyong/attendance/module/statistics/support/StatisticsSummarySupport.java`
- `src/main/java/com/quyong/attendance/module/statistics/dto/PersonalStatisticsQueryDTO.java`
- `src/main/java/com/quyong/attendance/module/statistics/dto/DepartmentStatisticsQueryDTO.java`
- `src/main/java/com/quyong/attendance/module/statistics/dto/ExceptionTrendQueryDTO.java`
- `src/main/java/com/quyong/attendance/module/statistics/dto/StatisticsSummaryQueryDTO.java`
- `src/main/java/com/quyong/attendance/module/statistics/dto/DepartmentRiskBriefQueryDTO.java`
- `src/main/java/com/quyong/attendance/module/statistics/dto/StatisticsExportQueryDTO.java`
- `src/main/java/com/quyong/attendance/module/statistics/dto/OperationLogQueryDTO.java`
- `src/main/java/com/quyong/attendance/module/statistics/vo/PersonalStatisticsVO.java`
- `src/main/java/com/quyong/attendance/module/statistics/vo/DepartmentStatisticsVO.java`
- `src/main/java/com/quyong/attendance/module/statistics/vo/ExceptionTrendVO.java`
- `src/main/java/com/quyong/attendance/module/statistics/vo/ExceptionTrendPointVO.java`
- `src/main/java/com/quyong/attendance/module/statistics/vo/StatisticsSummaryVO.java`
- `src/main/java/com/quyong/attendance/module/statistics/vo/DepartmentRiskBriefVO.java`
- `src/main/java/com/quyong/attendance/module/statistics/vo/OperationLogVO.java`
- `src/test/java/com/quyong/attendance/StatisticsControllerTest.java`

### Modify

- `src/main/java/com/quyong/attendance/config/SecurityConfig.java`
- `src/main/java/com/quyong/attendance/module/auth/service/impl/AuthServiceImpl.java`
- `src/main/java/com/quyong/attendance/module/attendance/service/impl/AttendanceServiceImpl.java`
- `src/main/java/com/quyong/attendance/module/warning/service/impl/WarningServiceImpl.java`
- `src/main/java/com/quyong/attendance/module/review/service/impl/ReviewServiceImpl.java`
- `src/test/resources/schema.sql`
- `docs/api/API接口设计文档.md`
- `docs/module-guides/BE-08-统计分析与审计模块开发指南.md`
- `docs/test/测试用例文档.md`

## Task 0: Worktree 与基线

**Files:**
- Verify: `.gitignore`
- Verify: `.worktrees/be-08-statistics-audit`

- [x] **Step 1: 校验 `.worktrees` 已被忽略**

Run: `git check-ignore .worktrees`

Expected: 输出 `.worktrees`

- [x] **Step 2: 基于 `origin/main` 创建隔离 worktree 和分支**

Run: `git worktree add -b "feature/be-08-statistics-audit" ".worktrees/be-08-statistics-audit" "origin/main"`

Expected: 输出包含 `Preparing worktree` 与新分支名。

- [x] **Step 3: 运行 BE-04 ~ BE-07 最小基线测试**

Run: `mvn "-Dtest=ExceptionControllerTest,WarningControllerTest,ReviewControllerTest" test`

Expected: `BUILD SUCCESS`

## Task 1: 先补测试库里的 `operationLog`

**Files:**
- Modify: `src/test/resources/schema.sql`
- Test: `src/test/java/com/quyong/attendance/StatisticsControllerTest.java`

- [ ] **Step 1: 在 H2 schema 中补上 `operationLog` 表定义**
- [ ] **Step 2: 运行 `StatisticsControllerTest`，确认在控制器未实现前因缺少 bean 或接口而失败，而不是因表缺失而失败**

## Task 2: 先写 BE-08 失败测试，固定完整闭环口径

**Files:**
- Create: `src/test/java/com/quyong/attendance/StatisticsControllerTest.java`

- [ ] **Step 1: 创建失败测试，覆盖个人统计、部门统计、趋势、摘要、风险简报、导出、审计日志与权限**
- [ ] **Step 2: 单独运行 `StatisticsControllerTest`，确认失败原因是 `statistics` 模块尚未实现**

## Task 3: 补最小 `operationLog` 落库能力

**Files:**
- Create: `src/main/java/com/quyong/attendance/module/statistics/entity/OperationLog.java`
- Create: `src/main/java/com/quyong/attendance/module/statistics/mapper/OperationLogMapper.java`
- Create: `src/main/java/com/quyong/attendance/module/statistics/service/OperationLogService.java`
- Create: `src/main/java/com/quyong/attendance/module/statistics/service/impl/OperationLogServiceImpl.java`

- [ ] **Step 1: 建立与现有表结构一致的 `OperationLog` 实体与 Mapper**
- [ ] **Step 2: 提供最小 `save(userId, type, content)` 服务**
- [ ] **Step 3: 运行 `StatisticsControllerTest`，确认仍然因统计接口缺失失败**

## Task 4: 给关键人工操作补审计写链

**Files:**
- Modify: `src/main/java/com/quyong/attendance/module/auth/service/impl/AuthServiceImpl.java`
- Modify: `src/main/java/com/quyong/attendance/module/attendance/service/impl/AttendanceServiceImpl.java`
- Modify: `src/main/java/com/quyong/attendance/module/warning/service/impl/WarningServiceImpl.java`
- Modify: `src/main/java/com/quyong/attendance/module/review/service/impl/ReviewServiceImpl.java`

- [ ] **Step 1: 在登录成功后写入 `LOGIN`**
- [ ] **Step 2: 在打卡成功后写入 `CHECKIN`**
- [ ] **Step 3: 在补卡申请成功后写入 `REPAIR`**
- [ ] **Step 4: 在预警重评估成功后写入 `WARNING`**
- [ ] **Step 5: 在复核提交、反馈成功后写入 `REVIEW`**
- [ ] **Step 6: 运行与这些行为直接相关的测试，确保原业务结果未被破坏**

## Task 5: 落 `statistics` DTO / VO / 校验支撑

**Files:**
- Create: `src/main/java/com/quyong/attendance/module/statistics/dto/*.java`
- Create: `src/main/java/com/quyong/attendance/module/statistics/vo/*.java`
- Create: `src/main/java/com/quyong/attendance/module/statistics/support/StatisticsValidationSupport.java`

- [ ] **Step 1: 固定查询参数和默认分页/日期校验**
- [ ] **Step 2: 固定返回字段，确保字段名与 API 文档一致**

## Task 6: 实现聚合 Mapper 与服务

**Files:**
- Create: `src/main/java/com/quyong/attendance/module/statistics/mapper/StatisticsMapper.java`
- Create: `src/main/java/com/quyong/attendance/module/statistics/service/StatisticsService.java`
- Create: `src/main/java/com/quyong/attendance/module/statistics/service/impl/StatisticsServiceImpl.java`
- Create: `src/main/java/com/quyong/attendance/module/statistics/support/StatisticsSummarySupport.java`

- [ ] **Step 1: 实现个人统计聚合**
- [ ] **Step 2: 实现部门统计聚合**
- [ ] **Step 3: 实现异常趋势聚合**
- [ ] **Step 4: 实现总览摘要文本和管理建议**
- [ ] **Step 5: 实现部门风险评分与风险简报**
- [ ] **Step 6: 实现 CSV 导出**

## Task 7: 实现控制器与权限

**Files:**
- Create: `src/main/java/com/quyong/attendance/module/statistics/controller/StatisticsController.java`
- Create: `src/main/java/com/quyong/attendance/module/statistics/controller/OperationLogController.java`
- Modify: `src/main/java/com/quyong/attendance/config/SecurityConfig.java`

- [ ] **Step 1: 暴露 `/api/statistics/*` 与 `/api/log/operation/list`**
- [ ] **Step 2: 允许员工访问个人统计且仅限本人**
- [ ] **Step 3: 保持其他统计和审计查询为管理员权限**

## Task 8: 跑通 TDD 闭环并补文档

**Files:**
- Modify: `docs/api/API接口设计文档.md`
- Modify: `docs/module-guides/BE-08-统计分析与审计模块开发指南.md`
- Modify: `docs/test/测试用例文档.md`

- [ ] **Step 1: 让 `StatisticsControllerTest` 通过**
- [ ] **Step 2: 运行 `mvn "-Dtest=StatisticsControllerTest" test` 做 BE-08 最小验证**
- [ ] **Step 3: 同步统计口径、导出口径、审计口径文档**
