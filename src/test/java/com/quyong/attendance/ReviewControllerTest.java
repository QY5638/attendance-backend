package com.quyong.attendance;

import com.quyong.attendance.module.exceptiondetect.entity.AttendanceException;
import com.quyong.attendance.module.exceptiondetect.mapper.AttendanceExceptionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ReviewControllerTest {

    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @SpyBean
    private AttendanceExceptionMapper attendanceExceptionMapper;

    @BeforeEach
    void setUp() {
        reset(attendanceExceptionMapper);
        jdbcTemplate.execute("DELETE FROM operationLog");
        jdbcTemplate.execute("DELETE FROM reviewRecord");
        jdbcTemplate.execute("DELETE FROM warningRecord");
        jdbcTemplate.execute("DELETE FROM decisionTrace");
        jdbcTemplate.execute("DELETE FROM modelCallLog");
        jdbcTemplate.execute("DELETE FROM exceptionAnalysis");
        jdbcTemplate.execute("DELETE FROM attendanceException");
        jdbcTemplate.execute("DELETE FROM attendanceRepair");
        jdbcTemplate.execute("DELETE FROM faceFeature");
        jdbcTemplate.execute("DELETE FROM attendanceRecord");
        jdbcTemplate.execute("DELETE FROM exceptionType");
        jdbcTemplate.execute("DELETE FROM promptTemplate");
        jdbcTemplate.execute("DELETE FROM rule");
        jdbcTemplate.execute("DELETE FROM device");
        jdbcTemplate.execute("DELETE FROM user");
        jdbcTemplate.execute("DELETE FROM department");
        jdbcTemplate.execute("DELETE FROM role");

        insertRole(1L, "ADMIN", "管理员");
        insertRole(2L, "EMPLOYEE", "员工");
        insertDepartment(1L, "技术部", "负责系统研发");
        insertUser(9001L, "admin", "系统管理员", 1L, 1L, 1);
        insertUser(1002L, "lisi", "李四", 1L, 2L, 1);
        insertDevice("DEV-009", "临时设备", "外部区域", 1, "异常场景设备");
        insertAttendanceRecord(2003L, 1002L, "2026-03-26 08:58:30", "IN", "DEV-009", "外部区域", 82.40, "ABNORMAL");
        insertAttendanceException(3001L, 2003L, 1002L, "PROXY_CHECKIN", "HIGH", "MODEL", "疑似代打卡", "PENDING");
    }

    @Test
    void shouldReturnLatestReviewRecordByExceptionId() throws Exception {
        insertReviewRecord(6001L, 3001L, 9001L, "CONFIRMED", "第一次复核", "建议优先人工确认", "存在相似案例", null, null, "2026-03-26 09:10:00");
        insertReviewRecord(6002L, 3001L, 9001L, "REJECTED", "最新复核意见", "建议结合历史记录再确认", "最近一周存在同类场景", null, null, "2026-03-26 09:20:00");
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/review/3001")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(6002))
                .andExpect(jsonPath("$.data.exceptionId").value(3001))
                .andExpect(jsonPath("$.data.reviewResult").value("REJECTED"))
                .andExpect(jsonPath("$.data.reviewComment").value("最新复核意见"));
    }

    @Test
    void shouldNormalizeLegacyFeedbackTagWhenQueryingLatestReview() throws Exception {
        insertReviewRecord(6001L, 3001L, 9001L, "CONFIRMED", "第一次复核", "建议优先人工确认", "存在相似案例", "CONFIRMED_EFFECTIVE", "建议保留当前策略", "2026-03-26 09:10:00");
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/review/3001")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.feedbackTag").value("TRUE_POSITIVE"));
    }

    @Test
    void shouldReturnNullWhenReviewRecordMissing() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/review/3001")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void shouldReturnUnauthorizedWhenAccessingReviewApiWithoutToken() throws Exception {
        mockMvc.perform(get("/api/review/3001"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldReturnForbiddenWhenEmployeeAccessesReviewApi() throws Exception {
        String employeeToken = loginAndExtractToken("lisi", "123456");

        mockMvc.perform(get("/api/review/3001")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void shouldReturnAssistantInfoFromAnalysisAndDecisionTrace() throws Exception {
        insertExceptionAnalysis(4001L, 3001L, 8001L, "输入摘要", "{\"conclusion\":\"PROXY_CHECKIN\"}", "PROXY_CHECKIN", "92.50", "分析层判定依据", "建议优先人工复核", "设备与地点异常共同提升风险", "建议优先人工复核", "存在相似设备异常与低分值组合案例", "v1.0");
        insertDecisionTrace(9501L, "ATTENDANCE_EXCEPTION", 3001L, "规则识别设备异常", "模型判定疑似代打卡", "最终进入高风险复核", "92.50", "规则与模型结论一致，建议人工复核");
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/review/3001/assistant")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.aiReviewSuggestion").value("设备与地点异常共同提升风险；建议优先人工复核"))
                .andExpect(jsonPath("$.data.similarCaseSummary").value("存在相似设备异常与低分值组合案例"))
                .andExpect(jsonPath("$.data.decisionReason").value("规则与模型结论一致，建议人工复核"))
                .andExpect(jsonPath("$.data.confidenceScore").value(92.5));
    }

    @Test
    void shouldReturnAssistantInfoFromAnalysisWhenDecisionTraceMissing() throws Exception {
        insertExceptionAnalysis(4001L, 3001L, 8001L, "输入摘要", "{\"conclusion\":\"PROXY_CHECKIN\"}", "PROXY_CHECKIN", "88.80", "分析层回退判定依据", "建议结合历史记录确认", "模型识别到跨设备与临界分值组合风险", "建议优先人工核验近期设备使用情况", "存在同部门相似代打卡案例", "v1.0");
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/review/3001/assistant")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.aiReviewSuggestion").value("模型识别到跨设备与临界分值组合风险；建议优先人工核验近期设备使用情况"))
                .andExpect(jsonPath("$.data.similarCaseSummary").value("存在同部门相似代打卡案例"))
                .andExpect(jsonPath("$.data.decisionReason").value("分析层回退判定依据"))
                .andExpect(jsonPath("$.data.confidenceScore").value(88.8));
    }

    @Test
    void shouldReturnBadRequestWhenAssistantInfoMissing() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/review/3001/assistant")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("复核辅助信息不存在"));
    }

    @Test
    void shouldSubmitReviewAndUpdateExceptionStatus() throws Exception {
        insertExceptionAnalysis(4001L, 3001L, 8001L, "输入摘要", "{\"conclusion\":\"PROXY_CHECKIN\"}", "PROXY_CHECKIN", "92.50", "分析层判定依据", "建议优先人工复核", "设备与地点异常共同提升风险", "建议优先人工复核", "存在相似设备异常与低分值组合案例", "v1.0");
        insertDecisionTrace(9501L, "ATTENDANCE_EXCEPTION", 3001L, "规则识别设备异常", "模型判定疑似代打卡", "最终进入高风险复核", "92.50", "规则与模型结论一致，建议人工复核");
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/review/submit")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"exceptionId\":3001,\"reviewResult\":\"CONFIRMED\",\"reviewComment\":\"人工确认异常\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.exceptionId").value(3001))
                .andExpect(jsonPath("$.data.reviewUserId").value(9001))
                .andExpect(jsonPath("$.data.reviewResult").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.reviewComment").value("人工确认异常"))
                .andExpect(jsonPath("$.data.aiReviewSuggestion").value("设备与地点异常共同提升风险；建议优先人工复核"));

        Integer reviewCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reviewRecord WHERE exceptionId = 3001",
                Integer.class
        );
        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(1), reviewCount);

        String processStatus = jdbcTemplate.queryForObject(
                "SELECT processStatus FROM attendanceException WHERE id = 3001",
                String.class
        );
        org.junit.jupiter.api.Assertions.assertEquals("REVIEWED", processStatus);

        Integer reviewLogCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM operationLog WHERE userId = ? AND type = 'REVIEW'",
                Integer.class,
                9001L
        );
        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(1), reviewLogCount);
    }

    @Test
    void shouldSubmitReviewAndMarkRelatedWarningProcessed() throws Exception {
        insertExceptionAnalysis(4001L, 3001L, 8001L, "输入摘要", "{\"conclusion\":\"PROXY_CHECKIN\"}", "PROXY_CHECKIN", "92.50", "分析层判定依据", "建议优先人工复核", "设备与地点异常共同提升风险", "建议优先人工复核", "存在相似设备异常与低分值组合案例", "v1.0");
        insertDecisionTrace(9501L, "ATTENDANCE_EXCEPTION", 3001L, "规则识别设备异常", "模型判定疑似代打卡", "最终进入高风险复核", "92.50", "规则与模型结论一致，建议人工复核");
        insertWarningRecord(5001L, 3001L, "RISK_WARNING", "HIGH", "UNPROCESSED", "96.00", "高风险摘要", "立即处理", "MODEL_FUSION", "2026-03-26 09:06:00");
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/review/submit")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"exceptionId\":3001,\"reviewResult\":\"CONFIRMED\",\"reviewComment\":\"人工确认异常\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        String warningStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM warningRecord WHERE id = 5001",
                String.class
        );
        org.junit.jupiter.api.Assertions.assertEquals("PROCESSED", warningStatus);
    }

    @Test
    void shouldRejectReviewSubmitWhenExceptionMissing() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/review/submit")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"exceptionId\":9999,\"reviewResult\":\"CONFIRMED\",\"reviewComment\":\"人工确认异常\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("异常记录不存在"));
    }

    @Test
    void shouldBlockSubmitWhenAssistantInfoMissing() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/review/submit")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"exceptionId\":3001,\"reviewResult\":\"CONFIRMED\",\"reviewComment\":\"人工确认异常\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("复核辅助信息不存在"));

        Integer reviewCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reviewRecord WHERE exceptionId = 3001",
                Integer.class
        );
        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(0), reviewCount);
    }

    @Test
    void shouldSubmitRuleExceptionReviewWithoutAnalysis() throws Exception {
        insertAttendanceRecord(2004L, 1002L, "2026-03-26 09:16:00", "IN", "DEV-009", "外部区域", 95.20, "ABNORMAL");
        insertAttendanceException(3002L, 2004L, 1002L, "LATE", "MEDIUM", "RULE", "超过上班时间阈值，判定为迟到", "PENDING");
        insertDecisionTrace(9502L, "ATTENDANCE_EXCEPTION", 3002L, "超过上班时间阈值", null, "LATE", null, "规则已判定迟到，建议管理员人工确认");
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/review/submit")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"exceptionId\":3002,\"reviewResult\":\"CONFIRMED\",\"reviewComment\":\"确认迟到异常\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.exceptionId").value(3002))
                .andExpect(jsonPath("$.data.reviewResult").value("CONFIRMED"));

        String processStatus = jdbcTemplate.queryForObject(
                "SELECT processStatus FROM attendanceException WHERE id = 3002",
                String.class
        );
        org.junit.jupiter.api.Assertions.assertEquals("REVIEWED", processStatus);
    }

    @Test
    void shouldSubmitReviewWhenOnlyAnalysisExists() throws Exception {
        insertExceptionAnalysis(4001L, 3001L, 8001L, "输入摘要", "{\"conclusion\":\"PROXY_CHECKIN\"}", "PROXY_CHECKIN", "88.80", "分析层回退判定依据", "建议结合历史记录确认", "模型识别到跨设备与临界分值组合风险", "建议优先人工核验近期设备使用情况", "存在同部门相似代打卡案例", "v1.0");
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/review/submit")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"exceptionId\":3001,\"reviewResult\":\"CONFIRMED\",\"reviewComment\":\"仅依赖 analysis 也允许复核\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.exceptionId").value(3001))
                .andExpect(jsonPath("$.data.reviewResult").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.aiReviewSuggestion").value("模型识别到跨设备与临界分值组合风险；建议优先人工核验近期设备使用情况"));

        String processStatus = jdbcTemplate.queryForObject(
                "SELECT processStatus FROM attendanceException WHERE id = 3001",
                String.class
        );
        org.junit.jupiter.api.Assertions.assertEquals("REVIEWED", processStatus);
    }

    @Test
    void shouldRollbackReviewSubmitWhenStatusUpdateFails() throws Exception {
        insertExceptionAnalysis(4001L, 3001L, 8001L, "输入摘要", "{\"conclusion\":\"PROXY_CHECKIN\"}", "PROXY_CHECKIN", "92.50", "分析层判定依据", "建议优先人工复核", "设备与地点异常共同提升风险", "建议优先人工复核", "存在相似设备异常与低分值组合案例", "v1.0");
        insertDecisionTrace(9501L, "ATTENDANCE_EXCEPTION", 3001L, "规则识别设备异常", "模型判定疑似代打卡", "最终进入高风险复核", "92.50", "规则与模型结论一致，建议人工复核");
        doThrow(new RuntimeException("mock update failure"))
                .when(attendanceExceptionMapper)
                .updateById((AttendanceException) argThat((AttendanceException entity) -> entity != null && Long.valueOf(3001L).equals(entity.getId())));
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/review/submit")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"exceptionId\":3001,\"reviewResult\":\"CONFIRMED\",\"reviewComment\":\"人工确认异常\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        Integer reviewCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reviewRecord WHERE exceptionId = 3001",
                Integer.class
        );
        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(0), reviewCount);

        String processStatus = jdbcTemplate.queryForObject(
                "SELECT processStatus FROM attendanceException WHERE id = 3001",
                String.class
        );
        org.junit.jupiter.api.Assertions.assertEquals("PENDING", processStatus);
    }

    @Test
    void shouldSaveFeedbackForExistingReview() throws Exception {
        insertReviewRecord(6001L, 3001L, 9001L, "CONFIRMED", "第一次复核", "建议优先人工确认", "存在相似案例", null, null, "2026-03-26 09:10:00");
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/review/feedback")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"reviewId\":6001,\"feedbackTag\":\"FALSE_POSITIVE\",\"strategyFeedback\":\"建议降低此类规则敏感度\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        String feedbackTag = jdbcTemplate.queryForObject(
                "SELECT feedbackTag FROM reviewRecord WHERE id = 6001",
                String.class
        );
        String strategyFeedback = jdbcTemplate.queryForObject(
                "SELECT strategyFeedback FROM reviewRecord WHERE id = 6001",
                String.class
        );
        org.junit.jupiter.api.Assertions.assertEquals("FALSE_POSITIVE", feedbackTag);
        org.junit.jupiter.api.Assertions.assertEquals("建议降低此类规则敏感度", strategyFeedback);

        Integer feedbackLogCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM operationLog WHERE userId = ? AND type = 'REVIEW'",
                Integer.class,
                9001L
        );
        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(1), feedbackLogCount);
    }

    @Test
    void shouldMapDeprecatedFeedbackTagToTruePositive() throws Exception {
        insertReviewRecord(6001L, 3001L, 9001L, "CONFIRMED", "第一次复核", "建议优先人工确认", "存在相似案例", null, null, "2026-03-26 09:10:00");
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/review/feedback")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"reviewId\":6001,\"feedbackTag\":\"CONFIRMED_EFFECTIVE\",\"strategyFeedback\":\"沿用兼容值\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        String feedbackTag = jdbcTemplate.queryForObject(
                "SELECT feedbackTag FROM reviewRecord WHERE id = 6001",
                String.class
        );
        org.junit.jupiter.api.Assertions.assertEquals("TRUE_POSITIVE", feedbackTag);
    }

    @Test
    void shouldReturnTruePositiveWhenStoredFeedbackTagUsesDeprecatedValue() throws Exception {
        insertReviewRecord(6001L, 3001L, 9001L, "CONFIRMED", "第一次复核", "建议优先人工确认", "存在相似案例", "CONFIRMED_EFFECTIVE", null, "2026-03-26 09:10:00");
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/review/3001")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(6001))
                .andExpect(jsonPath("$.data.feedbackTag").value("TRUE_POSITIVE"));
    }

    @Test
    void shouldRejectFeedbackWhenTagIsOutsideFrozenEnum() throws Exception {
        insertReviewRecord(6001L, 3001L, 9001L, "CONFIRMED", "第一次复核", "建议优先人工确认", "存在相似案例", null, null, "2026-03-26 09:10:00");
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/review/feedback")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"reviewId\":6001,\"feedbackTag\":\"UNKNOWN_TAG\",\"strategyFeedback\":\"不合法标签\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("反馈标签不合法"));
    }

    @Test
    void shouldAllowFeedbackWhenAssistantInfoMissingButLatestReviewExists() throws Exception {
        insertReviewRecord(6001L, 3001L, 9001L, "CONFIRMED", "第一次复核", null, null, null, null, "2026-03-26 09:10:00");
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/review/feedback")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"reviewId\":6001,\"feedbackTag\":\"NEEDS_TUNING\",\"strategyFeedback\":\"缺少 assistant 仍允许反馈\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        String feedbackTag = jdbcTemplate.queryForObject(
                "SELECT feedbackTag FROM reviewRecord WHERE id = 6001",
                String.class
        );
        org.junit.jupiter.api.Assertions.assertEquals("NEEDS_TUNING", feedbackTag);
    }

    @Test
    void shouldReturnAssistantMissingWhenNoAnalysisOrDecisionTrace() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/review/3001/assistant")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("复核辅助信息不存在"));
    }

    @Test
    void shouldNormalizeLegacyFeedbackTagWhenSavingFeedback() throws Exception {
        insertReviewRecord(6001L, 3001L, 9001L, "CONFIRMED", "第一次复核", "建议优先人工确认", "存在相似案例", null, null, "2026-03-26 09:10:00");
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/review/feedback")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"reviewId\":6001,\"feedbackTag\":\"CONFIRMED_EFFECTIVE\",\"strategyFeedback\":\"建议保留当前提示词模板\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        String feedbackTag = jdbcTemplate.queryForObject(
                "SELECT feedbackTag FROM reviewRecord WHERE id = 6001",
                String.class
        );
        org.junit.jupiter.api.Assertions.assertEquals("TRUE_POSITIVE", feedbackTag);
    }

    @Test
    void shouldRejectUnknownFeedbackTag() throws Exception {
        insertReviewRecord(6001L, 3001L, 9001L, "CONFIRMED", "第一次复核", "建议优先人工确认", "存在相似案例", null, null, "2026-03-26 09:10:00");
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/review/feedback")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"reviewId\":6001,\"feedbackTag\":\"UNKNOWN_TAG\",\"strategyFeedback\":\"建议调整\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("反馈标签不合法"));
    }

    @Test
    void shouldRejectStrategyFeedbackWithoutFeedbackTag() throws Exception {
        insertReviewRecord(6001L, 3001L, 9001L, "CONFIRMED", "第一次复核", "建议优先人工确认", "存在相似案例", null, null, "2026-03-26 09:10:00");
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/review/feedback")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"reviewId\":6001,\"strategyFeedback\":\"建议降低此类规则敏感度\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("反馈标签不能为空"));
    }

    @Test
    void shouldReturnExceptionTypeList() throws Exception {
        insertExceptionType(7001L, "PROXY_CHECKIN", "疑似代打卡", "疑似由他人代为打卡", 1, "2026-03-26 09:00:00", "2026-03-26 09:10:00");
        insertExceptionType(7002L, "MULTI_DEVICE", "多设备异常打卡", "短时间跨设备打卡", 0, "2026-03-26 09:00:00", "2026-03-26 09:05:00");
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/system/exception-type/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("status", "1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].code").value("PROXY_CHECKIN"));
    }

    @Test
    void shouldUpdateExceptionType() throws Exception {
        insertExceptionType(7001L, "PROXY_CHECKIN", "疑似代打卡", "疑似由他人代为打卡", 1, "2026-03-26 09:00:00", "2026-03-26 09:10:00");
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/system/exception-type/update")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"code\":\"PROXY_CHECKIN\",\"name\":\"代打卡异常\",\"description\":\"由管理员确认后的代打卡类型\",\"status\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        String name = jdbcTemplate.queryForObject(
                "SELECT name FROM exceptionType WHERE code = 'PROXY_CHECKIN'",
                String.class
        );
        Integer statusValue = jdbcTemplate.queryForObject(
                "SELECT status FROM exceptionType WHERE code = 'PROXY_CHECKIN'",
                Integer.class
        );
        org.junit.jupiter.api.Assertions.assertEquals("代打卡异常", name);
        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(0), statusValue);
    }

    private void insertRole(Long id, String code, String name) {
        jdbcTemplate.update(
                "INSERT INTO role (id, code, name, description, status) VALUES (?, ?, ?, ?, ?)",
                id,
                code,
                name,
                name + "角色",
                1
        );
    }

    private void insertDepartment(Long id, String name, String description) {
        jdbcTemplate.update(
                "INSERT INTO department (id, name, description) VALUES (?, ?, ?)",
                id,
                name,
                description
        );
    }

    private void insertUser(Long id, String username, String realName, Long deptId, Long roleId, int status) {
        jdbcTemplate.update(
                "INSERT INTO user (id, username, password, realName, gender, phone, deptId, roleId, status, createTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                id,
                username,
                PASSWORD_ENCODER.encode("123456"),
                realName,
                "男",
                "13800000000",
                deptId,
                roleId,
                status
        );
    }

    private void insertDevice(String id, String name, String location, int status, String description) {
        jdbcTemplate.update(
                "INSERT INTO device (id, name, location, status, description) VALUES (?, ?, ?, ?, ?)",
                id,
                name,
                location,
                status,
                description
        );
    }

    private void insertAttendanceRecord(Long id,
                                        Long userId,
                                        String checkTime,
                                        String checkType,
                                        String deviceId,
                                        String location,
                                        double faceScore,
                                        String status) {
        jdbcTemplate.update(
                "INSERT INTO attendanceRecord (id, userId, checkTime, checkType, deviceId, ipAddr, location, faceScore, status, createTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                id,
                userId,
                checkTime,
                checkType,
                deviceId,
                "127.0.0.1",
                location,
                faceScore,
                status
        );
    }

    private void insertAttendanceException(Long id,
                                           Long recordId,
                                           Long userId,
                                           String type,
                                           String riskLevel,
                                           String sourceType,
                                           String description,
                                           String processStatus) {
        jdbcTemplate.update(
                "INSERT INTO attendanceException (id, recordId, userId, type, riskLevel, sourceType, description, processStatus, createTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                id,
                recordId,
                userId,
                type,
                riskLevel,
                sourceType,
                description,
                processStatus
        );
    }

    private void insertReviewRecord(Long id,
                                    Long exceptionId,
                                    Long reviewUserId,
                                    String result,
                                    String comment,
                                    String aiReviewSuggestion,
                                    String similarCaseSummary,
                                    String feedbackTag,
                                    String strategyFeedback,
                                    String reviewTime) {
        jdbcTemplate.update(
                "INSERT INTO reviewRecord (id, exceptionId, reviewUserId, result, comment, aiReviewSuggestion, similarCaseSummary, feedbackTag, strategyFeedback, reviewTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id,
                exceptionId,
                reviewUserId,
                result,
                comment,
                aiReviewSuggestion,
                similarCaseSummary,
                feedbackTag,
                strategyFeedback,
                reviewTime
        );
    }

    private void insertExceptionAnalysis(Long id,
                                         Long exceptionId,
                                         Long promptTemplateId,
                                         String inputSummary,
                                         String modelResult,
                                         String modelConclusion,
                                         String confidenceScore,
                                         String decisionReason,
                                         String suggestion,
                                         String reasonSummary,
                                         String actionSuggestion,
                                         String similarCaseSummary,
                                         String promptVersion) {
        jdbcTemplate.update(
                "INSERT INTO exceptionAnalysis (id, exceptionId, promptTemplateId, inputSummary, modelResult, modelConclusion, confidenceScore, decisionReason, suggestion, reasonSummary, actionSuggestion, similarCaseSummary, promptVersion, createTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                id,
                exceptionId,
                promptTemplateId,
                inputSummary,
                modelResult,
                modelConclusion,
                confidenceScore,
                decisionReason,
                suggestion,
                reasonSummary,
                actionSuggestion,
                similarCaseSummary,
                promptVersion
        );
    }

    private void insertDecisionTrace(Long id,
                                     String businessType,
                                     Long businessId,
                                     String ruleResult,
                                     String modelResult,
                                     String finalDecision,
                                     String confidenceScore,
                                     String decisionReason) {
        jdbcTemplate.update(
                "INSERT INTO decisionTrace (id, businessType, businessId, ruleResult, modelResult, finalDecision, confidenceScore, decisionReason, createTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                id,
                businessType,
                businessId,
                ruleResult,
                modelResult,
                finalDecision,
                confidenceScore,
                decisionReason
        );
    }

    private void insertWarningRecord(Long id,
                                     Long exceptionId,
                                     String type,
                                     String level,
                                     String status,
                                     String priorityScore,
                                     String aiSummary,
                                     String disposeSuggestion,
                                     String decisionSource,
                                     String sendTime) {
        jdbcTemplate.update(
                "INSERT INTO warningRecord (id, exceptionId, type, level, status, priorityScore, aiSummary, disposeSuggestion, decisionSource, sendTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id,
                exceptionId,
                type,
                level,
                status,
                priorityScore,
                aiSummary,
                disposeSuggestion,
                decisionSource,
                sendTime
        );
    }

    private void insertExceptionType(Long id,
                                     String code,
                                     String name,
                                     String description,
                                     Integer status,
                                     String createTime,
                                     String updateTime) {
        jdbcTemplate.update(
                "INSERT INTO exceptionType (id, code, name, description, status, createTime, updateTime) VALUES (?, ?, ?, ?, ?, ?, ?)",
                id,
                code,
                name,
                description,
                status,
                createTime,
                updateTime
        );
    }

    private String loginAndExtractToken(String username, String password) throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        String content = mvcResult.getResponse().getContentAsString();
        int tokenStart = content.indexOf("\"token\":\"") + 9;
        int tokenEnd = content.indexOf('"', tokenStart);
        return content.substring(tokenStart, tokenEnd);
    }
}
