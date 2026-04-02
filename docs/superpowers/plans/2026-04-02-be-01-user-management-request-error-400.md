# BE-01 用户管理请求解析异常 400 语义 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复用户管理接口在收到畸形 JSON 时返回 `500` 的问题，使其统一返回业务 `code=400`。

**Architecture:** 先用集成测试锁定 `POST /api/user/add` 的请求解析失败场景，再在 `GlobalExceptionHandler` 中增加对 `HttpMessageNotReadableException` 的最小映射。成功返回结构、业务校验流程和鉴权链路保持不变。

**Tech Stack:** Spring Boot 2.7、Spring MVC、Spring Security、JUnit 5、MockMvc

> 当前用户要求：本计划执行阶段不包含 `git commit`、`git push`、创建 PR 或其他额外 Git 动作。

---

## 文件结构与职责

- `src/test/java/com/quyong/attendance/UserManagementIntegrationTest.java`
  - 增加畸形 JSON 的 RED/GREEN 集成测试，验证用户新增接口的输入错误语义。
- `src/main/java/com/quyong/attendance/common/exception/GlobalExceptionHandler.java`
  - 统一收口请求体解析异常，返回业务 `code=400`。
- `docs/test/测试用例文档.md`
  - 补充本轮新增的异常输入验证范围。

### Task 1: 锁定畸形 JSON 当前失败行为

**Files:**
- Modify: `src/test/java/com/quyong/attendance/UserManagementIntegrationTest.java`

- [ ] **Step 1: 写出新增用户接口的 RED 测试**

在 `shouldFailAddUserWhenStatusIsInvalid()` 后新增：

```java
    @Test
    void shouldFailAddUserWhenRequestJsonIsMalformed() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/user/add")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"david\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请求参数错误"));
    }
```

- [ ] **Step 2: 运行 RED 测试，确认当前行为不符合预期**

Run: `mvn "-Dtest=UserManagementIntegrationTest#shouldFailAddUserWhenRequestJsonIsMalformed" test`

Expected: FAIL，当前会返回 `500` 或断言到错误的业务码。

### Task 2: 最小实现 400 语义

**Files:**
- Modify: `src/main/java/com/quyong/attendance/common/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: 为请求体解析异常增加最小处理分支**

把 `GlobalExceptionHandler` 调整为：

```java
package com.quyong.attendance.common.exception;

import com.quyong.attendance.common.api.Result;
import com.quyong.attendance.common.api.ResultCode;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Object> handleBusinessException(BusinessException exception) {
        return new Result<Object>(exception.getCode(), exception.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Object> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getAllErrors().isEmpty()
                ? ResultCode.BAD_REQUEST.getMessage()
                : exception.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return new Result<Object>(ResultCode.BAD_REQUEST.getCode(), message, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Object> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
        return Result.failure(ResultCode.BAD_REQUEST, null);
    }

    @ExceptionHandler(Exception.class)
    public Result<Object> handleException(Exception exception) {
        return Result.failure(ResultCode.SERVER_ERROR, null);
    }
}
```

- [ ] **Step 2: 运行单测，确认 RED 用例转绿**

Run: `mvn "-Dtest=UserManagementIntegrationTest#shouldFailAddUserWhenRequestJsonIsMalformed" test`

Expected: PASS

### Task 3: 文档与最小回归

**Files:**
- Modify: `docs/test/测试用例文档.md`
- Test: `src/test/java/com/quyong/attendance/UserManagementIntegrationTest.java`

- [ ] **Step 1: 同步测试文档中的异常输入验证范围**

在接口测试或安全测试中补充“用户新增接口畸形 JSON 返回请求参数错误”的描述，保持与本轮验证一致。

- [ ] **Step 2: 运行最小相关回归**

Run: `mvn "-Dtest=UserManagementIntegrationTest,AuthSecurityIntegrationTest" test`

Expected: PASS，既有用户管理与鉴权回归不受影响。
