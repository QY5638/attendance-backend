# BE-01 用户更新接口畸形 JSON 回归 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 `PUT /api/user/update` 补齐畸形 JSON 输入回归，并同步测试文档。

**Architecture:** 复用现有 `GlobalExceptionHandler` 中对 `HttpMessageNotReadableException` 的全局处理，不新增生产逻辑。通过用户管理集成测试锁定 `update` 接口在请求体无法解析时的统一错误语义，再把同一场景补入测试文档并做最小回归。

**Tech Stack:** Spring Boot 2.7、Spring MVC、JUnit 5、MockMvc、Maven

> 当前用户要求：本计划执行阶段不包含 `git commit`、`git push`、创建 PR 或其他额外 Git 动作。

---

## 文件结构与职责

- `src/test/java/com/quyong/attendance/UserManagementIntegrationTest.java`
  - 新增 `PUT /api/user/update` 的畸形 JSON 回归测试，验证统一异常语义。
- `docs/test/测试用例文档.md`
  - 新增 `API014A`，记录用户更新接口畸形 JSON 的接口测试用例。

### Task 1: 补用户更新接口畸形 JSON 回归

**Files:**
- Modify: `src/test/java/com/quyong/attendance/UserManagementIntegrationTest.java`

- [ ] **Step 1: 先写新的集成测试**

在 `shouldFailUpdateUserWhenStatusIsInvalid()` 后新增以下测试：

```java
    @Test
    void shouldFailUpdateUserWhenRequestJsonIsMalformed() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(put("/api/user/update")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请求参数错误"));
    }
```

- [ ] **Step 2: 先只运行新增测试观察当前行为**

Run: `mvn "-Dtest=UserManagementIntegrationTest#shouldFailUpdateUserWhenRequestJsonIsMalformed" test`

Expected: PASS。如果失败，记录实际响应并在进入下一步前确认是否出现了与 `add` 不一致的异常链路。

- [ ] **Step 3: 如测试失败，再做最小实现；如已通过，则不改生产代码**

当前设计预期是无需修改生产代码，因为 `src/main/java/com/quyong/attendance/common/exception/GlobalExceptionHandler.java` 已具备：

```java
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Object> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
        return new Result<Object>(ResultCode.BAD_REQUEST.getCode(), "请求参数错误", null);
    }
```

如果 Step 2 已 PASS，本步骤不做代码变更，直接进入回归验证。

- [ ] **Step 4: 再次运行新增测试，确认最终结果**

Run: `mvn "-Dtest=UserManagementIntegrationTest#shouldFailUpdateUserWhenRequestJsonIsMalformed" test`

Expected: PASS

### Task 2: 同步测试文档并执行最小回归

**Files:**
- Modify: `docs/test/测试用例文档.md`

- [ ] **Step 1: 在接口测试表中补充 API014A**

把接口测试用例区段更新为：

```md
| API013 | `POST /api/user/add` | 新增员工 | 合法员工参数 | 返回新增员工信息 |
| API013A | `POST /api/user/add` | 用户新增接口畸形 JSON 校验 | 畸形 JSON 请求体 | 返回请求参数错误 |
| API014 | `PUT /api/user/update` | 修改员工 | 合法员工参数 | 返回更新后的员工信息 |
| API014A | `PUT /api/user/update` | 用户修改接口畸形 JSON 校验 | 畸形 JSON 请求体 | 返回请求参数错误 |
| API015 | `DELETE /api/user/{id}` | 删除员工 | 合法员工ID | 返回删除成功 |
```

- [ ] **Step 2: 运行最小相关回归**

Run: `mvn "-Dtest=UserManagementIntegrationTest,AuthSecurityIntegrationTest" test`

Expected: PASS，用户管理与鉴权相关测试全部通过。
