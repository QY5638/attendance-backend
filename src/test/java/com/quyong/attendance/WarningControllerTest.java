package com.quyong.attendance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class WarningControllerTest {

    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
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
        jdbcTemplate.execute("DELETE FROM promptTemplate");
        jdbcTemplate.execute("DELETE FROM rule");
        jdbcTemplate.execute("DELETE FROM device");
        jdbcTemplate.execute("DELETE FROM user");
        jdbcTemplate.execute("DELETE FROM department");
        jdbcTemplate.execute("DELETE FROM role");
        jdbcTemplate.execute("DELETE FROM riskLevel");

        insertRiskLevel(1L, "HIGH", "高风险", "需要优先人工复核", 1, "2026-03-26 09:00:00", "2026-03-26 09:00:00");
        insertRiskLevel(2L, "MEDIUM", "中风险", "建议尽快关注并结合历史记录判断", 1, "2026-03-26 09:01:00", "2026-03-26 09:01:00");
        insertRiskLevel(3L, "LOW", "低风险", "记录留档并持续观察", 1, "2026-03-26 09:02:00", "2026-03-26 09:02:00");

        insertRole(1L, "ADMIN", "管理员");
        insertRole(2L, "EMPLOYEE", "员工");
        insertDepartment(1L, "技术部", "负责系统研发");
        insertUser(9001L, "admin", "系统管理员", 1L, 1L, 1);
        insertUser(1001L, "zhangsan", "张三", 1L, 2L, 1);
        insertDevice("DEV-001", "前台考勤机", "办公区A", 1, "默认正常设备");

        insertAttendanceRecord(2001L, 1001L, "2026-03-26 08:58:00", "IN", "DEV-001", "办公区A", 96.50, "NORMAL");
        insertAttendanceRecord(2002L, 1001L, "2026-03-26 09:16:00", "IN", "DEV-001", "办公区A", 82.40, "ABNORMAL");
        insertAttendanceRecord(2003L, 1001L, "2026-03-26 08:59:10", "IN", "DEV-001", "办公区A", 91.00, "ABNORMAL");
    }

    @Test
    void shouldGenerateWarningsForHighAndMediumExceptionsWhenListing() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");
        insertAttendanceException(3001L, 2001L, 1001L, "PROXY_CHECKIN", "HIGH", "MODEL", "疑似代打卡", "PENDING");
        insertExceptionAnalysis(4001L, 3001L, "PROXY_CHECKIN", new BigDecimal("92.50"), "设备与地点异常共同提升风险", "建议优先人工复核");
        insertAttendanceException(3002L, 2002L, 1001L, "LATE", "MEDIUM", "RULE", "超过上班时间阈值，判定为迟到", "PENDING");
        insertAttendanceException(3003L, 2003L, 1001L, "REPEAT_CHECK", "LOW", "RULE", "短时间内重复打卡", "PENDING");

        MvcResult mvcResult = mockMvc.perform(get("/api/warning/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].exceptionId").value(3001))
                .andExpect(jsonPath("$.data.records[1].exceptionId").value(3002))
                .andReturn();

        JsonNode root = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        assertEquals(2, root.path("data").path("total").asInt());
        assertEquals(2, root.path("data").path("records").size());

        Integer warningCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM warningRecord", Integer.class);
        assertEquals(Integer.valueOf(2), warningCount);
    }

    @Test
    void shouldFilterWarningListByStatusAndType() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");
        insertAttendanceException(3001L, 2001L, 1001L, "PROXY_CHECKIN", "HIGH", "MODEL", "疑似代打卡", "PENDING");
        insertAttendanceException(3002L, 2002L, 1001L, "LATE", "MEDIUM", "RULE", "超过上班时间阈值，判定为迟到", "PENDING");
        insertWarningRecord(5001L, 3001L, "RISK_WARNING", "HIGH", "PROCESSED", new BigDecimal("96.00"), "高风险摘要", "立即处理", "MODEL_FUSION", "2026-03-26 08:59:20");
        insertWarningRecord(5002L, 3002L, "ATTENDANCE_WARNING", "MEDIUM", "UNPROCESSED", new BigDecimal("68.00"), "中风险摘要", "持续关注", "RULE", "2026-03-26 09:16:20");

        mockMvc.perform(get("/api/warning/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("status", "UNPROCESSED")
                        .param("type", "ATTENDANCE_WARNING")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(5002))
                .andExpect(jsonPath("$.data.records[0].type").value("ATTENDANCE_WARNING"))
                .andExpect(jsonPath("$.data.records[0].status").value("UNPROCESSED"));
    }

    @Test
    void shouldExposeMultiLocationConflictTypeInWarningList() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");
        insertAttendanceException(3004L, 2002L, 1001L, "MULTI_LOCATION_CONFLICT", "HIGH", "RULE", "检测到短时间内跨地点打卡", "PENDING");
        insertWarningRecord(5004L, 3004L, "ATTENDANCE_WARNING", "HIGH", "UNPROCESSED", new BigDecimal("95.00"), "检测到短时间内跨地点打卡（高风险）", "建议优先人工复核", "RULE", "2026-03-26 09:20:00");

        mockMvc.perform(get("/api/warning/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].id").value(5004))
                .andExpect(jsonPath("$.data.records[0].exceptionType").value("MULTI_LOCATION_CONFLICT"));
    }

    @Test
    void shouldReturnWarningAdvice() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");
        insertAttendanceException(3001L, 2001L, 1001L, "PROXY_CHECKIN", "HIGH", "MODEL", "疑似代打卡", "PENDING");
        insertWarningRecord(5001L, 3001L, "RISK_WARNING", "HIGH", "UNPROCESSED", new BigDecimal("96.00"), "高风险摘要", "立即处理", "MODEL_FUSION", "2026-03-26 08:59:20");

        mockMvc.perform(get("/api/warning/5001/advice")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(5001))
                .andExpect(jsonPath("$.data.exceptionId").value(3001))
                .andExpect(jsonPath("$.data.priorityScore").value(96.0))
                .andExpect(jsonPath("$.data.aiSummary").value("高风险摘要"))
                .andExpect(jsonPath("$.data.disposeSuggestion").value("立即处理"))
                .andExpect(jsonPath("$.data.decisionSource").value("MODEL_FUSION"));
    }

    @Test
    void shouldExposeExceptionIdInWarningListForReviewEntry() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");
        insertAttendanceException(3005L, 2002L, 1001L, "PROXY_CHECKIN", "HIGH", "MODEL", "用于复核入口校验的异常", "PENDING");
        insertWarningRecord(5005L, 3005L, "RISK_WARNING", "HIGH", "UNPROCESSED", new BigDecimal("98.00"), "需要人工复核", "建议直接进入复核页", "MODEL_FUSION", "2026-03-26 09:30:00");

        mockMvc.perform(get("/api/warning/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(5005))
                .andExpect(jsonPath("$.data.records[0].exceptionId").value(3005));
    }

    @Test
    void shouldReevaluateWarningByLatestExceptionResult() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");
        insertAttendanceException(3001L, 2001L, 1001L, "PROXY_CHECKIN", "HIGH", "MODEL", "疑似代打卡", "PENDING");
        insertExceptionAnalysis(4001L, 3001L, "PROXY_CHECKIN", new BigDecimal("92.50"), "设备与地点异常共同提升风险", "建议优先人工复核");
        insertWarningRecord(5001L, 3001L, "ATTENDANCE_WARNING", "HIGH", "PROCESSED", new BigDecimal("60.00"), "旧摘要", "旧建议", "RULE", "2026-03-26 08:59:20");

        mockMvc.perform(post("/api/warning/re-evaluate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"warningId\":5001,\"reason\":\"规则已更新\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(5001))
                .andExpect(jsonPath("$.data.type").value("RISK_WARNING"))
                .andExpect(jsonPath("$.data.status").value("PROCESSED"))
                .andExpect(jsonPath("$.data.aiSummary").value("设备与地点异常共同提升风险"))
                .andExpect(jsonPath("$.data.disposeSuggestion").value("建议优先人工复核"));

        String processStatus = jdbcTemplate.queryForObject(
                "SELECT processStatus FROM attendanceException WHERE id = 3001",
                String.class
        );
        assertEquals("PENDING", processStatus);

        Integer warningLogCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM operationLog WHERE userId = ? AND type = 'WARNING'",
                Integer.class,
                9001L
        );
        assertEquals(Integer.valueOf(1), warningLogCount);
    }

    @Test
    void shouldReturnDefaultRiskLevelConfigs() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/system/risk-level/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.records[0].code").value("HIGH"))
                .andExpect(jsonPath("$.data.records[0].name").value("高风险"));
    }

    @Test
    void shouldUpdateRiskLevelConfig() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(put("/api/system/risk-level/update")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"code\":\"HIGH\",\"name\":\"重点风险\",\"description\":\"需优先关注\",\"status\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/system/risk-level/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("status", "0")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].code").value("HIGH"))
                .andExpect(jsonPath("$.data.records[0].name").value("重点风险"))
                .andExpect(jsonPath("$.data.records[0].description").value("需优先关注"));
    }

    @Test
    void shouldReadPersistedRiskLevelConfigFromDatabase() throws Exception {
        jdbcTemplate.execute("DELETE FROM riskLevel");
        insertRiskLevel(11L, "HIGH", "重点高风险", "数据库中的高风险名称", 1, "2026-03-26 10:00:00", "2026-03-26 10:00:00");
        insertRiskLevel(12L, "MEDIUM", "重点中风险", "数据库中的中风险名称", 1, "2026-03-26 10:01:00", "2026-03-26 10:01:00");
        insertRiskLevel(13L, "LOW", "重点低风险", "数据库中的低风险名称", 1, "2026-03-26 10:02:00", "2026-03-26 10:02:00");
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/system/risk-level/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.records[0].code").value("HIGH"))
                .andExpect(jsonPath("$.data.records[0].name").value("重点高风险"));
    }

    @Test
    void shouldReturnForbiddenWhenEmployeeAccessesWarningApi() throws Exception {
        String employeeToken = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(get("/api/warning/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void shouldReturnUnauthorizedWhenRequestHasNoToken() throws Exception {
        mockMvc.perform(get("/api/system/risk-level/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
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

    private void insertRiskLevel(Long id,
                                 String code,
                                 String name,
                                 String description,
                                 Integer status,
                                 String createTime,
                                 String updateTime) {
        jdbcTemplate.update(
                "INSERT INTO riskLevel (id, code, name, description, status, createTime, updateTime) VALUES (?, ?, ?, ?, ?, ?, ?)",
                id,
                code,
                name,
                description,
                status,
                createTime,
                updateTime
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
                                         String modelConclusion,
                                         BigDecimal confidenceScore,
                                         String reasonSummary,
                                         String actionSuggestion) {
        jdbcTemplate.update(
                "INSERT INTO exceptionAnalysis (id, exceptionId, promptTemplateId, inputSummary, modelResult, modelConclusion, confidenceScore, decisionReason, suggestion, reasonSummary, actionSuggestion, similarCaseSummary, promptVersion, createTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                id,
                exceptionId,
                null,
                "input-summary",
                "raw-result",
                modelConclusion,
                confidenceScore,
                reasonSummary,
                actionSuggestion,
                reasonSummary,
                actionSuggestion,
                "similar-case",
                "v1.0"
        );
    }

    private void insertWarningRecord(Long id,
                                     Long exceptionId,
                                     String type,
                                     String level,
                                     String status,
                                     BigDecimal priorityScore,
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

    private String loginAndExtractToken(String username, String password) throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password).getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        String token = response.path("data").path("token").asText();
        assertTrue(token != null && !token.isEmpty());
        return token;
    }
}
