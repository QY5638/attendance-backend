# Phase 4 提示词模板输入可观察性 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让复杂异常分析在模板内容变化后，`inputSummary` 与 `modelCallLog` 中都能保留稳定的模板上下文标识。

**Architecture:** 继续复用 `ExceptionAnalysisOrchestratorImpl` 作为复杂异常分析主入口，只在它构建 `inputSummary` 的地方追加 `promptVersion` 与 `promptFingerprint`。指纹基于模板内容生成稳定摘要，不扩散模板全文，不改控制器、数据库和前端。

**Tech Stack:** Spring Boot, MyBatis-Plus, JUnit 5, MockMvc, Maven

---

### Task 1: 锁定模板上下文必须进入复杂异常日志输入

**Files:**
- Modify: `src/test/java/com/quyong/attendance/ExceptionControllerTest.java`
- Test: `src/test/java/com/quyong/attendance/ExceptionControllerTest.java`

- [ ] **Step 1: 写失败测试**

```java
@Test
void shouldRecordPromptTemplateContextInModelCallLogWhenComplexCheckSucceeds() throws Exception {
    String adminToken = loginAndExtractToken("admin", "123456");
    insertAttendanceRecord(2006L, 1002L, "2026-03-26 08:59:10", "IN", "DEV-009", "外部区域", 81.20, "NORMAL");
    insertPromptTemplate(8001L, "COMPLEX_EXCEPTION", "复杂异常分析模板", "EXCEPTION_ANALYSIS", "v2.1", "请重点关注设备跳变并输出结构化结论", "ENABLED", "模板已更新");
    when(modelGateway.invoke(any(ModelInvokeRequest.class))).thenReturn(mockResponse());

    MvcResult mvcResult = mockMvc.perform(post("/api/exception/complex-check")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(APPLICATION_JSON)
                    .content("{\"recordId\":2006,\"userId\":1002,\"riskFeatures\":{\"faceScore\":81.2,\"deviceChanged\":true,\"locationChanged\":true,\"historyAbnormalCount\":2}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andReturn();

    JsonNode response = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
    long exceptionId = response.path("data").path("exceptionId").asLong();

    String inputSummary = jdbcTemplate.queryForObject(
            "SELECT inputSummary FROM modelCallLog WHERE businessType = 'EXCEPTION_ANALYSIS' AND businessId = ?",
            String.class,
            exceptionId
    );

    org.junit.jupiter.api.Assertions.assertTrue(inputSummary.contains("promptVersion=v2.1"));
    org.junit.jupiter.api.Assertions.assertTrue(inputSummary.contains("promptFingerprint="));
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -Dtest=ExceptionControllerTest#shouldRecordPromptTemplateContextInModelCallLogWhenComplexCheckSucceeds test`
Expected: FAIL，因为当前 `inputSummary` 还没有模板上下文字段。

### Task 2: 做最小实现并验证

**Files:**
- Modify: `src/main/java/com/quyong/attendance/module/exceptiondetect/service/impl/ExceptionAnalysisOrchestratorImpl.java`
- Modify: `src/test/java/com/quyong/attendance/ExceptionControllerTest.java`
- Test: `src/test/java/com/quyong/attendance/ExceptionControllerTest.java`

- [ ] **Step 1: 为模板内容生成稳定摘要并追加到输入摘要**

```java
String inputSummary = buildInputSummary(record, riskFeatures);
String enrichedInputSummary = enrichPromptContext(inputSummary, promptTemplate);
```

```java
private String enrichPromptContext(String inputSummary, PromptTemplate promptTemplate) {
    return inputSummary
            + ", promptVersion=" + promptTemplate.getVersion()
            + ", promptFingerprint=" + digestPromptContent(promptTemplate.getContent());
}
```

- [ ] **Step 2: 运行目标测试确认通过**

Run: `mvn -Dtest=ExceptionControllerTest#shouldRecordPromptTemplateContextInModelCallLogWhenComplexCheckSucceeds test`
Expected: PASS

- [ ] **Step 3: 运行最小相关回归**

Run: `mvn "-Dtest=ExceptionControllerTest#shouldCreateProxyCheckinExceptionByComplexCheck+shouldRecordQwenProviderInModelCallLogWhenComplexCheckSucceeds+shouldRecordPromptTemplateContextInModelCallLogWhenComplexCheckSucceeds+shouldFallbackWhenModelGatewayFailsDuringComplexCheck,QwenModelGatewayTest" test`
Expected: PASS，且 `Failures: 0`、`Errors: 0`
