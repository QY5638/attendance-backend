# Phase 4 复核反馈决策追踪 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让复核反馈在提交成功后沉淀为同一异常下新增的一条 `decisionTrace`，并且不污染复核辅助信息读取结果。

**Architecture:** 保持现有 `reviewRecord` 持久化逻辑不变，只在 `ReviewServiceImpl.feedback()` 成功后追加一条反馈型 trace。`getAssistant()` 通过跳过 `REVIEW_FEEDBACK` trace，继续返回原始规则/模型判因。当前会话不做 commit，除非用户另行要求。

**Tech Stack:** Spring Boot, MyBatis-Plus, JUnit 5, MockMvc, Maven

---

### Task 1: 锁定反馈提交后新增治理 trace

**Files:**
- Modify: `src/test/java/com/quyong/attendance/ReviewControllerTest.java`
- Test: `src/test/java/com/quyong/attendance/ReviewControllerTest.java`

- [ ] **Step 1: 写失败测试**

```java
@Test
void shouldAppendDecisionTraceWhenSavingReviewFeedback() throws Exception {
    insertDecisionTrace(9501L, "ATTENDANCE_EXCEPTION", 3001L, "规则识别设备异常", "模型判定疑似代打卡", "最终进入高风险复核", "92.50", "规则与模型结论一致，建议人工复核");
    insertReviewRecord(6001L, 3001L, 9001L, "CONFIRMED", "第一次复核", "建议优先人工确认", "存在相似案例", null, null, "2026-03-26 09:10:00");
    String adminToken = loginAndExtractToken("admin", "123456");

    mockMvc.perform(post("/api/review/feedback")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(APPLICATION_JSON)
                    .content("{\"reviewId\":6001,\"feedbackTag\":\"FALSE_POSITIVE\",\"strategyFeedback\":\"建议降低此类规则敏感度\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

    Integer traceCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM decisionTrace WHERE businessType = 'ATTENDANCE_EXCEPTION' AND businessId = ?",
            Integer.class,
            3001L
    );
    String finalDecision = jdbcTemplate.queryForObject(
            "SELECT finalDecision FROM decisionTrace WHERE businessType = 'ATTENDANCE_EXCEPTION' AND businessId = ? ORDER BY id DESC LIMIT 1",
            String.class,
            3001L
    );
    String ruleResult = jdbcTemplate.queryForObject(
            "SELECT ruleResult FROM decisionTrace WHERE businessType = 'ATTENDANCE_EXCEPTION' AND businessId = ? ORDER BY id DESC LIMIT 1",
            String.class,
            3001L
    );
    String decisionReason = jdbcTemplate.queryForObject(
            "SELECT decisionReason FROM decisionTrace WHERE businessType = 'ATTENDANCE_EXCEPTION' AND businessId = ? ORDER BY id DESC LIMIT 1",
            String.class,
            3001L
    );

    org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(2), traceCount);
    org.junit.jupiter.api.Assertions.assertEquals("REVIEW_FEEDBACK", finalDecision);
    org.junit.jupiter.api.Assertions.assertTrue(ruleResult.contains("reviewId=6001"));
    org.junit.jupiter.api.Assertions.assertTrue(ruleResult.contains("feedbackTag=FALSE_POSITIVE"));
    org.junit.jupiter.api.Assertions.assertTrue(decisionReason.contains("strategyFeedback=建议降低此类规则敏感度"));
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -Dtest=ReviewControllerTest#shouldAppendDecisionTraceWhenSavingReviewFeedback test`
Expected: FAIL，因为当前反馈提交还不会新增 `decisionTrace`。

### Task 2: 锁定 assistant 不被反馈 trace 污染

**Files:**
- Modify: `src/test/java/com/quyong/attendance/ReviewControllerTest.java`
- Test: `src/test/java/com/quyong/attendance/ReviewControllerTest.java`

- [ ] **Step 1: 写失败测试**

