# Phase 4 Qwen 真实接入可观察性 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让异常智能分析成功调用真实 Qwen 时，`modelCallLog` 留下可直接观察的 `qwen` 接入标识。

**Architecture:** 保持现有异常分析、日志落库、决策追踪主链不变，只在成功日志内容中补一层最小 provider 观测信息。优先把改动集中在 `ExceptionAnalysisOrchestratorImpl` 与模型网关极小接口扩展上，避免扩散到控制器、前端和数据库。

**Tech Stack:** Spring Boot, MyBatis-Plus, JUnit 5, Mockito, Maven

---

### Task 1: 写失败测试锁定 Qwen 成功日志可观察性

**Files:**
- Modify: `src/test/java/com/quyong/attendance/ExceptionControllerTest.java`
- Test: `src/test/java/com/quyong/attendance/ExceptionControllerTest.java`

- [ ] **Step 1: 写失败测试**

```java
@Test
void shouldRecordQwenProviderInModelCallLogWhenComplexCheckSucceeds() throws Exception {
    // 复用现有 complexCheck 成功路径夹具，触发一次模型成功分析
    // 断言最新 modelCallLog.success 的 responseContent 或 inputSummary 中包含 qwen 标识
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -Dtest=ExceptionControllerTest#shouldRecordQwenProviderInModelCallLogWhenComplexCheckSucceeds test`
Expected: FAIL，原因是当前成功日志内容中还没有 `qwen` 标识。

- [ ] **Step 3: 写最小实现**

```java
String providerTag = resolveModelProviderTag();
modelCallLogService.logSuccess(
        "EXCEPTION_ANALYSIS",
        attendanceException.getId(),
        promptTemplate.getId(),
        enrichInputSummaryWithProvider(inputSummary, providerTag),
        response.getRawResponse(),
        Integer.valueOf((int) (System.currentTimeMillis() - startAt))
);
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -Dtest=ExceptionControllerTest#shouldRecordQwenProviderInModelCallLogWhenComplexCheckSucceeds test`
Expected: PASS

### Task 2: 回归最小网关与异常分析相关测试

**Files:**
- Modify: `src/main/java/com/quyong/attendance/module/exceptiondetect/service/impl/ExceptionAnalysisOrchestratorImpl.java`
- Modify: `src/main/java/com/quyong/attendance/module/model/gateway/service/ModelGateway.java`
- Modify: `src/main/java/com/quyong/attendance/module/model/gateway/service/QwenModelGateway.java`
- Modify: `src/main/java/com/quyong/attendance/module/model/gateway/service/MockModelGateway.java`
- Test: `src/test/java/com/quyong/attendance/ExceptionControllerTest.java`
- Test: `src/test/java/com/quyong/attendance/module/model/gateway/service/QwenModelGatewayTest.java`

- [ ] **Step 1: 保持接口最小兼容**

```java
public interface ModelGateway {
    ModelInvokeResponse invoke(ModelInvokeRequest request);

    default String provider() {
        return "unknown";
    }
}
```

- [ ] **Step 2: 为真实/Mock 网关返回 provider 标识**

```java
@Override
public String provider() {
    return "qwen";
}
```

```java
@Override
public String provider() {
    return "mock";
}
```

- [ ] **Step 3: 运行最小回归**

Run: `mvn -Dtest=ExceptionControllerTest#shouldRecordQwenProviderInModelCallLogWhenComplexCheckSucceeds,QwenModelGatewayTest test`
Expected: PASS
