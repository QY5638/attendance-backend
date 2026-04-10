package com.quyong.attendance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.model.gateway.dto.ModelInvokeRequest;
import com.quyong.attendance.module.model.gateway.dto.ModelInvokeResponse;
import com.quyong.attendance.module.model.gateway.service.ModelGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.llm.provider=qwen",
        "app.llm.model=qwen-plus"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ExceptionControllerTest {

    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ModelGateway modelGateway;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM reviewRecord");
        jdbcTemplate.execute("DELETE FROM decisionTrace");
        jdbcTemplate.execute("DELETE FROM modelCallLog");
        jdbcTemplate.execute("DELETE FROM exceptionAnalysis");
        jdbcTemplate.execute("DELETE FROM attendanceException");
        jdbcTemplate.execute("DELETE FROM attendanceRepair");
        jdbcTemplate.execute("DELETE FROM faceFeature");
        jdbcTemplate.execute("DELETE FROM attendanceRecord");
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
        insertUser(1001L, "zhangsan", "张三", 1L, 2L, 1);
        insertUser(1002L, "lisi", "李四", 1L, 2L, 1);

        insertDevice("DEV-001", "前台考勤机1", "办公区A", 1, "默认正常设备");
        insertDevice("DEV-002", "前台考勤机2", "办公区B", 1, "默认正常设备");
        insertDevice("DEV-009", "临时设备", "外部区域", 1, "异常场景设备");
        insertRule(1L, "默认考勤规则", "09:00:00", "18:00:00", 10, 10, 3, 1);

        insertAttendanceRecord(2001L, 1001L, "2026-03-26 08:58:00", "IN", "DEV-001", "办公区A", 96.50, "NORMAL");
        insertAttendanceRecord(2004L, 1002L, "2026-03-26 09:16:00", "IN", "DEV-002", "办公区B", 95.20, "NORMAL");
    }

    @Test
    void shouldCreateLateExceptionByRuleCheck() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/exception/rule-check")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"recordId\":2004}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.type").value("LATE"))
                .andExpect(jsonPath("$.data.riskLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.data.sourceType").value("RULE"))
                .andExpect(jsonPath("$.data.processStatus").value("PENDING"));

        Integer exceptionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendanceException WHERE recordId = 2004 AND sourceType = 'RULE'",
                Integer.class
        );
        assertEquals(Integer.valueOf(1), exceptionCount);

        String recordStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM attendanceRecord WHERE id = 2004",
                String.class
        );
        assertEquals("ABNORMAL", recordStatus);
    }

    @Test
    void shouldReturnNullWhenRuleCheckMissesAnyException() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/exception/rule-check")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"recordId\":2001}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void shouldReuseExistingRuleExceptionForSameRecord() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/exception/rule-check")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"recordId\":2004}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/exception/rule-check")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"recordId\":2004}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendanceException WHERE recordId = 2004 AND sourceType = 'RULE'",
                Integer.class
        );
        assertEquals(Integer.valueOf(1), count);
    }

    @Test
    void shouldCreateProxyCheckinExceptionByComplexCheck() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");
        insertAttendanceRecord(2006L, 1002L, "2026-03-26 08:59:10", "IN", "DEV-009", "外部区域", 81.20, "NORMAL");
        insertPromptTemplate(8001L, "COMPLEX_EXCEPTION", "复杂异常分析模板", "EXCEPTION_ANALYSIS", "v1.0", "请基于输入摘要输出结构化分析结果", "ENABLED", "默认模板");
        when(modelGateway.invoke(any(ModelInvokeRequest.class))).thenReturn(mockResponse());

        MvcResult mvcResult = mockMvc.perform(post("/api/exception/complex-check")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"recordId\":2006,\"userId\":1002,\"riskFeatures\":{\"faceScore\":81.2,\"deviceChanged\":true,\"locationChanged\":true,\"historyAbnormalCount\":2}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.type").value("PROXY_CHECKIN"))
                .andExpect(jsonPath("$.data.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.data.sourceType").value("MODEL"))
                .andExpect(jsonPath("$.data.modelConclusion").value("PROXY_CHECKIN"))
                .andExpect(jsonPath("$.data.reasonSummary").value("电脑设备与地点异常共同提升风险"))
                .andExpect(jsonPath("$.data.actionSuggestion").value("建议优先人工复核"))
                .andExpect(jsonPath("$.data.confidenceScore").value(92.5))
                .andReturn();

        JsonNode response = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        long exceptionId = response.path("data").path("exceptionId").asLong();

        Integer traceCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM decisionTrace WHERE businessType = 'ATTENDANCE_EXCEPTION' AND businessId = ?",
                Integer.class,
                exceptionId
        );
        assertEquals(Integer.valueOf(1), traceCount);

        String inputSummary = jdbcTemplate.queryForObject(
                "SELECT inputSummary FROM modelCallLog WHERE businessType = 'EXCEPTION_ANALYSIS' AND businessId = ?",
                String.class,
                exceptionId
        );
        org.junit.jupiter.api.Assertions.assertTrue(inputSummary.contains("客户端电脑设备是否变化：是"));
        org.junit.jupiter.api.Assertions.assertTrue(inputSummary.contains("客户端打卡地点是否变化：是"));
    }

    @Test
    void shouldRecordQwenProviderInModelCallLogWhenComplexCheckSucceeds() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");
        insertAttendanceRecord(2006L, 1002L, "2026-03-26 08:59:10", "IN", "DEV-009", "外部区域", 81.20, "NORMAL");
        insertPromptTemplate(8001L, "COMPLEX_EXCEPTION", "复杂异常分析模板", "EXCEPTION_ANALYSIS", "v1.0", "请基于输入摘要输出结构化分析结果", "ENABLED", "默认模板");
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
        org.junit.jupiter.api.Assertions.assertTrue(inputSummary.contains("模型提供方：qwen"));
    }

    @Test
    void shouldRecordPromptTemplateContextInModelCallLogWhenComplexCheckSucceeds() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");
        String promptContent = "请重点关注电脑设备变化并输出结构化结论";
        String promptFingerprint = DigestUtils.md5DigestAsHex(promptContent.getBytes(StandardCharsets.UTF_8));
        insertAttendanceRecord(2006L, 1002L, "2026-03-26 08:59:10", "IN", "DEV-009", "外部区域", 81.20, "NORMAL");
        insertPromptTemplate(8001L, "COMPLEX_EXCEPTION", "复杂异常分析模板", "EXCEPTION_ANALYSIS", "v2.1", promptContent, "ENABLED", "模板已更新");
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
        org.junit.jupiter.api.Assertions.assertTrue(inputSummary.contains("提示词版本：v2.1"));
        org.junit.jupiter.api.Assertions.assertTrue(inputSummary.contains("提示词指纹：" + promptFingerprint));
    }

    @Test
    void shouldFallbackWhenModelGatewayFailsDuringComplexCheck() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");
        insertAttendanceRecord(2006L, 1002L, "2026-03-26 08:59:10", "IN", "DEV-009", "外部区域", 81.20, "NORMAL");
        insertPromptTemplate(8001L, "COMPLEX_EXCEPTION", "复杂异常分析模板", "EXCEPTION_ANALYSIS", "v1.0", "请基于输入摘要输出结构化分析结果", "ENABLED", "默认模板");
        when(modelGateway.invoke(any(ModelInvokeRequest.class))).thenThrow(new BusinessException(400, "外部模型调用失败"));

        MvcResult mvcResult = mockMvc.perform(post("/api/exception/complex-check")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"recordId\":2006,\"userId\":1002,\"riskFeatures\":{\"faceScore\":81.2,\"deviceChanged\":true,\"locationChanged\":true,\"historyAbnormalCount\":2}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sourceType").value("MODEL_FALLBACK"))
                .andExpect(jsonPath("$.data.processStatus").value("PENDING"))
                .andReturn();

        JsonNode response = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        long exceptionId = response.path("data").path("exceptionId").asLong();

        Integer failedLogCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM modelCallLog WHERE businessType = 'EXCEPTION_ANALYSIS' AND status = 'FAILED' AND errorMessage = '外部模型调用失败'",
                Integer.class
        );
        assertEquals(Integer.valueOf(1), failedLogCount);

        mockMvc.perform(get("/api/exception/" + exceptionId + "/analysis-brief")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.reasonSummary").value("模型调用失败，已转人工复核"));
    }

    @Test
    void shouldReuseExistingComplexExceptionForSameRecord() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");
        insertAttendanceRecord(2006L, 1002L, "2026-03-26 08:59:10", "IN", "DEV-009", "外部区域", 81.20, "NORMAL");
        insertPromptTemplate(8001L, "COMPLEX_EXCEPTION", "复杂异常分析模板", "EXCEPTION_ANALYSIS", "v1.0", "请基于输入摘要输出结构化分析结果", "ENABLED", "默认模板");
        when(modelGateway.invoke(any(ModelInvokeRequest.class))).thenReturn(mockResponse());

        mockMvc.perform(post("/api/exception/complex-check")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"recordId\":2006,\"userId\":1002,\"riskFeatures\":{\"faceScore\":81.2,\"deviceChanged\":true,\"locationChanged\":true,\"historyAbnormalCount\":2}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/exception/complex-check")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"recordId\":2006,\"userId\":1002,\"riskFeatures\":{\"faceScore\":81.2,\"deviceChanged\":true,\"locationChanged\":true,\"historyAbnormalCount\":2}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendanceException WHERE recordId = 2006 AND sourceType = 'MODEL'",
                Integer.class
        );
        assertEquals(Integer.valueOf(1), count);
    }

    @Test
    void shouldTrimLongModelFieldsDuringComplexCheck() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");
        insertAttendanceRecord(2006L, 1002L, "2026-03-26 08:59:10", "IN", "DEV-009", "外部区域", 81.20, "NORMAL");
        insertPromptTemplate(8001L, "COMPLEX_EXCEPTION", "复杂异常分析模板", "EXCEPTION_ANALYSIS", "v1.0", "请基于输入摘要输出结构化分析结果", "ENABLED", "默认模板");
        when(modelGateway.invoke(any(ModelInvokeRequest.class))).thenReturn(mockLongResponse());

        MvcResult mvcResult = mockMvc.perform(post("/api/exception/complex-check")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"recordId\":2006,\"userId\":1002,\"riskFeatures\":{\"faceScore\":81.2,\"deviceChanged\":true,\"locationChanged\":true,\"historyAbnormalCount\":2}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sourceType").value("MODEL"))
                .andReturn();

        JsonNode response = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        long exceptionId = response.path("data").path("exceptionId").asLong();

        String description = jdbcTemplate.queryForObject(
                "SELECT description FROM attendanceException WHERE id = ?",
                String.class,
                exceptionId
        );
        String suggestion = jdbcTemplate.queryForObject(
                "SELECT actionSuggestion FROM exceptionAnalysis WHERE exceptionId = ?",
                String.class,
                exceptionId
        );

        org.junit.jupiter.api.Assertions.assertTrue(description.length() <= 255);
        org.junit.jupiter.api.Assertions.assertTrue(suggestion.length() <= 255);
    }

    @Test
    void shouldReturnExceptionList() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");
        insertAttendanceException(3001L, 2004L, 1002L, "PROXY_CHECKIN", "HIGH", "MODEL", "疑似代打卡", "PENDING");

        mockMvc.perform(get("/api/exception/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(3001))
                .andExpect(jsonPath("$.data.records[0].type").value("PROXY_CHECKIN"));
    }

    @Test
    void shouldReturnExceptionDetail() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");
        insertAttendanceException(3001L, 2004L, 1002L, "PROXY_CHECKIN", "HIGH", "MODEL", "疑似代打卡", "PENDING");

        mockMvc.perform(get("/api/exception/3001")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(3001))
                .andExpect(jsonPath("$.data.type").value("PROXY_CHECKIN"));
    }

    @Test
    void shouldReturnDecisionTrace() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");
        insertAttendanceException(3001L, 2004L, 1002L, "PROXY_CHECKIN", "HIGH", "MODEL", "疑似代打卡", "PENDING");
        insertDecisionTrace(9501L, "ATTENDANCE_EXCEPTION", 3001L, "规则识别设备异常", "模型判定疑似代打卡", "最终进入高风险复核", new BigDecimal("92.50"), "规则与模型结论一致");

        mockMvc.perform(get("/api/exception/3001/decision-trace")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].businessType").value("ATTENDANCE_EXCEPTION"))
                .andExpect(jsonPath("$.data[0].finalDecision").value("最终进入高风险复核"));
    }

    @Test
    void shouldReturnAnalysisBrief() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");
        insertAttendanceException(3001L, 2004L, 1002L, "PROXY_CHECKIN", "HIGH", "MODEL", "疑似代打卡", "PENDING");
        insertExceptionAnalysis(4001L, 3001L, 8001L, "输入摘要", "{\"conclusion\":\"PROXY_CHECKIN\"}", "PROXY_CHECKIN", new BigDecimal("92.50"), "设备异常、地点异常且行为模式偏离历史规律", "建议优先人工复核", "设备与地点异常共同提升风险", "建议优先人工复核", "存在相似案例", "v1.0");

        mockMvc.perform(get("/api/exception/3001/analysis-brief")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.modelConclusion").value("PROXY_CHECKIN"))
                .andExpect(jsonPath("$.data.promptVersion").value("v1.0"));
    }

    @Test
    void shouldReturnUnauthorizedWhenAccessingExceptionApiWithoutToken() throws Exception {
        mockMvc.perform(get("/api/exception/list"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldReturnForbiddenWhenEmployeeAccessesExceptionApi() throws Exception {
        String employeeToken = loginAndExtractToken("lisi", "123456");

        mockMvc.perform(get("/api/exception/list")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
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

    private void insertRule(Long id,
                            String name,
                            String startTime,
                            String endTime,
                            int lateThreshold,
                            int earlyThreshold,
                            int repeatLimit,
                            int status) {
        jdbcTemplate.update(
                "INSERT INTO rule (id, name, startTime, endTime, lateThreshold, earlyThreshold, repeatLimit, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                id,
                name,
                startTime,
                endTime,
                lateThreshold,
                earlyThreshold,
                repeatLimit,
                status
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

    private void insertPromptTemplate(Long id,
                                      String code,
                                      String name,
                                      String sceneType,
                                      String version,
                                      String content,
                                      String status,
                                      String remark) {
        jdbcTemplate.update(
                "INSERT INTO promptTemplate (id, code, name, sceneType, version, content, status, remark, createTime, updateTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                id,
                code,
                name,
                sceneType,
                version,
                content,
                status,
                remark
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

    private void insertExceptionAnalysis(Long id,
                                         Long exceptionId,
                                         Long promptTemplateId,
                                         String inputSummary,
                                         String modelResult,
                                         String modelConclusion,
                                         BigDecimal confidenceScore,
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
                                     BigDecimal confidenceScore,
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

    private ModelInvokeResponse mockResponse() {
        ModelInvokeResponse response = new ModelInvokeResponse();
        response.setConclusion("PROXY_CHECKIN");
        response.setRiskLevel("HIGH");
        response.setConfidenceScore(new BigDecimal("92.50"));
        response.setDecisionReason("电脑设备异常、地点异常且行为模式偏离历史规律");
        response.setReasonSummary("电脑设备与地点异常共同提升风险");
        response.setActionSuggestion("建议优先人工复核");
        response.setSimilarCaseSummary("存在相似电脑设备异常与低分值组合案例");
        response.setRawResponse("{\"conclusion\":\"PROXY_CHECKIN\"}");
        return response;
    }

    private ModelInvokeResponse mockLongResponse() {
        ModelInvokeResponse response = mockResponse();
        response.setDecisionReason(repeatText("超长判定依据", 40));
        response.setActionSuggestion(repeatText("超长处理建议", 40));
        return response;
    }

    private String repeatText(String text, int times) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < times; index++) {
            builder.append(text);
        }
        return builder.toString();
    }

    private String loginAndExtractToken(String username, String password) throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        return response.path("data").path("token").asText();
    }
}
