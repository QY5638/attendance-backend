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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class DeviceManagementIntegrationTest {

    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM reviewRecord");
        jdbcTemplate.execute("DELETE FROM decisionTrace");
        jdbcTemplate.execute("DELETE FROM modelCallLog");
        jdbcTemplate.execute("DELETE FROM exceptionAnalysis");
        jdbcTemplate.execute("DELETE FROM attendanceException");
        jdbcTemplate.execute("DELETE FROM attendanceRepair");
        jdbcTemplate.execute("DELETE FROM attendanceRecord");
        jdbcTemplate.execute("DELETE FROM device");
        jdbcTemplate.execute("DELETE FROM user");
        jdbcTemplate.execute("DELETE FROM department");
        jdbcTemplate.execute("DELETE FROM role");

        insertRole(1L, "ADMIN", "管理员");
        insertRole(2L, "EMPLOYEE", "员工");

        insertDepartment(1L, "研发部", "负责系统研发");

        insertUser(9001L, "admin", "系统管理员", 1L, 1L, 1);
        insertUser(1001L, "zhangsan", "张三", 1L, 2L, 1);

        insertDevice("DEV-001", "前台考勤机1", "办公区A", 1, "默认正常设备");
        insertDevice("DEV-002", "前台考勤机2", "办公区B", 1, "默认正常设备");
        insertDevice("DEV-009", "临时设备", "外部区域", 0, "临时停用设备");
    }

    @Test
    void shouldReturnDeviceListOrderedByDeviceId() throws Exception {
        String token = loginAndExtractToken("admin", "123456");
        jdbcTemplate.update(
                "UPDATE device SET longitude = ?, latitude = ? WHERE id = ?",
                new BigDecimal("116.397128"),
                new BigDecimal("39.916527"),
                "DEV-001"
        );

        mockMvc.perform(get("/api/device/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].deviceId").value("DEV-001"))
                .andExpect(jsonPath("$.data[0].longitude").value(116.397128))
                .andExpect(jsonPath("$.data[0].latitude").value(39.916527))
                .andExpect(jsonPath("$.data[1].deviceId").value("DEV-002"))
                .andExpect(jsonPath("$.data[2].deviceId").value("DEV-009"))
                .andExpect(jsonPath("$.data[0].id").doesNotExist());
    }

    @Test
    void shouldFilterDeviceListByKeywordAndStatus() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/device/list")
                        .param("keyword", "办公区")
                        .param("status", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].deviceId").value("DEV-001"))
                .andExpect(jsonPath("$.data[1].deviceId").value("DEV-002"));
    }

    @Test
    void shouldAddDeviceWhenInputIsValid() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/device/add")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"deviceId\":\"  DEV-010  \",\"name\":\"  后门考勤机  \",\"location\":\"  办公区C  \",\"longitude\":116.397128,\"latitude\":39.916527,\"description\":\"  新增设备  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.deviceId").value("DEV-010"))
                .andExpect(jsonPath("$.data.name").value("后门考勤机"))
                .andExpect(jsonPath("$.data.location").value("办公区C"))
                .andExpect(jsonPath("$.data.longitude").value(116.397128))
                .andExpect(jsonPath("$.data.latitude").value(39.916527))
                .andExpect(jsonPath("$.data.status").value(1))
                .andExpect(jsonPath("$.data.description").value("新增设备"))
                .andExpect(jsonPath("$.data.id").doesNotExist());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM device WHERE id = ?",
                Integer.class,
                "DEV-010"
        );
        String name = jdbcTemplate.queryForObject(
                "SELECT name FROM device WHERE id = ?",
                String.class,
                "DEV-010"
        );
        Integer statusValue = jdbcTemplate.queryForObject(
                "SELECT status FROM device WHERE id = ?",
                Integer.class,
                "DEV-010"
        );
        BigDecimal longitude = jdbcTemplate.queryForObject(
                "SELECT longitude FROM device WHERE id = ?",
                BigDecimal.class,
                "DEV-010"
        );
        BigDecimal latitude = jdbcTemplate.queryForObject(
                "SELECT latitude FROM device WHERE id = ?",
                BigDecimal.class,
                "DEV-010"
        );

        assertEquals(1, count);
        assertEquals("后门考勤机", name);
        assertEquals(1, statusValue);
        assertEquals(new BigDecimal("116.397128"), longitude);
        assertEquals(new BigDecimal("39.916527"), latitude);
    }

    @Test
    void shouldFailAddDeviceWhenDeviceIdAlreadyExists() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/device/add")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"deviceId\":\"DEV-001\",\"name\":\"重复设备\",\"location\":\"办公区A\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("地点编号已存在"));
    }

    @Test
    void shouldFailAddDeviceWhenDeviceIdIsBlank() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/device/add")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"deviceId\":\"   \",\"name\":\"新增设备\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("地点编号不能为空"));
    }

    @Test
    void shouldFailAddDeviceWhenNameIsBlank() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/device/add")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"deviceId\":\"DEV-011\",\"name\":\"   \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("管理名称不能为空"));
    }

    @Test
    void shouldReturnBadRequestWhenAddDeviceRequestBodyIsMalformed() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/device/add")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"deviceId\":\"DEV-011\",\"name\":}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请求参数错误"));
    }

    @Test
    void shouldFailAddDeviceWhenStatusIsInvalid() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/device/add")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"deviceId\":\"DEV-011\",\"name\":\"新增设备\",\"status\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("地点状态不合法"));
    }

    @Test
    void shouldUpdateDeviceWhenInputIsValid() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(put("/api/device/update")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"deviceId\":\"DEV-002\",\"name\":\"  二号考勤机  \",\"location\":\"  办公区B-北侧  \",\"longitude\":121.473701,\"latitude\":31.230416,\"status\":0,\"description\":\"  调整位置  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.deviceId").value("DEV-002"))
                .andExpect(jsonPath("$.data.name").value("二号考勤机"))
                .andExpect(jsonPath("$.data.location").value("办公区B-北侧"))
                .andExpect(jsonPath("$.data.longitude").value(121.473701))
                .andExpect(jsonPath("$.data.latitude").value(31.230416))
                .andExpect(jsonPath("$.data.status").value(0))
                .andExpect(jsonPath("$.data.description").value("调整位置"));

        String name = jdbcTemplate.queryForObject(
                "SELECT name FROM device WHERE id = ?",
                String.class,
                "DEV-002"
        );
        String location = jdbcTemplate.queryForObject(
                "SELECT location FROM device WHERE id = ?",
                String.class,
                "DEV-002"
        );
        Integer statusValue = jdbcTemplate.queryForObject(
                "SELECT status FROM device WHERE id = ?",
                Integer.class,
                "DEV-002"
        );
        BigDecimal longitude = jdbcTemplate.queryForObject(
                "SELECT longitude FROM device WHERE id = ?",
                BigDecimal.class,
                "DEV-002"
        );
        BigDecimal latitude = jdbcTemplate.queryForObject(
                "SELECT latitude FROM device WHERE id = ?",
                BigDecimal.class,
                "DEV-002"
        );

        assertEquals("二号考勤机", name);
        assertEquals("办公区B-北侧", location);
        assertEquals(0, statusValue);
        assertEquals(new BigDecimal("121.473701"), longitude);
        assertEquals(new BigDecimal("31.230416"), latitude);
    }

    @Test
    void shouldFailUpdateDeviceWhenDeviceIdIsBlank() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(put("/api/device/update")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"deviceId\":\"   \",\"name\":\"二号考勤机\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("地点编号不能为空"));
    }

    @Test
    void shouldFailUpdateDeviceWhenNameIsBlank() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(put("/api/device/update")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"deviceId\":\"DEV-002\",\"name\":\"   \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("管理名称不能为空"));
    }

    @Test
    void shouldReturnBadRequestWhenUpdateDeviceRequestBodyIsMalformed() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(put("/api/device/update")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"deviceId\":\"DEV-002\",\"name\":}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请求参数错误"));
    }

    @Test
    void shouldFailUpdateDeviceWhenStatusIsInvalid() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(put("/api/device/update")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"deviceId\":\"DEV-002\",\"name\":\"二号考勤机\",\"status\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("地点状态不合法"));
    }

    @Test
    void shouldUpdateDeviceStatusWhenInputIsValid() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(put("/api/device/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"deviceId\":\"DEV-009\",\"status\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.deviceId").value("DEV-009"))
                .andExpect(jsonPath("$.data.status").value(1));

        Integer statusValue = jdbcTemplate.queryForObject(
                "SELECT status FROM device WHERE id = ?",
                Integer.class,
                "DEV-009"
        );
        assertEquals(1, statusValue);
    }

    @Test
    void shouldFailUpdateDeviceStatusWhenStatusIsInvalid() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(put("/api/device/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"deviceId\":\"DEV-009\",\"status\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("地点状态不合法"));
    }

    @Test
    void shouldFailUpdateDeviceStatusWhenDeviceIdIsBlank() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(put("/api/device/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"deviceId\":\"   \",\"status\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("地点编号不能为空"));
    }

    @Test
    void shouldReturnBadRequestWhenUpdateDeviceStatusRequestBodyIsMalformed() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(put("/api/device/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"deviceId\":\"DEV-009\",\"status\":}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请求参数错误"));
    }

    @Test
    void shouldDeleteDeviceWhenNotReferenced() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(delete("/api/device/DEV-002")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM device WHERE id = ?",
                Integer.class,
                "DEV-002"
        );
        assertEquals(0, count);
    }

    @Test
    void shouldFailDeleteDeviceWhenDeviceDoesNotExist() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(delete("/api/device/DEV-404")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("打卡地点不存在"));
    }

    @Test
    void shouldFailDeleteDeviceWhenReferencedByAttendanceRecord() throws Exception {
        String token = loginAndExtractToken("admin", "123456");
        insertAttendanceRecord(2001L, 1001L, "DEV-001");

        mockMvc.perform(delete("/api/device/DEV-001")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("打卡地点已关联考勤记录，不能删除，请先停用地点"));
    }

    @Test
    void shouldReturnUnauthorizedWhenAccessingDeviceApiWithoutToken() throws Exception {
        mockMvc.perform(get("/api/device/list"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("unauthorized"));
    }

    @Test
    void shouldReturnForbiddenWhenEmployeeAccessesDeviceApi() throws Exception {
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(get("/api/device/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("forbidden"));
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

    private void insertAttendanceRecord(Long id, Long userId, String deviceId) {
        jdbcTemplate.update(
                "INSERT INTO attendanceRecord (id, userId, checkTime, checkType, deviceId, ipAddr, location, faceScore, status, createTime) VALUES (?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                id,
                userId,
                "IN",
                deviceId,
                "127.0.0.1",
                "办公区A",
                98.50,
                "NORMAL"
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
