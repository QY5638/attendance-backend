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
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

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

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM operationLog");
        jdbcTemplate.execute("DELETE FROM reviewRecord");
        jdbcTemplate.execute("DELETE FROM decisionTrace");
        jdbcTemplate.execute("DELETE FROM modelCallLog");
        jdbcTemplate.execute("DELETE FROM exceptionAnalysis");
        jdbcTemplate.execute("DELETE FROM attendanceException");
        jdbcTemplate.execute("DELETE FROM attendanceRepair");
        jdbcTemplate.execute("DELETE FROM faceFeature");
        jdbcTemplate.execute("DELETE FROM attendanceRecord");
        jdbcTemplate.execute("DELETE FROM device");
        jdbcTemplate.execute("DELETE FROM user");
        jdbcTemplate.execute("DELETE FROM department");
        jdbcTemplate.execute("DELETE FROM role");

        insertRole(1L, "ADMIN", "管理员");
        insertRole(2L, "EMPLOYEE", "员工");

        insertDepartment(1L, "技术部", "负责系统研发");
        insertDepartment(2L, "行政部", "负责行政支持");

        insertUser(9001L, "admin", "系统管理员", 1L, 1L, 1);
        insertUser(1001L, "zhangsan", "张三", 1L, 2L, 1);
        insertUser(1002L, "lisi", "李四", 2L, 2L, 1);

        insertDevice("DEV-001", "前台考勤机", "办公区A", 1, "正常设备");
        insertDevice("DEV-002", "后门考勤机", "办公区B", 0, "停用设备");
    }

    @Test
    void shouldCheckInWhenFaceVerifyPasses() throws Exception {
        insertFaceFeature(1001L, "face-image-checkin-success", 1101L, "hash-success-1101");
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(post("/api/attendance/checkin")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":1001,\"checkType\":\"IN\",\"deviceId\":\"DEV-001\",\"ipAddr\":\"192.168.1.11\",\"location\":\"办公区A\",\"imageData\":\"face-image-checkin-success\"}"))
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
    }

    @Test
    void shouldRejectCheckInWhenFaceIsNotRegistered() throws Exception {
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(post("/api/attendance/checkin")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":1001,\"checkType\":\"IN\",\"deviceId\":\"DEV-001\",\"ipAddr\":\"192.168.1.11\",\"location\":\"办公区A\",\"imageData\":\"face-image-not-registered\"}"))
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
                        .content("{\"userId\":1001,\"checkType\":\"IN\",\"deviceId\":\"DEV-001\",\"ipAddr\":\"192.168.1.11\",\"location\":\"办公区A\",\"imageData\":\"face-image-other\"}"))
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
                        .content("{\"userId\":1001,\"checkType\":\"IN\",\"deviceId\":\"DEV-002\",\"ipAddr\":\"192.168.1.12\",\"location\":\"办公区B\",\"imageData\":\"face-image-disabled-device\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("设备已停用，不能打卡"));
    }

    @Test
    void shouldRejectEmployeeCheckInForOtherUser() throws Exception {
        insertFaceFeature(1002L, "face-image-for-lisi", 1104L, "hash-lisi-1104");
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(post("/api/attendance/checkin")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":1002,\"checkType\":\"IN\",\"deviceId\":\"DEV-001\",\"ipAddr\":\"192.168.1.21\",\"location\":\"办公区A\",\"imageData\":\"face-image-for-lisi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权为其他用户提交打卡"));
    }

    @Test
    void shouldReturnOwnAttendanceRecordsForEmployee() throws Exception {
        insertAttendanceRecord(2001L, 1001L, "2026-03-31 08:59:00", "IN", "DEV-001", "办公区A", 98.50, "NORMAL");
        insertAttendanceRecord(2002L, 1001L, "2026-03-31 18:02:00", "OUT", "DEV-001", "办公区A", 98.80, "NORMAL");
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(get("/api/attendance/record/1001")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records.length()").value(2))
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
    void shouldSubmitRepairRequest() throws Exception {
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(post("/api/attendance/repair")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":1001,\"checkType\":\"IN\",\"checkTime\":\"2026-03-31 09:05:00\",\"repairReason\":\"设备故障未成功打卡\"}"))
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
    void shouldRejectDuplicatePendingRepairRequest() throws Exception {
        insertAttendanceRepair(3001L, 1001L, "IN", "2026-03-31 09:05:00", "设备故障未成功打卡", "PENDING");
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(post("/api/attendance/repair")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":1001,\"checkType\":\"IN\",\"checkTime\":\"2026-03-31 09:05:00\",\"repairReason\":\"设备故障未成功打卡\"}"))
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
                "INSERT INTO device (id, name, location, status, description) VALUES (?, ?, ?, ?, ?)",
                id,
                name,
                location,
                status,
                description
        );
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