```java
@Test
void shouldKeepAssistantDecisionReasonAfterFeedbackTraceIsAppended() throws Exception {
    insertExceptionAnalysis(4001L, 3001L, 8001L, "输入摘要", "{\"conclusion\":\"PROXY_CHECKIN\"}", "PROXY_CHECKIN", "92.50", "分析层判定依据", "建议优先人工复核", "设备与地点异常共同提升风险", "建议优先人工复核", "存在相似设备异常与低分值组合案例", "v1.0");
    insertDecisionTrace(9501L, "ATTENDANCE_EXCEPTION", 3001L, "规则识别设备异常", "模型判定疑似代打卡", "最终进入高风险复核", "92.50", "规则与模型结论一致，建议人工复核");
    insertReviewRecord(6001L, 3001L, 9001L, "CONFIRMED", "第一次复核", "建议优先人工确认", "存在相似案例", null, null, "2026-03-26 09:10:00");
    String adminToken = loginAndExtractToken("admin", "123456");

    mockMvc.perform(post("/api/review/feedback")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(APPLICATION_JSON)
                    .content("{\"reviewId\":6001,\"feedbackTag\":\"NEEDS_TUNING\",\"strategyFeedback\":\"建议微调提示词\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

    mockMvc.perform(get("/api/review/3001/assistant")
                    .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.decisionReason").value("规则与模型结论一致，建议人工复核"))
            .andExpect(jsonPath("$.data.confidenceScore").value(92.5));
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -Dtest=ReviewControllerTest#shouldKeepAssistantDecisionReasonAfterFeedbackTraceIsAppended test`
Expected: FAIL，因为新增反馈 trace 后当前 `getAssistant()` 会直接读取最后一条 trace。

### Task 3: 做最小实现并验证

**Files:**
- Modify: `src/main/java/com/quyong/attendance/module/review/service/impl/ReviewServiceImpl.java`
- Modify: `src/test/java/com/quyong/attendance/ReviewControllerTest.java`
- Test: `src/test/java/com/quyong/attendance/ReviewControllerTest.java`

- [ ] **Step 1: 在 `feedback()` 中新增反馈 trace**

```java
private static final String ATTENDANCE_EXCEPTION_BUSINESS_TYPE = "ATTENDANCE_EXCEPTION";
private static final String REVIEW_FEEDBACK = "REVIEW_FEEDBACK";

decisionTraceService.save(
        ATTENDANCE_EXCEPTION_BUSINESS_TYPE,
        reviewRecord.getExceptionId(),
        "reviewId=" + reviewRecord.getId() + ", feedbackTag=" + validatedDTO.getFeedbackTag(),
        null,
        REVIEW_FEEDBACK,
        null,
        buildFeedbackDecisionReason(validatedDTO.getStrategyFeedback())
);
```

- [ ] **Step 2: 让 `getAssistant()` 跳过反馈 trace**

```java
DecisionTraceVO trace = findLatestDecisionTrace(traces);
if (trace == null) {
    if (analysis == null) {
        throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "复核辅助信息不存在");
    }
    vo.setDecisionReason(analysis.getDecisionReason());
    vo.setConfidenceScore(analysis.getConfidenceScore());
    return vo;
}
```

```java
private DecisionTraceVO findLatestDecisionTrace(List<DecisionTraceVO> traces) {
    for (int index = traces.size() - 1; index >= 0; index--) {
        DecisionTraceVO trace = traces.get(index);
        if (!REVIEW_FEEDBACK.equals(trace.getFinalDecision())) {
            return trace;
        }
    }
    return null;
}
```

- [ ] **Step 3: 运行目标测试确认通过**

Run: `mvn "-Dtest=ReviewControllerTest#shouldAppendDecisionTraceWhenSavingReviewFeedback+shouldKeepAssistantDecisionReasonAfterFeedbackTraceIsAppended" test`
Expected: PASS

- [ ] **Step 4: 运行最小相关回归**

Run: `mvn "-Dtest=ReviewControllerTest#shouldSaveFeedbackForExistingReview+shouldMapDeprecatedFeedbackTagToTruePositive+shouldAllowFeedbackWhenAssistantInfoMissingButLatestReviewExists+shouldAppendDecisionTraceWhenSavingReviewFeedback+shouldKeepAssistantDecisionReasonAfterFeedbackTraceIsAppended" test`
Expected: PASS，且 `Failures: 0`、`Errors: 0`
