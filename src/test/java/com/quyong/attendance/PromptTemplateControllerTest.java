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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PromptTemplateControllerTest {

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

        insertRole(1L, "ADMIN", "管理员", 1);
        insertRole(2L, "EMPLOYEE", "员工", 1);
        insertDepartment(1L, "管理部", "负责系统管理");
        insertUser(9001L, "admin", "系统管理员", 1L, 1L, 1);
        insertUser(1001L, "zhangsan", "张三", 1L, 2L, 1);
    }

    @Test
    void shouldReturnPromptTemplateListBySceneTypeAndStatus() throws Exception {
        insertPromptTemplate(8001L, "COMPLEX_EXCEPTION", "复杂异常分析模板", "EXCEPTION_ANALYSIS", "v1.0", "模板内容1", "ENABLED", "默认模板", "2026-03-26 09:00:00");
        insertPromptTemplate(8002L, "WARNING_BRIEF", "预警摘要模板", "WARNING_ADVICE", "v1.0", "模板内容2", "DISABLED", "预警模板", "2026-03-26 09:05:00");
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/system/prompt/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("sceneType", "EXCEPTION_ANALYSIS")
                        .param("status", "ENABLED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].code").value("COMPLEX_EXCEPTION"))
                .andExpect(jsonPath("$.data.records[0].sceneType").value("EXCEPTION_ANALYSIS"));
    }

    @Test
    void shouldAddPromptTemplateWhenInputIsValid() throws Exception {
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/system/prompt/add")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"code\":\"COMPLEX_EXCEPTION\",\"name\":\"复杂异常分析模板\",\"sceneType\":\"EXCEPTION_ANALYSIS\",\"version\":\"v1.1\",\"content\":\"请输出结构化结论\",\"status\":\"ENABLED\",\"remark\":\"默认模板\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM promptTemplate WHERE code = ? AND version = ?",
                Integer.class,
                "COMPLEX_EXCEPTION",
                "v1.1"
        );
        assertEquals(1, count);
    }

    @Test
    void shouldFailAddPromptTemplateWhenCodeVersionAlreadyExists() throws Exception {
        insertPromptTemplate(8001L, "COMPLEX_EXCEPTION", "复杂异常分析模板", "EXCEPTION_ANALYSIS", "v1.0", "模板内容1", "ENABLED", "默认模板", "2026-03-26 09:00:00");
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/system/prompt/add")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"code\":\"COMPLEX_EXCEPTION\",\"name\":\"复杂异常分析模板\",\"sceneType\":\"EXCEPTION_ANALYSIS\",\"version\":\"v1.0\",\"content\":\"请输出结构化结论\",\"status\":\"ENABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("提示词模板版本已存在"));
    }

    @Test
    void shouldUpdatePromptTemplate() throws Exception {
        insertPromptTemplate(8001L, "COMPLEX_EXCEPTION", "复杂异常分析模板", "EXCEPTION_ANALYSIS", "v1.0", "模板内容1", "ENABLED", "默认模板", "2026-03-26 09:00:00");
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(put("/api/system/prompt/update")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":8001,\"code\":\"COMPLEX_EXCEPTION\",\"name\":\"复杂异常分析模板-新版\",\"sceneType\":\"EXCEPTION_ANALYSIS\",\"version\":\"v1.0\",\"content\":\"新的模板内容\",\"status\":\"ENABLED\",\"remark\":\"更新后\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        String name = jdbcTemplate.queryForObject(
                "SELECT name FROM promptTemplate WHERE id = ?",
                String.class,
                8001L
        );
        assertEquals("复杂异常分析模板-新版", name);
    }

    @Test
    void shouldUpdatePromptTemplateStatus() throws Exception {
        insertPromptTemplate(8001L, "COMPLEX_EXCEPTION", "复杂异常分析模板", "EXCEPTION_ANALYSIS", "v1.0", "模板内容1", "ENABLED", "默认模板", "2026-03-26 09:00:00");
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(put("/api/system/prompt/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":8001,\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        String statusValue = jdbcTemplate.queryForObject(
                "SELECT status FROM promptTemplate WHERE id = ?",
                String.class,
                8001L
        );
        assertEquals("DISABLED", statusValue);
    }

    @Test
    void shouldReturnForbiddenWhenEmployeeAccessesPromptApi() throws Exception {
        String employeeToken = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(get("/api/system/prompt/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void shouldReturnUnauthorizedWhenRequestHasNoTokenForPromptApi() throws Exception {
        mockMvc.perform(get("/api/system/prompt/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    private void insertRole(Long id, String code, String name, Integer status) {
        jdbcTemplate.update(
                "INSERT INTO role (id, code, name, description, status) VALUES (?, ?, ?, ?, ?)",
                id,
                code,
                name,
                name + "角色",
                status
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

    private void insertPromptTemplate(Long id,
                                      String code,
                                      String name,
                                      String sceneType,
                                      String version,
                                      String content,
                                      String status,
                                      String remark,
                                      String updateTime) {
        jdbcTemplate.update(
                "INSERT INTO promptTemplate (id, code, name, sceneType, version, content, status, remark, createTime, updateTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?)",
                id,
                code,
                name,
                sceneType,
                version,
                content,
                status,
                remark,
                updateTime
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
