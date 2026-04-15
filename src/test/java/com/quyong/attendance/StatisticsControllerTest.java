package com.quyong.attendance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class StatisticsControllerTest {

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
        jdbcTemplate.execute("DELETE FROM notificationRecord");
        jdbcTemplate.execute("DELETE FROM warningInteractionRecord");
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

        insertRole(1L, "ADMIN", "管理员");
        insertRole(2L, "EMPLOYEE", "员工");

        insertDepartment(1L, "技术部", "负责系统研发");
        insertDepartment(2L, "行政部", "负责综合支持");

        insertUser(9001L, "admin", "系统管理员", 1L, 1L, 1);
        insertUser(1001L, "zhangsan", "张三", 1L, 2L, 1);
        insertUser(1002L, "lisi", "李四", 1L, 2L, 1);
        insertUser(1003L, "wangwu", "王五", 2L, 2L, 1);

        insertDevice("DEV-001", "前台考勤机1", "办公区A", 1, "默认正常设备");
        insertDevice("DEV-002", "前台考勤机2", "办公区B", 1, "默认正常设备");
        insertDevice("DEV-003", "行政区考勤机", "行政区", 1, "行政部门设备");

        insertAttendanceRecord(2001L, 1001L, "2026-03-25 08:55:00", "IN", "DEV-001", "办公区A", 98.10, "NORMAL");
        insertAttendanceRecord(2002L, 1001L, "2026-03-25 09:18:00", "IN", "DEV-001", "办公区A", 83.40, "ABNORMAL");
        insertAttendanceRecord(2003L, 1001L, "2026-03-26 09:12:00", "IN", "DEV-002", "办公区B", 82.10, "ABNORMAL");
        insertAttendanceRecord(2004L, 1002L, "2026-03-25 08:58:00", "IN", "DEV-001", "办公区A", 97.20, "NORMAL");
        insertAttendanceRecord(2005L, 1002L, "2026-03-26 09:20:00", "IN", "DEV-002", "办公区B", 84.50, "ABNORMAL");
        insertAttendanceRecord(2006L, 1003L, "2026-03-26 08:57:00", "IN", "DEV-003", "行政区", 96.80, "NORMAL");
        insertAttendanceRecord(2007L, 1003L, "2026-03-26 09:25:00", "IN", "DEV-003", "行政区", 81.70, "ABNORMAL");

        insertAttendanceException(3001L, 2002L, 1001L, "LATE", "MEDIUM", "RULE", "超过上班时间阈值，判定为迟到", "REVIEWED");
        insertAttendanceException(3002L, 2003L, 1001L, "PROXY_CHECKIN", "HIGH", "MODEL", "疑似代打卡", "PENDING");
        insertAttendanceException(3003L, 2005L, 1002L, "LATE", "MEDIUM", "RULE", "超过上班时间阈值，判定为迟到", "REVIEWED");
        insertAttendanceException(3004L, 2007L, 1003L, "PROXY_CHECKIN", "HIGH", "MODEL", "疑似跨部门异常打卡", "PENDING");

        insertExceptionAnalysis(4001L, 3001L, "LATE", new BigDecimal("68.00"), "上班时间后首次打卡", "建议提醒员工关注考勤规则");
        insertExceptionAnalysis(4002L, 3002L, "PROXY_CHECKIN", new BigDecimal("93.50"), "设备与地点异常共同提升风险", "建议优先人工复核");
        insertExceptionAnalysis(4003L, 3003L, "LATE", new BigDecimal("71.20"), "连续两次接近迟到阈值", "建议部门负责人跟进");
        insertExceptionAnalysis(4004L, 3004L, "PROXY_CHECKIN", new BigDecimal("94.10"), "行政区设备短时多次异常", "建议核验现场情况");

        insertWarningRecord(5001L, 3001L, "ATTENDANCE_WARNING", "MEDIUM", "PROCESSED", new BigDecimal("68.00"), "张三存在迟到风险", "建议提醒员工", "RULE", "2026-03-25 09:20:00");
        insertWarningRecord(5002L, 3002L, "RISK_WARNING", "HIGH", "UNPROCESSED", new BigDecimal("93.50"), "张三疑似代打卡", "建议优先人工复核", "MODEL_FUSION", "2026-03-26 09:13:00");
        insertWarningRecord(5003L, 3003L, "ATTENDANCE_WARNING", "MEDIUM", "PROCESSED", new BigDecimal("71.20"), "李四迟到风险上升", "建议部门负责人跟进", "RULE", "2026-03-26 09:22:00");
        insertWarningRecord(5004L, 3004L, "RISK_WARNING", "HIGH", "UNPROCESSED", new BigDecimal("94.10"), "王五疑似跨部门异常打卡", "建议核验现场情况", "MODEL_FUSION", "2026-03-26 09:27:00");

        insertReviewRecord(6001L, 3001L, 9001L, "CONFIRMED", "确认迟到异常", "建议后续持续关注", "存在同类迟到样本", "TRUE_POSITIVE", "建议保留当前规则", "2026-03-25 10:00:00");
        insertReviewRecord(6002L, 3003L, 9001L, "CONFIRMED", "确认迟到异常", "建议部门负责人提醒", "存在同类迟到样本", "TRUE_POSITIVE", "建议增加晨会提醒", "2026-03-26 10:05:00");

        insertOperationLog(7001L, 9001L, "LOGIN", "管理员登录系统", "2026-03-25 08:40:00");
        insertOperationLog(7002L, 9001L, "WARNING", "管理员重新评估高风险预警", "2026-03-26 10:30:00");
        insertOperationLog(7003L, 1001L, "CHECKIN", "员工完成上班打卡", "2026-03-26 09:12:30");
    }

    @Test
    void shouldReturnPersonalStatistics() throws Exception {
        String employeeToken = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(get("/api/statistics/personal")
                        .param("startDate", "2026-03-25")
                        .param("endDate", "2026-03-26")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(1001))
                .andExpect(jsonPath("$.data.recordCount").value(3))
                .andExpect(jsonPath("$.data.exceptionCount").value(2))
                .andExpect(jsonPath("$.data.analysisCount").value(2))
                .andExpect(jsonPath("$.data.warningCount").value(2))
                .andExpect(jsonPath("$.data.reviewCount").value(1))
                .andExpect(jsonPath("$.data.closedLoopCount").value(1));
    }

    @Test
    void shouldReturnDepartmentStatistics() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/statistics/department")
                        .param("startDate", "2026-03-25")
                        .param("endDate", "2026-03-26")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.deptName").value("全部部门"))
                .andExpect(jsonPath("$.data.recordCount").value(7))
                .andExpect(jsonPath("$.data.exceptionCount").value(4))
                .andExpect(jsonPath("$.data.analysisCount").value(4))
                .andExpect(jsonPath("$.data.warningCount").value(4))
                .andExpect(jsonPath("$.data.reviewCount").value(2))
                .andExpect(jsonPath("$.data.closedLoopCount").value(2));
    }

    @Test
    void shouldReturnExceptionTrend() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/statistics/exception-trend")
                        .param("deptId", "1")
                        .param("startDate", "2026-03-25")
                        .param("endDate", "2026-03-26")
                        .param("periodType", "DAY")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.points.length()").value(2))
                .andExpect(jsonPath("$.data.points[0].date").value("2026-03-25"))
                .andExpect(jsonPath("$.data.points[0].recordCount").value(3))
                .andExpect(jsonPath("$.data.points[0].exceptionCount").value(1))
                .andExpect(jsonPath("$.data.points[0].analysisCount").value(1))
                .andExpect(jsonPath("$.data.points[0].warningCount").value(1))
                .andExpect(jsonPath("$.data.points[0].reviewCount").value(1))
                .andExpect(jsonPath("$.data.points[0].closedLoopCount").value(1))
                .andExpect(jsonPath("$.data.points[1].date").value("2026-03-26"))
                .andExpect(jsonPath("$.data.points[1].recordCount").value(2))
                .andExpect(jsonPath("$.data.points[1].exceptionCount").value(2))
                .andExpect(jsonPath("$.data.points[1].analysisCount").value(2))
                .andExpect(jsonPath("$.data.points[1].warningCount").value(2))
                .andExpect(jsonPath("$.data.points[1].reviewCount").value(1))
                .andExpect(jsonPath("$.data.points[1].closedLoopCount").value(1));
    }

    @Test
    void shouldReturnExceptionTypeTrend() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/statistics/exception-type-trend")
                        .param("deptId", "1")
                        .param("startDate", "2026-03-25")
                        .param("endDate", "2026-03-26")
                        .param("periodType", "DAY")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.labels.length()").value(2))
                .andExpect(jsonPath("$.data.labels[0]").value("2026-03-25"))
                .andExpect(jsonPath("$.data.items[0].type").value("LATE"))
                .andExpect(jsonPath("$.data.items[0].values.length()").value(2));
    }

    @Test
    void shouldAggregateExceptionTrendByWeek() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/statistics/exception-trend")
                        .param("deptId", "1")
                        .param("startDate", "2026-03-24")
                        .param("endDate", "2026-03-30")
                        .param("periodType", "WEEK")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.points.length()").value(1))
                .andExpect(jsonPath("$.data.points[0].date").value("2026-03-23"))
                .andExpect(jsonPath("$.data.points[0].recordCount").value(5))
                .andExpect(jsonPath("$.data.points[0].exceptionCount").value(3))
                .andExpect(jsonPath("$.data.points[0].analysisCount").value(3))
                .andExpect(jsonPath("$.data.points[0].warningCount").value(3))
                .andExpect(jsonPath("$.data.points[0].reviewCount").value(2))
                .andExpect(jsonPath("$.data.points[0].closedLoopCount").value(2));
    }

    @Test
    void shouldReturnStatisticsSummary() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/statistics/summary")
                        .param("startDate", "2026-03-25")
                        .param("endDate", "2026-03-26")
                        .param("periodType", "DAY")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.periodType").value("DAY"))
                .andExpect(jsonPath("$.data.summary").value("王五疑似跨部门异常打卡"))
                .andExpect(jsonPath("$.data.highlightRisks").value("疑似跨部门异常打卡"))
                .andExpect(jsonPath("$.data.manageSuggestion").value("建议部门负责人提醒"));
    }

    @Test
    void shouldReturnPersonalSummaryForEmployee() throws Exception {
        String employeeToken = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(get("/api/statistics/summary")
                        .param("startDate", "2026-03-25")
                        .param("endDate", "2026-03-26")
                        .param("periodType", "DAY")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.summary").value("张三疑似代打卡"))
                .andExpect(jsonPath("$.data.highlightRisks").value("疑似代打卡"))
                .andExpect(jsonPath("$.data.manageSuggestion").value("建议优先人工复核"));
    }

    @Test
    void shouldReturnDepartmentRiskBrief() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/statistics/department-risk-brief")
                        .param("deptId", "1")
                        .param("startDate", "2026-03-25")
                        .param("endDate", "2026-03-26")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.deptId").value(1))
                .andExpect(jsonPath("$.data.deptName").value("技术部"))
                .andExpect(jsonPath("$.data.riskScore", greaterThan(0.0)))
                .andExpect(jsonPath("$.data.riskSummary").value("超过上班时间阈值，判定为迟到"))
                .andExpect(jsonPath("$.data.manageSuggestion").value("建议部门负责人提醒"));
    }

    @Test
    void shouldReturnDepartmentRiskOverview() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/statistics/department-risk-overview")
                        .param("startDate", "2026-03-25")
                        .param("endDate", "2026-03-26")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].deptId").value(2))
                .andExpect(jsonPath("$.data[0].deptName").value("行政部"))
                .andExpect(jsonPath("$.data[0].riskSummary").value("疑似跨部门异常打卡"))
                .andExpect(jsonPath("$.data[0].manageSuggestion").value("建议核验现场情况"))
                .andExpect(jsonPath("$.data[1].deptId").value(1))
                .andExpect(jsonPath("$.data[1].deptName").value("技术部"))
                .andExpect(jsonPath("$.data[1].riskSummary").value("超过上班时间阈值，判定为迟到"));
    }

    @Test
    void shouldExportStatistics() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");

        MvcResult result = mockMvc.perform(get("/api/statistics/export")
                        .param("exportType", "DEPARTMENT")
                        .param("startDate", "2026-03-25")
                        .param("endDate", "2026-03-26")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", allOf(
                        containsString("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                )))
                .andExpect(header().string("Content-Disposition", allOf(
                        containsString("statistics-export.xlsx"),
                        containsString("filename*=UTF-8''")
                )))
                .andReturn();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()))) {
            assertEquals("部门统计报表", workbook.getSheetAt(0).getSheetName());
            assertEquals("部门统计报表", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertEquals("统计周期", workbook.getSheetAt(0).getRow(2).getCell(0).getStringCellValue());
            assertEquals("汇总概况", workbook.getSheetAt(0).getRow(4).getCell(0).getStringCellValue());
            assertEquals("全部部门", workbook.getSheetAt(0).getRow(6).getCell(0).getStringCellValue());
            assertEquals("部门明细", workbook.getSheetAt(0).getRow(8).getCell(0).getStringCellValue());
            assertEquals("处置完成率", workbook.getSheetAt(0).getRow(5).getCell(13).getStringCellValue());
            assertEquals("异常率", workbook.getSheetAt(0).getRow(9).getCell(11).getStringCellValue());
            assertEquals("技术部", workbook.getSheetAt(0).getRow(10).getCell(2).getStringCellValue());
            assertEquals("行政部", workbook.getSheetAt(0).getRow(11).getCell(2).getStringCellValue());
            assertEquals("指标说明", workbook.getSheetAt(0).getRow(13).getCell(0).getStringCellValue());
            assertEquals("系统处理次数", workbook.getSheetAt(0).getRow(15).getCell(0).getStringCellValue());
        }
    }

    @Test
    void shouldExportWarningDashboardStatistics() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/statistics/export")
                        .param("exportType", "WARNING_DASHBOARD")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(content().string(containsString("预警看板,预警总量")))
                .andExpect(content().string(containsString("预警看板,超时24-48小时")))
                .andExpect(content().string(containsString("预警看板,高风险人员")))
                .andExpect(content().string(containsString("高风险人员排行")))
                .andExpect(content().string(containsString("异常人员画像")))
                .andExpect(content().string(containsString("风险层级")))
                .andExpect(content().string(containsString("处置超时提醒")));
    }

    @Test
    void shouldReturnBadRequestWhenAuditExportExceedsLimit() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");
        for (long i = 0; i < 5001; i++) {
            insertOperationLog(8000L + i, 9001L, "AUDIT", "批量审计日志" + i, "2026-03-26 11:00:00");
        }

        mockMvc.perform(get("/api/statistics/export")
                        .param("exportType", "AUDIT")
                        .param("startDate", "2026-03-25")
                        .param("endDate", "2026-03-26")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("导出数据量过大，请缩小查询范围"));
    }

    @Test
    void shouldReturnOperationLogList() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/log/operation/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("type", "WARNING")
                        .param("startDate", "2026-03-26")
                        .param("endDate", "2026-03-26")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(7002))
                .andExpect(jsonPath("$.data.records[0].type").value("WARNING"))
                .andExpect(jsonPath("$.data.records[0].content").value("管理员重新评估高风险预警"))
                .andExpect(jsonPath("$.data.records[0].operationTime").exists());
    }

    @Test
    void shouldAllowAdminAccessPersonalStatisticsByCurrentSession() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/statistics/personal")
                        .param("startDate", "2026-03-25")
                        .param("endDate", "2026-03-26")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(9001))
                .andExpect(jsonPath("$.data.recordCount").value(0))
                .andExpect(jsonPath("$.data.exceptionCount").value(0));
    }

    @Test
    void shouldIgnoreExternalUserIdWhenEmployeeAccessesPersonalStatistics() throws Exception {
        String employeeToken = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(get("/api/statistics/personal")
                        .param("userId", "1002")
                        .param("startDate", "2026-03-25")
                        .param("endDate", "2026-03-26")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(1001))
                .andExpect(jsonPath("$.data.recordCount").value(3));
    }

    @Test
    void shouldReturnForbiddenWhenEmployeeAccessesDepartmentStatistics() throws Exception {
        String employeeToken = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(get("/api/statistics/department")
                        .param("startDate", "2026-03-25")
                        .param("endDate", "2026-03-26")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void shouldAllowLoginWhenOperationLogWriteFails() throws Exception {
        jdbcTemplate.execute("DROP TABLE operationLog");
        try {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(APPLICATION_JSON)
                            .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.roleCode").value("ADMIN"));
        } finally {
            recreateOperationLogTable();
        }
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
                "INSERT INTO attendanceException (id, recordId, userId, type, riskLevel, sourceType, description, processStatus, createTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, COALESCE((SELECT checkTime FROM attendanceRecord WHERE id = ?), CURRENT_TIMESTAMP))",
                id,
                recordId,
                userId,
                type,
                riskLevel,
                sourceType,
                description,
                processStatus,
                recordId
        );
    }

    private void insertExceptionAnalysis(Long id,
                                         Long exceptionId,
                                         String modelConclusion,
                                         BigDecimal confidenceScore,
                                         String reasonSummary,
                                         String actionSuggestion) {
        jdbcTemplate.update(
                "INSERT INTO exceptionAnalysis (id, exceptionId, promptTemplateId, inputSummary, modelResult, modelConclusion, confidenceScore, decisionReason, suggestion, reasonSummary, actionSuggestion, similarCaseSummary, promptVersion, createTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, COALESCE((SELECT createTime FROM attendanceException WHERE id = ?), CURRENT_TIMESTAMP))",
                id,
                exceptionId,
                null,
                "statistics-input",
                "statistics-model-result",
                modelConclusion,
                confidenceScore,
                reasonSummary,
                actionSuggestion,
                reasonSummary,
                actionSuggestion,
                "similar-case",
                "v1.0",
                exceptionId
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

    private void insertOperationLog(Long id,
                                    Long userId,
                                    String type,
                                    String content,
                                    String operationTime) {
        jdbcTemplate.update(
                "INSERT INTO operationLog (id, userId, type, content, operationTime) VALUES (?, ?, ?, ?, ?)",
                id,
                userId,
                type,
                content,
                operationTime
        );
    }

    private void recreateOperationLogTable() {
        jdbcTemplate.execute("CREATE TABLE operationLog (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, userId BIGINT DEFAULT NULL, type VARCHAR(50) NOT NULL, content VARCHAR(255) NOT NULL, operationTime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, CONSTRAINT fkOperationLogUser FOREIGN KEY (userId) REFERENCES user (id) ON DELETE SET NULL)");
        jdbcTemplate.execute("CREATE INDEX idxOperationLogUserTime ON operationLog (userId, operationTime)");
    }

    private String loginAndExtractToken(String username, String password) throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        JsonNode response = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        String token = response.path("data").path("token").asText();
        assertTrue(token != null && !token.isEmpty());
        return token;
    }
}
