# BE-01 基础分支回归验证设计

## 1. 目标

在不扩大功能范围、不主动修改实现的前提下，为 `feature/be-01-user-auth-foundation` 补一次更高强度的回归验证，确认认证、部门管理、用户管理三条主线在同一 worktree 中一起仍然稳定。

## 2. 范围

- 纳入范围：`AuthSecurityIntegrationTest`
- 纳入范围：`DepartmentManagementIntegrationTest`
- 纳入范围：`UserManagementIntegrationTest`
- 视第一轮结果决定是否继续执行 `mvn test`
- 不纳入范围：新功能开发、无关重构、数据库结构改动、提交/推送/PR、worktree 清理

## 3. 验证方案

### 3.1 第一层验证

先运行三组核心集成测试：

```bash
mvn "-Dtest=UserManagementIntegrationTest,AuthSecurityIntegrationTest,DepartmentManagementIntegrationTest" test
```

通过标准：

- Maven 退出码为 0
- `Failures: 0`
- `Errors: 0`
- 三个测试类均执行完成

### 3.2 第二层验证

如果第一层验证全部通过，再决定是否提升到：

```bash
mvn test
```

第二层验证的目的不是新增范围，而是补足“当前分支整体可运行性”的证据强度。

## 4. 失败处理

- 若第一层或第二层验证失败，先停止继续扩展验证。
- 使用 `systematic-debugging` 对失败点做最小定位，不直接盲目改代码。
- 在定位出明确原因后，再单独提出最小修复设计，等待确认。

## 5. 交付物

- 一次核心三件套回归验证结果
- 如条件允许，再补一次 `mvn test` 的全量验证结果
- 一份剩余风险与下一步建议总结
