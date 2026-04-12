package com.quyong.attendance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quyong.attendance.module.exceptiondetect.dto.RuleCheckDTO;
import com.quyong.attendance.module.exceptiondetect.service.ExceptionAnalysisOrchestrator;
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
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AttendanceManagementIntegrationTest {

    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExceptionAnalysisOrchestrator exceptionAnalysisOrchestrator;

    @MockBean
    private Clock clock;

    @MockBean
    private ModelGateway modelGateway;

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
        jdbcTemplate.execute("DELETE FROM riskLevel");
        jdbcTemplate.execute("DELETE FROM rule");
        jdbcTemplate.execute("DELETE FROM device");
        jdbcTemplate.execute("DELETE FROM user");
        jdbcTemplate.execute("DELETE FROM department");
        jdbcTemplate.execute("DELETE FROM role");

        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Shanghai"));
        when(clock.instant()).thenReturn(LocalDateTime.of(2026, 3, 31, 8, 59, 0).atZone(ZoneId.of("Asia/Shanghai")).toInstant());

        insertRole(1L, "ADMIN", "管理员");
        insertRole(2L, "EMPLOYEE", "员工");

        insertDepartment(1L, "技术部", "负责系统研发");
        insertDepartment(2L, "行政部", "负责行政支持");

        insertUser(9001L, "admin", "系统管理员", 1L, 1L, 1);
        insertUser(1001L, "zhangsan", "张三", 1L, 2L, 1);
        insertUser(1002L, "lisi", "李四", 2L, 2L, 1);

        insertRiskLevel(1L, "HIGH", "高风险", "需要优先人工复核", 1);
        insertRiskLevel(2L, "MEDIUM", "中风险", "建议尽快关注并结合历史记录判断", 1);
        insertRiskLevel(3L, "LOW", "低风险", "记录留档并持续观察", 1);
        insertRule(1L, "默认考勤规则", "09:00:00", "18:00:00", 10, 10, 3, 1);

        insertDevice("DEV-001", "前台考勤机", "办公区A", 1, "正常设备");
        insertDevice("DEV-002", "后门考勤机", "办公区B", 0, "停用设备");
    }

    @Test
    void shouldReturnEnabledDeviceOptionsForEmployee() throws Exception {
        String token = loginAndExtractToken("zhangsan", "123456");
        jdbcTemplate.update(
                "UPDATE device SET longitude = ?, latitude = ? WHERE id = ?",
                new BigDecimal("116.397128"),
                new BigDecimal("39.916527"),
                "DEV-001"
        );

        mockMvc.perform(get("/api/attendance/device-options")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].deviceId").value("DEV-001"))
                .andExpect(jsonPath("$.data[0].name").value("前台考勤机"))
                .andExpect(jsonPath("$.data[0].location").value("办公区A"))
                .andExpect(jsonPath("$.data[0].longitude").value(116.397128))
                .andExpect(jsonPath("$.data[0].latitude").value(39.916527))
                .andExpect(jsonPath("$.data[0].status").doesNotExist())
                .andExpect(jsonPath("$.data[0].description").doesNotExist());
    }

    @Test
    void shouldCheckInWhenRequestBodyOmitsUserIdIpAndLocationAndFaceVerifyPasses() throws Exception {
        jdbcTemplate.update(
                "UPDATE device SET longitude = ?, latitude = ? WHERE id = ?",
                new BigDecimal("116.397128"),
                new BigDecimal("39.916527"),
                "DEV-001"
        );
        insertFaceFeature(1001L, "face-image-checkin-success", 1101L, "hash-success-1101");
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(post("/api/attendance/checkin")
                        .with(request -> {
                            request.setRemoteAddr("192.168.1.66");
                            return request;
                        })
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"checkType\":\"IN\",\"deviceId\":\"DEV-001\",\"clientLongitude\":116.397128,\"clientLatitude\":39.916527,\"imageData\":\"face-image-checkin-success\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.userId").value(1001))
                .andExpect(jsonPath("$.data.checkType").value("IN"))
                .andExpect(jsonPath("$.data.deviceId").value("DEV-001"))
                .andExpect(jsonPath("$.data.location").value("办公区A"))
                .andExpect(jsonPath("$.data.faceScore").value(99.99))
                .andExpect(jsonPath("$.data.threshold").value(85.0))
                .andExpect(jsonPath("$.data.status").value("NORMAL"));

        Integer checkinLogCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM operationLog WHERE userId = ? AND type = 'CHECKIN'",
                Integer.class,
                1001L
        );
        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(1), checkinLogCount);
        org.junit.jupiter.api.Assertions.assertEquals(
                "192.168.1.66",
                jdbcTemplate.queryForObject(
                        "SELECT ipAddr FROM attendanceRecord WHERE userId = ? ORDER BY id DESC LIMIT 1",
                        String.class,
                        1001L
                )
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                "办公区A",
                jdbcTemplate.queryForObject(
                        "SELECT location FROM attendanceRecord WHERE userId = ? ORDER BY id DESC LIMIT 1",
                        String.class,
                        1001L
                )
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                new BigDecimal("116.397128"),
                jdbcTemplate.queryForObject(
                        "SELECT longitude FROM attendanceRecord WHERE userId = ? ORDER BY id DESC LIMIT 1",
                        BigDecimal.class,
                        1001L
                )
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                new BigDecimal("39.916527"),
                jdbcTemplate.queryForObject(
                        "SELECT latitude FROM attendanceRecord WHERE userId = ? ORDER BY id DESC LIMIT 1",
                        BigDecimal.class,
                        1001L
                )
        );
    }

    @Test
    void shouldRejectCheckInWhenFaceIsNotRegistered() throws Exception {
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(post("/api/attendance/checkin")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":1001,\"checkType\":\"IN\",\"deviceId\":\"DEV-001\",\"clientLongitude\":116.397128,\"clientLatitude\":39.916527,\"imageData\":\"face-image-not-registered\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("该用户未录入人脸"));
    }

    @Test
    void shouldRejectCheckInWhenFaceDoesNotMatch() throws Exception {
        insertFaceFeature(1001L, "face-image-origin", 1102L, "hash-origin-1102");
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(post("/api/attendance/checkin")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":1001,\"checkType\":\"IN\",\"deviceId\":\"DEV-001\",\"clientLongitude\":116.397128,\"clientLatitude\":39.916527,\"imageData\":\"face-image-other\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("人脸验证未通过"));
    }

    @Test
    void shouldRejectCheckInWhenDeviceIsDisabled() throws Exception {
        insertFaceFeature(1001L, "face-image-disabled-device", 1103L, "hash-disabled-1103");
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(post("/api/attendance/checkin")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":1001,\"checkType\":\"IN\",\"deviceId\":\"DEV-002\",\"clientLongitude\":116.397128,\"clientLatitude\":39.916527,\"imageData\":\"face-image-disabled-device\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("打卡地点已停用，不能打卡"));
    }

    @Test
    void shouldIgnoreRequestUserIdWhenEmployeeChecksIn() throws Exception {
        insertFaceFeature(1001L, "face-image-for-zhangsan", 1104L, "hash-zhangsan-1104");
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(post("/api/attendance/checkin")
                        .with(request -> {
                            request.setRemoteAddr("192.168.1.21");
                            return request;
                        })
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":1002,\"checkType\":\"IN\",\"deviceId\":\"DEV-001\",\"clientLongitude\":116.397128,\"clientLatitude\":39.916527,\"imageData\":\"face-image-for-zhangsan\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.userId").value(1001));

        Integer currentUserCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendanceRecord WHERE userId = ?",
                Integer.class,
                1001L
        );
        Integer ignoredUserCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendanceRecord WHERE userId = ?",
                Integer.class,
                1002L
        );

        org.junit.jupiter.api.Assertions.assertEquals(1, currentUserCount);
        org.junit.jupiter.api.Assertions.assertEquals(0, ignoredUserCount);
    }

    @Test
    void shouldAutoCreateRuleExceptionAndWarningAfterLateCheckin() throws Exception {
        when(clock.instant()).thenReturn(LocalDateTime.of(2026, 3, 31, 9, 16, 0).atZone(ZoneId.of("Asia/Shanghai")).toInstant());
        insertFaceFeature(1001L, "face-image-late-auto", 1201L, "hash-late-1201");
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(post("/api/attendance/checkin")
                        .with(request -> {
                            request.setRemoteAddr("192.168.1.66");
                            return request;
                        })
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"checkType\":\"IN\",\"deviceId\":\"DEV-001\",\"clientLongitude\":116.397128,\"clientLatitude\":39.916527,\"imageData\":\"face-image-late-auto\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("ABNORMAL"));

        Integer ruleExceptionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendanceException WHERE userId = ? AND sourceType = 'RULE' AND type = 'LATE'",
                Integer.class,
                1001L
        );
        Integer warningCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM warningRecord",
                Integer.class
        );
        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(1), ruleExceptionCount);
        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(1), warningCount);
    }

    @Test
    void shouldUpgradeToContinuousLateExceptionAfterThreeRecentLateCheckins() throws Exception {
        when(clock.instant()).thenReturn(LocalDateTime.of(2026, 3, 31, 9, 16, 0).atZone(ZoneId.of("Asia/Shanghai")).toInstant());
        insertAttendanceRecord(1988L, 1001L, "2026-03-29 09:14:00", "IN", "DEV-001", "办公区A", 97.20, "ABNORMAL");
        insertAttendanceRecord(1989L, 1001L, "2026-03-30 09:13:00", "IN", "DEV-001", "办公区A", 97.40, "ABNORMAL");
        insertFaceFeature(1001L, "face-image-continuous-late", 1206L, "hash-continuous-late-1206");
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(post("/api/attendance/checkin")
                        .with(request -> {
                            request.setRemoteAddr("192.168.1.91");
                            return request;
                        })
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"checkType\":\"IN\",\"deviceId\":\"DEV-001\",\"clientLongitude\":116.397128,\"clientLatitude\":39.916527,\"imageData\":\"face-image-continuous-late\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("ABNORMAL"));

        Integer continuousLateCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendanceException WHERE userId = ? AND sourceType = 'RULE' AND type = 'CONTINUOUS_LATE'",
                Integer.class,
                1001L
        );
        Integer warningCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM warningRecord WHERE level = 'HIGH'",
                Integer.class
        );
        String description = jdbcTemplate.queryForObject(
                "SELECT description FROM attendanceException WHERE userId = ? AND sourceType = 'RULE' AND type = 'CONTINUOUS_LATE' ORDER BY id DESC LIMIT 1",
                String.class,
                1001L
        );

        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(1), continuousLateCount);
        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(1), warningCount);
        org.junit.jupiter.api.Assertions.assertTrue(description.contains("连续3次上班打卡迟到"));
    }

    @Test
    void shouldUpgradeToContinuousEarlyLeaveExceptionAfterThreeRecentEarlyLeaveCheckouts() throws Exception {
        when(clock.instant()).thenReturn(LocalDateTime.of(2026, 3, 31, 17, 44, 0).atZone(ZoneId.of("Asia/Shanghai")).toInstant());
        insertAttendanceRecord(1990L, 1001L, "2026-03-29 17:45:00", "OUT", "DEV-001", "办公区A", 97.20, "ABNORMAL");
        insertAttendanceRecord(1991L, 1001L, "2026-03-30 17:46:00", "OUT", "DEV-001", "办公区A", 97.40, "ABNORMAL");
        insertFaceFeature(1001L, "face-image-continuous-early-leave", 1207L, "hash-continuous-early-leave-1207");
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(post("/api/attendance/checkin")
                        .with(request -> {
                            request.setRemoteAddr("192.168.1.92");
                            return request;
                        })
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"checkType\":\"OUT\",\"deviceId\":\"DEV-001\",\"clientLongitude\":116.397128,\"clientLatitude\":39.916527,\"imageData\":\"face-image-continuous-early-leave\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("ABNORMAL"));

        Integer continuousEarlyLeaveCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendanceException WHERE userId = ? AND sourceType = 'RULE' AND type = 'CONTINUOUS_EARLY_LEAVE'",
                Integer.class,
                1001L
        );
        Integer warningCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM warningRecord WHERE level = 'HIGH'",
                Integer.class
        );
        String description = jdbcTemplate.queryForObject(
                "SELECT description FROM attendanceException WHERE userId = ? AND sourceType = 'RULE' AND type = 'CONTINUOUS_EARLY_LEAVE' ORDER BY id DESC LIMIT 1",
                String.class,
                1001L
        );

        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(1), continuousEarlyLeaveCount);
        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(1), warningCount);
        org.junit.jupiter.api.Assertions.assertTrue(description.contains("连续3次下班打卡早退"));
    }

    @Test
    void shouldUpgradeToContinuousIllegalTimeExceptionAfterThreeRecentIllegalTimeCheckins() throws Exception {
        when(clock.instant()).thenReturn(LocalDateTime.of(2026, 3, 31, 4, 45, 0).atZone(ZoneId.of("Asia/Shanghai")).toInstant());
        insertAttendanceRecord(1992L, 1001L, "2026-03-29 04:20:00", "IN", "DEV-001", "办公区A", 97.20, "ABNORMAL");
        insertAttendanceRecord(1993L, 1001L, "2026-03-30 04:40:00", "IN", "DEV-001", "办公区A", 97.40, "ABNORMAL");
        insertFaceFeature(1001L, "face-image-continuous-illegal-time", 1209L, "hash-continuous-illegal-time-1209");
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(post("/api/attendance/checkin")
                        .with(request -> {
                            request.setRemoteAddr("192.168.1.94");
                            return request;
                        })
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"checkType\":\"IN\",\"deviceId\":\"DEV-001\",\"clientLongitude\":116.397128,\"clientLatitude\":39.916527,\"imageData\":\"face-image-continuous-illegal-time\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("ABNORMAL"));

        Integer continuousIllegalTimeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendanceException WHERE userId = ? AND sourceType = 'RULE' AND type = 'CONTINUOUS_ILLEGAL_TIME'",
                Integer.class,
                1001L
        );
        String description = jdbcTemplate.queryForObject(
                "SELECT description FROM attendanceException WHERE userId = ? AND sourceType = 'RULE' AND type = 'CONTINUOUS_ILLEGAL_TIME' ORDER BY id DESC LIMIT 1",
                String.class,
                1001L
        );

        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(1), continuousIllegalTimeCount);
        org.junit.jupiter.api.Assertions.assertTrue(description.contains("连续3次发生非法时间打卡"));
    }

    @Test
    void shouldUpgradeToContinuousRepeatCheckExceptionAfterThreeRecentRepeatCheckins() throws Exception {
        when(clock.instant()).thenReturn(LocalDateTime.of(2026, 3, 31, 8, 59, 0).atZone(ZoneId.of("Asia/Shanghai")).toInstant());
        insertAttendanceRecord(1984L, 1001L, "2026-03-29 08:56:00", "IN", "DEV-001", "办公区A", 97.20, "ABNORMAL");
        insertAttendanceRecord(1985L, 1001L, "2026-03-30 08:57:00", "IN", "DEV-001", "办公区A", 97.40, "ABNORMAL");
        insertAttendanceRecord(1995L, 1001L, "2026-03-31 08:58:00", "IN", "DEV-001", "办公区A", 98.40, "NORMAL");
        jdbcTemplate.update(
                "INSERT INTO attendanceException (id, recordId, userId, type, riskLevel, sourceType, description, processStatus, createTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                4014L,
                1984L,
                1001L,
                "REPEAT_CHECK",
                "LOW",
                "RULE",
                "历史重复打卡一",
                "PENDING",
                "2026-03-29 08:56:00"
        );
        jdbcTemplate.update(
                "INSERT INTO attendanceException (id, recordId, userId, type, riskLevel, sourceType, description, processStatus, createTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                4015L,
                1985L,
                1001L,
                "REPEAT_CHECK",
                "LOW",
                "RULE",
                "历史重复打卡二",
                "PENDING",
                "2026-03-30 08:57:00"
        );
        insertFaceFeature(1001L, "face-image-continuous-repeat-check", 1210L, "hash-continuous-repeat-check-1210");
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(post("/api/attendance/checkin")
                        .with(request -> {
                            request.setRemoteAddr("192.168.1.95");
                            return request;
                        })
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"checkType\":\"IN\",\"deviceId\":\"DEV-001\",\"clientLongitude\":116.397128,\"clientLatitude\":39.916527,\"imageData\":\"face-image-continuous-repeat-check\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("ABNORMAL"));

        Integer continuousRepeatCheckCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendanceException WHERE userId = ? AND sourceType = 'RULE' AND type = 'CONTINUOUS_REPEAT_CHECK'",
                Integer.class,
                1001L
        );
        String description = jdbcTemplate.queryForObject(
                "SELECT description FROM attendanceException WHERE userId = ? AND sourceType = 'RULE' AND type = 'CONTINUOUS_REPEAT_CHECK' ORDER BY id DESC LIMIT 1",
                String.class,
                1001L
        );

        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(1), continuousRepeatCheckCount);
        org.junit.jupiter.api.Assertions.assertTrue(description.contains("连续3次出现短时间重复打卡"));
    }

    @Test
    void shouldUpgradeToContinuousAttendanceRiskAfterThreeRecentRuleExceptions() throws Exception {
        when(clock.instant()).thenReturn(LocalDateTime.of(2026, 3, 31, 9, 16, 0).atZone(ZoneId.of("Asia/Shanghai")).toInstant());
        insertAttendanceRecord(1996L, 1001L, "2026-03-28 04:20:00", "IN", "DEV-001", "办公区A", 97.20, "ABNORMAL");
        insertAttendanceRecord(1997L, 1001L, "2026-03-30 08:57:00", "IN", "DEV-001", "办公区A", 97.40, "ABNORMAL");
        insertAttendanceException(4018L, 1996L, 1001L, "ILLEGAL_TIME", "HIGH", "RULE", "历史非法时间打卡", "PENDING", "2026-03-28 04:21:00");
        insertAttendanceException(4019L, 1997L, 1001L, "REPEAT_CHECK", "LOW", "RULE", "历史重复打卡", "PENDING", "2026-03-30 08:57:00");
        insertFaceFeature(1001L, "face-image-continuous-attendance-risk", 1212L, "hash-continuous-attendance-risk-1212");
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(post("/api/attendance/checkin")
                        .with(request -> {
                            request.setRemoteAddr("192.168.1.97");
                            return request;
                        })
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"checkType\":\"IN\",\"deviceId\":\"DEV-001\",\"clientLongitude\":116.397128,\"clientLatitude\":39.916527,\"imageData\":\"face-image-continuous-attendance-risk\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("ABNORMAL"));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendanceException WHERE userId = ? AND sourceType = 'RULE' AND type = 'CONTINUOUS_ATTENDANCE_RISK'",
                Integer.class,
                1001L
        );
        String description = jdbcTemplate.queryForObject(
                "SELECT description FROM attendanceException WHERE userId = ? AND sourceType = 'RULE' AND type = 'CONTINUOUS_ATTENDANCE_RISK' ORDER BY id DESC LIMIT 1",
                String.class,
                1001L
        );

        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(1), count);
        org.junit.jupiter.api.Assertions.assertTrue(description.contains("连续3次出现规则异常"));
    }

    @Test
    void shouldAutoCreateModelExceptionAndWarningAfterSuspiciousCheckin() throws Exception {
        insertDevice("DEV-009", "异地设备", "外部区域", 1, "用于复杂异常触发");
        insertAttendanceRecord(1999L, 1001L, "2026-03-30 08:58:00", "IN", "DEV-009", "外部区域", 95.50, "NORMAL");
        insertPromptTemplate(8001L, "COMPLEX_EXCEPTION", "复杂异常分析模板", "EXCEPTION_ANALYSIS", "v1.0", "请输出结构化分析结果", "ENABLED", "默认模板");
        insertFaceFeature(1001L, "face-image-model-auto", 1202L, "hash-model-1202");
        when(modelGateway.invoke(any(ModelInvokeRequest.class))).thenReturn(mockModelResponse());
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(post("/api/attendance/checkin")
                        .with(request -> {
                            request.setRemoteAddr("192.168.1.77");
                            return request;
                        })
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"checkType\":\"IN\",\"deviceId\":\"DEV-001\",\"clientLongitude\":116.397128,\"clientLatitude\":39.916527,\"imageData\":\"face-image-model-auto\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("ABNORMAL"));

        Integer modelExceptionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendanceException WHERE userId = ? AND sourceType = 'MODEL' AND type = 'PROXY_CHECKIN'",
                Integer.class,
                1001L
        );
        Integer warningCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM warningRecord WHERE type = 'RISK_WARNING'",
                Integer.class
        );
        Integer modelLogCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM modelCallLog WHERE businessType = 'EXCEPTION_ANALYSIS' AND status = 'SUCCESS'",
                Integer.class
        );
        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(1), modelExceptionCount);
        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(1), warningCount);
        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(1), modelLogCount);
    }

    @Test
    void shouldUpgradeToContinuousProxyCheckinAfterThreeRecentModelDecisions() throws Exception {
        insertDevice("DEV-009", "异地设备", "外部区域", 1, "用于复杂异常触发");
        insertAttendanceRecord(1979L, 1001L, "2026-03-28 08:58:00", "IN", "DEV-009", "外部区域", 95.50, "NORMAL");
        insertAttendanceRecord(1980L, 1001L, "2026-03-29 08:58:00", "IN", "DEV-009", "外部区域", 95.40, "NORMAL");
        insertAttendanceException(4016L, 1979L, 1001L, "PROXY_CHECKIN", "HIGH", "MODEL", "历史疑似代打卡一", "PENDING", "2026-03-28 09:00:00");
        insertAttendanceException(4017L, 1980L, 1001L, "PROXY_CHECKIN", "HIGH", "MODEL", "历史疑似代打卡二", "PENDING", "2026-03-29 09:00:00");
        insertPromptTemplate(8001L, "COMPLEX_EXCEPTION", "复杂异常分析模板", "EXCEPTION_ANALYSIS", "v1.0", "请输出结构化分析结果", "ENABLED", "默认模板");
        insertFaceFeature(1001L, "face-image-continuous-proxy", 1211L, "hash-continuous-proxy-1211");
        when(modelGateway.invoke(any(ModelInvokeRequest.class))).thenReturn(mockModelResponse());
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(post("/api/attendance/checkin")
                        .with(request -> {
                            request.setRemoteAddr("192.168.1.96");
                            return request;
                        })
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"checkType\":\"IN\",\"deviceId\":\"DEV-001\",\"clientLongitude\":116.397128,\"clientLatitude\":39.916527,\"imageData\":\"face-image-continuous-proxy\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("ABNORMAL"));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendanceException WHERE userId = ? AND sourceType = 'MODEL' AND type = 'CONTINUOUS_PROXY_CHECKIN'",
                Integer.class,
                1001L
        );
        String description = jdbcTemplate.queryForObject(
                "SELECT description FROM attendanceException WHERE userId = ? AND sourceType = 'MODEL' AND type = 'CONTINUOUS_PROXY_CHECKIN' ORDER BY id DESC LIMIT 1",
                String.class,
                1001L
        );

        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(1), count);
        org.junit.jupiter.api.Assertions.assertTrue(description.contains("历史同类模型异常"));
    }

    @Test
    void shouldUpgradeToContinuousModelRiskAfterThreeRecentModelDecisions() throws Exception {
        insertDevice("DEV-009", "异地设备", "外部区域", 1, "用于复杂异常触发");
        insertAttendanceRecord(1979L, 1001L, "2026-03-28 08:58:00", "IN", "DEV-009", "外部区域", 95.50, "NORMAL");
        insertAttendanceRecord(1980L, 1001L, "2026-03-29 08:58:00", "IN", "DEV-009", "外部区域", 95.40, "NORMAL");
        insertAttendanceException(4020L, 1979L, 1001L, "DEVICE_RISK", "HIGH", "MODEL", "历史模型异常一", "PENDING", "2026-03-28 09:00:00");
        insertAttendanceException(4021L, 1980L, 1001L, "DEVICE_RISK", "HIGH", "MODEL", "历史模型异常二", "PENDING", "2026-03-29 09:00:00");
        insertPromptTemplate(8001L, "COMPLEX_EXCEPTION", "复杂异常分析模板", "EXCEPTION_ANALYSIS", "v1.0", "请输出结构化分析结果", "ENABLED", "默认模板");
        insertFaceFeature(1001L, "face-image-continuous-model-risk", 1213L, "hash-continuous-model-risk-1213");
        when(modelGateway.invoke(any(ModelInvokeRequest.class))).thenReturn(mockGenericModelResponse());
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(post("/api/attendance/checkin")
                        .with(request -> {
                            request.setRemoteAddr("192.168.1.98");
                            return request;
                        })
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"checkType\":\"IN\",\"deviceId\":\"DEV-001\",\"clientLongitude\":116.397128,\"clientLatitude\":39.916527,\"imageData\":\"face-image-continuous-model-risk\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("ABNORMAL"));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendanceException WHERE userId = ? AND sourceType = 'MODEL' AND type = 'CONTINUOUS_MODEL_RISK'",
                Integer.class,
                1001L
        );
        String description = jdbcTemplate.queryForObject(
                "SELECT description FROM attendanceException WHERE userId = ? AND sourceType = 'MODEL' AND type = 'CONTINUOUS_MODEL_RISK' ORDER BY id DESC LIMIT 1",
                String.class,
                1001L
        );

        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(1), count);
        org.junit.jupiter.api.Assertions.assertTrue(description.contains("历史模型异常在最近7天内已连续出现"));
    }

    @Test
    void shouldUpgradeToContinuousProxyCheckinExceptionAfterThreeRecentModelWarnings() throws Exception {
        insertDevice("DEV-009", "异地设备", "外部区域", 1, "用于复杂异常触发");
        insertAttendanceRecord(1979L, 1001L, "2026-03-28 08:58:00", "IN", "DEV-009", "外部区域", 95.50, "NORMAL");
        insertAttendanceRecord(1980L, 1001L, "2026-03-29 08:58:00", "IN", "DEV-009", "外部区域", 95.40, "NORMAL");
        insertAttendanceException(4016L, 1979L, 1001L, "PROXY_CHECKIN", "HIGH", "MODEL", "历史疑似代打卡一", "PENDING", "2026-03-28 09:00:00");
        insertAttendanceException(4017L, 1980L, 1001L, "PROXY_CHECKIN", "HIGH", "MODEL", "历史疑似代打卡二", "PENDING", "2026-03-29 09:00:00");
        insertPromptTemplate(8001L, "COMPLEX_EXCEPTION", "复杂异常分析模板", "EXCEPTION_ANALYSIS", "v1.0", "请输出结构化分析结果", "ENABLED", "默认模板");
        insertFaceFeature(1001L, "face-image-continuous-proxy", 1211L, "hash-continuous-proxy-1211");
        when(modelGateway.invoke(any(ModelInvokeRequest.class))).thenReturn(mockModelResponse());
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(post("/api/attendance/checkin")
                        .with(request -> {
                            request.setRemoteAddr("192.168.1.96");
                            return request;
                        })
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"checkType\":\"IN\",\"deviceId\":\"DEV-001\",\"clientLongitude\":116.397128,\"clientLatitude\":39.916527,\"imageData\":\"face-image-continuous-proxy\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("ABNORMAL"));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendanceException WHERE userId = ? AND sourceType = 'MODEL' AND type = 'CONTINUOUS_PROXY_CHECKIN'",
                Integer.class,
                1001L
        );
        String description = jdbcTemplate.queryForObject(
                "SELECT description FROM attendanceException WHERE userId = ? AND sourceType = 'MODEL' AND type = 'CONTINUOUS_PROXY_CHECKIN' ORDER BY id DESC LIMIT 1",
                String.class,
                1001L
        );

        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(1), count);
        org.junit.jupiter.api.Assertions.assertTrue(description.contains("历史同类模型异常"));
    }

    @Test
    void shouldNotBlockCheckinWhenComplexTemplateIsMissing() throws Exception {
        insertDevice("DEV-009", "异地设备", "外部区域", 1, "用于复杂异常触发");
        insertAttendanceRecord(1997L, 1001L, "2026-03-30 08:58:00", "IN", "DEV-009", "外部区域", 95.50, "NORMAL");
        insertFaceFeature(1001L, "face-image-no-template", 1204L, "hash-no-template-1204");
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(post("/api/attendance/checkin")
                        .with(request -> {
                            request.setRemoteAddr("192.168.1.79");
                            return request;
                        })
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"checkType\":\"IN\",\"deviceId\":\"DEV-001\",\"clientLongitude\":116.397128,\"clientLatitude\":39.916527,\"imageData\":\"face-image-no-template\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("NORMAL"));

        Integer exceptionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendanceException WHERE userId = ?",
                Integer.class,
                1001L
        );
        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(0), exceptionCount);
    }

    @Test
    void shouldAutoCreateMultiLocationRuleExceptionAfterCrossSiteCheckin() throws Exception {
        when(clock.instant()).thenReturn(LocalDateTime.of(2026, 3, 31, 9, 5, 0).atZone(ZoneId.of("Asia/Shanghai")).toInstant());
        jdbcTemplate.update(
                "UPDATE device SET longitude = ?, latitude = ? WHERE id = ?",
                new BigDecimal("116.397128"),
                new BigDecimal("39.916527"),
                "DEV-001"
        );
        insertDevice("DEV-009", "异地设备", "上海园区", 1, "用于多地点异常触发");
        jdbcTemplate.update(
                "UPDATE device SET longitude = ?, latitude = ? WHERE id = ?",
                new BigDecimal("121.473701"),
                new BigDecimal("31.230416"),
                "DEV-009"
        );
        jdbcTemplate.update(
                "INSERT INTO attendanceRecord (id, userId, checkTime, checkType, deviceId, ipAddr, location, longitude, latitude, faceScore, status, createTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                1998L,
                1001L,
                "2026-03-31 08:59:00",
                "IN",
                "DEV-001",
                "192.168.1.50",
                "办公区A",
                new BigDecimal("116.397128"),
                new BigDecimal("39.916527"),
                new BigDecimal("98.90"),
                "NORMAL"
        );
        insertFaceFeature(1001L, "face-image-cross-site", 1203L, "hash-cross-site-1203");
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(post("/api/attendance/checkin")
                        .with(request -> {
                            request.setRemoteAddr("192.168.1.88");
                            return request;
                        })
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"checkType\":\"IN\",\"deviceId\":\"DEV-009\",\"clientLongitude\":121.473700,\"clientLatitude\":31.230400,\"imageData\":\"face-image-cross-site\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("ABNORMAL"));

        Integer multiLocationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendanceException WHERE userId = ? AND sourceType = 'RULE' AND type = 'MULTI_LOCATION_CONFLICT'",
                Integer.class,
                1001L
        );
        Integer warningCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM warningRecord",
                Integer.class
        );
        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(1), multiLocationCount);
        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(1), warningCount);

        Long exceptionId = jdbcTemplate.queryForObject(
                "SELECT id FROM attendanceException WHERE userId = ? AND sourceType = 'RULE' AND type = 'MULTI_LOCATION_CONFLICT' ORDER BY id DESC LIMIT 1",
                Long.class,
                1001L
        );
        String exceptionDescription = jdbcTemplate.queryForObject(
                "SELECT description FROM attendanceException WHERE id = ?",
                String.class,
                exceptionId
        );
        Map<String, Object> decisionTrace = jdbcTemplate.queryForMap(
                "SELECT * FROM decisionTrace WHERE businessType = 'ATTENDANCE_EXCEPTION' AND businessId = ? ORDER BY id DESC LIMIT 1",
                exceptionId
        );
        String traceEvidence = decisionTrace.values().stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.joining(" | "));
        String visibleEvidence = exceptionDescription + " | " + traceEvidence;

        org.junit.jupiter.api.Assertions.assertTrue(visibleEvidence.contains("前一条地点=办公区A"));
        org.junit.jupiter.api.Assertions.assertTrue(visibleEvidence.contains("当前地点=上海园区"));
        org.junit.jupiter.api.Assertions.assertTrue(visibleEvidence.contains("间隔分钟=6"));
        org.junit.jupiter.api.Assertions.assertTrue(visibleEvidence.contains("实际距离米="));
        org.junit.jupiter.api.Assertions.assertTrue(visibleEvidence.contains("阈值距离米=3000"));
        org.junit.jupiter.api.Assertions.assertTrue(visibleEvidence.contains("窗口分钟=30"));
    }

    @Test
    void shouldUpgradeToContinuousMultiLocationConflictAfterThreeRecentCrossSiteCheckins() throws Exception {
        when(clock.instant()).thenReturn(LocalDateTime.of(2026, 3, 31, 9, 5, 0).atZone(ZoneId.of("Asia/Shanghai")).toInstant());
        jdbcTemplate.update(
                "UPDATE device SET longitude = ?, latitude = ? WHERE id = ?",
                new BigDecimal("116.397128"),
                new BigDecimal("39.916527"),
                "DEV-001"
        );
        insertDevice("DEV-012", "连续异地设备", "上海园区", 1, "用于连续多地点异常触发");
        jdbcTemplate.update(
                "UPDATE device SET longitude = ?, latitude = ? WHERE id = ?",
                new BigDecimal("121.473701"),
                new BigDecimal("31.230416"),
                "DEV-012"
        );
        jdbcTemplate.update(
                "INSERT INTO attendanceRecord (id, userId, checkTime, checkType, deviceId, ipAddr, location, longitude, latitude, faceScore, status, createTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                1994L,
                1001L,
                "2026-03-31 08:59:00",
                "IN",
                "DEV-001",
                "192.168.1.52",
                "办公区A",
                new BigDecimal("116.397128"),
                new BigDecimal("39.916527"),
                new BigDecimal("98.90"),
                "NORMAL"
        );
        jdbcTemplate.update(
                "INSERT INTO attendanceRecord (id, userId, checkTime, checkType, deviceId, ipAddr, location, longitude, latitude, faceScore, status, createTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                1986L,
                1001L,
                "2026-03-28 08:58:00",
                "IN",
                "DEV-001",
                "192.168.1.53",
                "办公区A",
                new BigDecimal("116.397128"),
                new BigDecimal("39.916527"),
                new BigDecimal("98.30"),
                "ABNORMAL"
        );
        jdbcTemplate.update(
                "INSERT INTO attendanceRecord (id, userId, checkTime, checkType, deviceId, ipAddr, location, longitude, latitude, faceScore, status, createTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                1987L,
                1001L,
                "2026-03-30 08:57:00",
                "IN",
                "DEV-001",
                "192.168.1.54",
                "办公区A",
                new BigDecimal("116.397128"),
                new BigDecimal("39.916527"),
                new BigDecimal("98.40"),
                "ABNORMAL"
        );
        jdbcTemplate.update(
                "INSERT INTO attendanceException (id, recordId, userId, type, riskLevel, sourceType, description, processStatus, createTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                4010L,
                1986L,
                1001L,
                "MULTI_LOCATION_CONFLICT",
                "HIGH",
                "RULE",
                "历史多地点冲突一",
                "PENDING",
                "2026-03-28 09:03:00"
        );
        jdbcTemplate.update(
                "INSERT INTO attendanceException (id, recordId, userId, type, riskLevel, sourceType, description, processStatus, createTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                4011L,
                1987L,
                1001L,
                "MULTI_LOCATION_CONFLICT",
                "HIGH",
                "RULE",
                "历史多地点冲突二",
                "PENDING",
                "2026-03-30 09:01:00"
        );
        insertFaceFeature(1001L, "face-image-continuous-multi-location", 1208L, "hash-continuous-multi-location-1208");
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(post("/api/attendance/checkin")
                        .with(request -> {
                            request.setRemoteAddr("192.168.1.93");
                            return request;
                        })
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"checkType\":\"IN\",\"deviceId\":\"DEV-012\",\"clientLongitude\":121.473700,\"clientLatitude\":31.230400,\"imageData\":\"face-image-continuous-multi-location\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("ABNORMAL"));

        Integer continuousConflictCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendanceException WHERE userId = ? AND sourceType = 'RULE' AND type = 'CONTINUOUS_MULTI_LOCATION_CONFLICT'",
                Integer.class,
                1001L
        );
        String description = jdbcTemplate.queryForObject(
                "SELECT description FROM attendanceException WHERE userId = ? AND sourceType = 'RULE' AND type = 'CONTINUOUS_MULTI_LOCATION_CONFLICT' ORDER BY id DESC LIMIT 1",
                String.class,
                1001L
        );

        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(1), continuousConflictCount);
        org.junit.jupiter.api.Assertions.assertTrue(description.contains("连续3次出现多地点冲突"));
    }

    @Test
    void shouldKeepMultiLocationDescriptionShortAndStoreFullEvidenceInDecisionTraceWhenLocationsAreLong() throws Exception {
        when(clock.instant()).thenReturn(LocalDateTime.of(2026, 3, 31, 9, 5, 0).atZone(ZoneId.of("Asia/Shanghai")).toInstant());
        String previousLocation = "前序地点-" + String.join("", Collections.nCopies(15, "超长园区定位片段"));
        String currentLocation = "当前地点-" + String.join("", Collections.nCopies(15, "超长园区定位片段"));
        jdbcTemplate.update(
                "UPDATE device SET longitude = ?, latitude = ? WHERE id = ?",
                new BigDecimal("116.397128"),
                new BigDecimal("39.916527"),
                "DEV-001"
        );
        insertDevice("DEV-010", "超长地点设备", currentLocation, 1, "用于长地点多地点异常触发");
        jdbcTemplate.update(
                "UPDATE device SET longitude = ?, latitude = ? WHERE id = ?",
                new BigDecimal("121.473701"),
                new BigDecimal("31.230416"),
                "DEV-010"
        );
        jdbcTemplate.update(
                "INSERT INTO attendanceRecord (id, userId, checkTime, checkType, deviceId, ipAddr, location, longitude, latitude, faceScore, status, createTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                1996L,
                1001L,
                "2026-03-31 08:59:00",
                "IN",
                "DEV-001",
                "192.168.1.51",
                previousLocation,
                new BigDecimal("116.397128"),
                new BigDecimal("39.916527"),
                new BigDecimal("98.90"),
                "NORMAL"
        );
        insertFaceFeature(1001L, "face-image-long-location", 1205L, "hash-long-location-1205");
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(post("/api/attendance/checkin")
                        .with(request -> {
                            request.setRemoteAddr("192.168.1.89");
                            return request;
                        })
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"checkType\":\"IN\",\"deviceId\":\"DEV-010\",\"clientLongitude\":121.473701,\"clientLatitude\":31.230416,\"imageData\":\"face-image-long-location\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("ABNORMAL"));

        Long exceptionId = jdbcTemplate.queryForObject(
                "SELECT id FROM attendanceException WHERE userId = ? AND sourceType = 'RULE' AND type = 'MULTI_LOCATION_CONFLICT' ORDER BY id DESC LIMIT 1",
                Long.class,
                1001L
        );
        String exceptionDescription = jdbcTemplate.queryForObject(
                "SELECT description FROM attendanceException WHERE id = ?",
                String.class,
                exceptionId
        );
        String ruleResult = jdbcTemplate.queryForObject(
                "SELECT ruleResult FROM decisionTrace WHERE businessType = 'ATTENDANCE_EXCEPTION' AND businessId = ? ORDER BY id DESC LIMIT 1",
                String.class,
                exceptionId
        );
        String decisionReason = jdbcTemplate.queryForObject(
                "SELECT decisionReason FROM decisionTrace WHERE businessType = 'ATTENDANCE_EXCEPTION' AND businessId = ? ORDER BY id DESC LIMIT 1",
                String.class,
                exceptionId
        );
        String traceEvidence = ruleResult + " | " + decisionReason;

        org.junit.jupiter.api.Assertions.assertEquals("短时间内在多个地点完成打卡，判定为空间冲突", exceptionDescription);
        org.junit.jupiter.api.Assertions.assertFalse(exceptionDescription.contains("前一条地点="));
        org.junit.jupiter.api.Assertions.assertTrue(traceEvidence.contains("前一条地点=" + previousLocation));
        org.junit.jupiter.api.Assertions.assertTrue(traceEvidence.contains("当前地点=" + currentLocation));
        org.junit.jupiter.api.Assertions.assertTrue(traceEvidence.contains("间隔分钟=6"));
        org.junit.jupiter.api.Assertions.assertTrue(traceEvidence.contains("实际距离米="));
        org.junit.jupiter.api.Assertions.assertTrue(traceEvidence.contains("阈值距离米=3000"));
        org.junit.jupiter.api.Assertions.assertTrue(traceEvidence.contains("窗口分钟=30"));
    }

    @Test
    void shouldAppendExplanationTraceWhenHistoricalMultiLocationTraceOnlyHasShortSummary() {
        jdbcTemplate.update(
                "UPDATE device SET longitude = ?, latitude = ? WHERE id = ?",
                new BigDecimal("116.397128"),
                new BigDecimal("39.916527"),
                "DEV-001"
        );
        insertDevice("DEV-011", "历史多地点设备", "上海园区", 1, "用于历史多地点异常补 trace");
        jdbcTemplate.update(
                "UPDATE device SET longitude = ?, latitude = ? WHERE id = ?",
                new BigDecimal("121.473701"),
                new BigDecimal("31.230416"),
                "DEV-011"
        );
        jdbcTemplate.update(
                "INSERT INTO attendanceRecord (id, userId, checkTime, checkType, deviceId, ipAddr, location, longitude, latitude, faceScore, status, createTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                1994L,
                1001L,
                "2026-03-31 08:59:00",
                "IN",
                "DEV-001",
                "192.168.1.40",
                "办公区A",
                new BigDecimal("116.397128"),
                new BigDecimal("39.916527"),
                new BigDecimal("98.60"),
                "NORMAL"
        );
        jdbcTemplate.update(
                "INSERT INTO attendanceRecord (id, userId, checkTime, checkType, deviceId, ipAddr, location, longitude, latitude, faceScore, status, createTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                1995L,
                1001L,
                "2026-03-31 09:05:00",
                "IN",
                "DEV-011",
                "192.168.1.41",
                "上海园区",
                new BigDecimal("121.473701"),
                new BigDecimal("31.230416"),
                new BigDecimal("98.70"),
                "ABNORMAL"
        );
        jdbcTemplate.update(
                "INSERT INTO attendanceException (id, recordId, userId, type, riskLevel, sourceType, description, processStatus, createTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                4001L,
                1995L,
                1001L,
                "MULTI_LOCATION_CONFLICT",
                "HIGH",
                "RULE",
                "短时间内在多个地点完成打卡，判定为空间冲突",
                "PENDING"
        );
        jdbcTemplate.update(
                "INSERT INTO decisionTrace (id, businessType, businessId, ruleResult, finalDecision, decisionReason) VALUES (?, ?, ?, ?, ?, ?)",
                5001L,
                "ATTENDANCE_EXCEPTION",
                4001L,
                "短时间内在多个地点完成打卡，判定为空间冲突",
                "MULTI_LOCATION_CONFLICT",
                "短时间内在多个地点完成打卡，判定为空间冲突"
        );

        RuleCheckDTO dto = new RuleCheckDTO();
        dto.setRecordId(1995L);
        exceptionAnalysisOrchestrator.ruleCheck(dto);

        Integer traceCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM decisionTrace WHERE businessType = 'ATTENDANCE_EXCEPTION' AND businessId = ?",
                Integer.class,
                4001L
        );
        String latestRuleResult = jdbcTemplate.queryForObject(
                "SELECT ruleResult FROM decisionTrace WHERE businessType = 'ATTENDANCE_EXCEPTION' AND businessId = ? ORDER BY id DESC LIMIT 1",
                String.class,
                4001L
        );
        String latestDecisionReason = jdbcTemplate.queryForObject(
                "SELECT decisionReason FROM decisionTrace WHERE businessType = 'ATTENDANCE_EXCEPTION' AND businessId = ? ORDER BY id DESC LIMIT 1",
                String.class,
                4001L
        );
        String latestTraceEvidence = latestRuleResult + " | " + latestDecisionReason;

        org.junit.jupiter.api.Assertions.assertTrue(traceCount >= 2);
        org.junit.jupiter.api.Assertions.assertTrue(latestTraceEvidence.contains("历史版本未持久化完整空间证据"));
        org.junit.jupiter.api.Assertions.assertTrue(latestTraceEvidence.contains("不做明细回填"));
        org.junit.jupiter.api.Assertions.assertFalse(latestTraceEvidence.contains("前一条地点="));
        org.junit.jupiter.api.Assertions.assertFalse(latestTraceEvidence.contains("当前地点="));
        org.junit.jupiter.api.Assertions.assertFalse(latestTraceEvidence.contains("间隔分钟="));
        org.junit.jupiter.api.Assertions.assertFalse(latestTraceEvidence.contains("实际距离米="));
    }

    @Test
    void shouldReturnCurrentUserRecordsFromRecordMe() throws Exception {
        insertAttendanceRecord(2001L, 1001L, "2026-03-31 08:59:00", "IN", "DEV-001", "办公区A", 98.50, "NORMAL");
        insertAttendanceRecord(2002L, 1001L, "2026-03-31 18:02:00", "OUT", "DEV-001", "办公区A", 98.80, "NORMAL");
        insertAttendanceRecord(2003L, 1002L, "2026-03-31 09:03:00", "IN", "DEV-001", "办公区A", 97.10, "NORMAL");
        jdbcTemplate.update(
                "INSERT INTO attendanceException (id, recordId, userId, type, riskLevel, sourceType, description, processStatus, createTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                4002L,
                2002L,
                1001L,
                "MULTI_LOCATION_CONFLICT",
                "HIGH",
                "RULE",
                "短时间内在多个地点完成打卡，判定为空间冲突",
                "PENDING"
        );
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(get("/api/attendance/record/me")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records.length()").value(2))
                .andExpect(jsonPath("$.data.records[0].userId").value(1001))
                .andExpect(jsonPath("$.data.records[0].exceptionType").value("MULTI_LOCATION_CONFLICT"));
    }

    @Test
    void shouldReturnSpecifiedUserRecordsForAdminFromCompatibleRecordPath() throws Exception {
        insertAttendanceRecord(2001L, 1001L, "2026-03-31 08:59:00", "IN", "DEV-001", "办公区A", 98.50, "NORMAL");
        insertAttendanceRecord(2002L, 1002L, "2026-03-31 09:03:00", "IN", "DEV-001", "办公区A", 97.10, "NORMAL");
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/attendance/record/1001")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].userId").value(1001));
    }

    @Test
    void shouldRejectEmployeeQueryingOtherUserRecords() throws Exception {
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(get("/api/attendance/record/1002")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权查看其他用户考勤记录"));
    }

    @Test
    void shouldReturnAttendanceListForAdminWithDeptFilter() throws Exception {
        insertAttendanceRecord(2001L, 1001L, "2026-03-31 08:59:00", "IN", "DEV-001", "办公区A", 98.50, "NORMAL");
        insertAttendanceRecord(2002L, 1002L, "2026-03-31 09:03:00", "IN", "DEV-001", "办公区A", 97.10, "NORMAL");
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/attendance/list")
                        .param("deptId", "1")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].userId").value(1001))
                .andExpect(jsonPath("$.data.records[0].realName").value("张三"));
    }

    @Test
    void shouldSubmitRepairRequestWhenRequestBodyOmitsUserId() throws Exception {
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(post("/api/attendance/repair")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"checkType\":\"IN\",\"checkTime\":\"2026-03-31 09:05:00\",\"repairReason\":\"设备故障未成功打卡\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.userId").value(1001))
                .andExpect(jsonPath("$.data.checkType").value("IN"))
                .andExpect(jsonPath("$.data.repairReason").value("设备故障未成功打卡"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        Integer repairLogCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM operationLog WHERE userId = ? AND type = 'REPAIR'",
                Integer.class,
                1001L
        );
        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(1), repairLogCount);
    }

    @Test
    void shouldIgnoreRequestUserIdWhenCheckingDuplicatePendingRepairRequest() throws Exception {
        insertAttendanceRepair(3001L, 1001L, "IN", "2026-03-31 09:05:00", "设备故障未成功打卡", "PENDING");
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(post("/api/attendance/repair")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":1002,\"checkType\":\"IN\",\"checkTime\":\"2026-03-31 09:05:00\",\"repairReason\":\"设备故障未成功打卡\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("补卡申请已存在，请勿重复提交"));
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
                "INSERT INTO device (id, name, location, longitude, latitude, radiusMeters, status, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                id,
                name,
                location,
                (location != null && (location.contains("上海") || location.contains("外部"))) ? new BigDecimal("121.473701") : new BigDecimal("116.397128"),
                (location != null && (location.contains("上海") || location.contains("外部"))) ? new BigDecimal("31.230416") : new BigDecimal("39.916527"),
                30,
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

    private void insertRiskLevel(Long id, String code, String name, String description, int status) {
        jdbcTemplate.update(
                "INSERT INTO riskLevel (id, code, name, description, status, createTime, updateTime) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                id,
                code,
                name,
                description,
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

    private ModelInvokeResponse mockModelResponse() {
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

    private ModelInvokeResponse mockGenericModelResponse() {
        ModelInvokeResponse response = new ModelInvokeResponse();
        response.setConclusion("DEVICE_RISK");
        response.setRiskLevel("HIGH");
        response.setConfidenceScore(new BigDecimal("89.50"));
        response.setDecisionReason("设备与历史行为偏离较大");
        response.setReasonSummary("模型识别当前存在设备行为异常风险");
        response.setActionSuggestion("建议优先人工复核设备与地点信息");
        response.setSimilarCaseSummary("存在相似设备异常风险案例");
        response.setRawResponse("{\"conclusion\":\"DEVICE_RISK\"}");
        return response;
    }

    private void insertFaceFeature(Long userId, String imageData, Long id, String featureHash) {
        jdbcTemplate.update(
                "INSERT INTO faceFeature (id, userId, featureData, featureHash, encryptFlag, createTime) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                id,
                userId,
                DigestUtils.md5DigestAsHex(imageData.getBytes(StandardCharsets.UTF_8)),
                featureHash,
                1
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
                                           String processStatus,
                                           String createTime) {
        jdbcTemplate.update(
                "INSERT INTO attendanceException (id, recordId, userId, type, riskLevel, sourceType, description, processStatus, createTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id,
                recordId,
                userId,
                type,
                riskLevel,
                sourceType,
                description,
                processStatus,
                createTime
        );
    }

    private void insertAttendanceRepair(Long id,
                                        Long userId,
                                        String checkType,
                                        String checkTime,
                                        String repairReason,
                                        String status) {
        jdbcTemplate.update(
                "INSERT INTO attendanceRepair (id, userId, checkType, checkTime, repairReason, status, recordId, createTime) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                id,
                userId,
                checkType,
                checkTime,
                repairReason,
                status,
                null
        );
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
