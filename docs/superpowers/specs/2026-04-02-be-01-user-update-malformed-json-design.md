# BE-01 用户更新接口畸形 JSON 回归设计

## 1. 目标

为 `PUT /api/user/update` 补齐畸形 JSON 输入场景的回归验证，确认当前全局异常处理对用户更新接口同样生效：接口在请求体无法解析时保持 HTTP `200`，并返回统一业务失败结构 `code=400`、`message=请求参数错误`。

## 2. 范围

- 纳入范围：`PUT /api/user/update` 的畸形 JSON 集成测试。
- 纳入范围：`docs/test/测试用例文档.md` 中对应接口测试用例补充。
- 纳入范围：与本次新增用例直接相关的最小回归验证。
- 不纳入范围：生产代码改动、其他参数绑定异常扩展、鉴权规则调整、数据库结构改动。

## 3. 设计方案

### 3.1 接口行为

本轮不修改生产代码，直接复用当前 `GlobalExceptionHandler` 中针对 `HttpMessageNotReadableException` 的处理分支。

预期行为为：

- 请求：管理员携带有效 token 调用 `PUT /api/user/update`
- 请求头：`Content-Type: application/json`
- 请求体：畸形 JSON
- HTTP 状态：`200`
- 业务返回：`code=400`
- 业务返回：`message=请求参数错误`

### 3.2 测试策略

在 `src/test/java/com/quyong/attendance/UserManagementIntegrationTest.java` 中新增一个独立场景，方法名保持与现有风格一致：`shouldFailUpdateUserWhenRequestJsonIsMalformed`。

测试约束：

- 只验证一个行为：用户更新接口收到畸形 JSON 时的返回语义。
- 使用管理员 token，避免把失败原因混入鉴权问题。
- 断言与现有失败用例风格保持一致，同时校验 `code` 与 `message`。

由于生产代码理论上已覆盖该异常类型，这个测试预计会直接通过；如果未通过，再根据实际异常链路判断是否存在接口级特例。

### 3.3 文档同步

在 `docs/test/测试用例文档.md` 中，紧邻 `API014` 增加一条 `API014A`，描述 `PUT /api/user/update` 的畸形 JSON 输入返回请求参数错误。

文案保持与现有 `API013A` 一致的表达风格，避免文档口径不统一。

### 3.4 验证范围

执行两层验证：

1. 最小验证：仅运行新增测试，确认该场景通过。
2. 回归验证：运行 `UserManagementIntegrationTest` 与 `AuthSecurityIntegrationTest`，确认用户管理和鉴权链路未被影响。

## 4. 风险与取舍

- 本轮只补 `update` 的畸形 JSON 回归，不继续扩展到字段类型错误、空 body 等更多请求解析变体，目的是保持最小必要改动。
- 本轮也不把部门等其他模块一并纳入同类回归，避免扩大范围；如果后续需要，可以按同样模式继续补充。

## 5. 交付物

- 新增 `shouldFailUpdateUserWhenRequestJsonIsMalformed` 集成测试。
- `docs/test/测试用例文档.md` 中新增 `API014A`。
- 最小验证与相关回归通过。
