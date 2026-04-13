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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class RoleManagementIntegrationTest {

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
        jdbcTemplate.execute("DELETE FROM attendanceRecord");
        jdbcTemplate.execute("DELETE FROM faceFeature");
        jdbcTemplate.execute("DELETE FROM device");
        jdbcTemplate.execute("DELETE FROM user");
        jdbcTemplate.execute("DELETE FROM department");
        jdbcTemplate.execute("DELETE FROM role");

        insertRole(1L, "ADMIN", "管理员", "系统管理员", 1);
        insertRole(2L, "EMPLOYEE", "员工", "普通员工", 1);
        insertRole(3L, "HR", "人事专员", "负责人事与考勤", 1);

        insertDepartment(1L, "管理部", "负责系统管理");
        insertDepartment(2L, "技术部", "负责技术研发");

        insertUser(9001L, "admin", "系统管理员", 1L, 1L, 1);
        insertUser(1001L, "zhangsan", "张三", 2L, 2L, 1);
    }

    @Test
    void shouldReturnRoleListForAdmin() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/role/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].code").value("ADMIN"))
                .andExpect(jsonPath("$.data[1].code").value("EMPLOYEE"))
                .andExpect(jsonPath("$.data[2].code").value("HR"));
    }

    @Test
    void shouldFilterRoleListByKeywordAndStatus() throws Exception {
        String token = loginAndExtractToken("admin", "123456");
        jdbcTemplate.update("UPDATE role SET status = 0 WHERE id = ?", 3L);

        mockMvc.perform(get("/api/role/list")
                        .param("keyword", "人事")
                        .param("status", "0")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].code").value("HR"))
                .andExpect(jsonPath("$.data[0].status").value(0));
    }

    @Test
    void shouldAddRoleWhenInputIsValid() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/role/add")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"code\":\"  AUDITOR  \",\"name\":\"  审计员  \",\"description\":\"  负责审计  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.code").value("AUDITOR"))
                .andExpect(jsonPath("$.data.name").value("审计员"))
                .andExpect(jsonPath("$.data.description").value("负责审计"))
                .andExpect(jsonPath("$.data.status").value(1));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM role WHERE code = ?",
                Integer.class,
                "AUDITOR"
        );
        assertEquals(1, count);
    }

    @Test
    void shouldFailAddRoleWhenCodeIsBlank() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/role/add")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"code\":\"   \",\"name\":\"审计员\",\"description\":\"负责审计\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("角色编码不能为空"));
    }

    @Test
    void shouldFailAddRoleWhenCodeAlreadyExists() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/role/add")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"code\":\"  ADMIN  \",\"name\":\"管理员副本\",\"description\":\"重复\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("角色编码已存在"));
    }

    @Test
    void shouldUpdateRoleWhenInputIsValid() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(put("/api/role/update")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":3,\"code\":\"HR\",\"name\":\"人事主管\",\"description\":\"负责人事与复核\",\"status\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(3))
                .andExpect(jsonPath("$.data.name").value("人事主管"))
                .andExpect(jsonPath("$.data.status").value(0));

        String name = jdbcTemplate.queryForObject(
                "SELECT name FROM role WHERE id = ?",
                String.class,
                3L
        );
        assertEquals("人事主管", name);
    }

    @Test
    void shouldFailUpdateRoleWhenRoleDoesNotExist() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(put("/api/role/update")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":99,\"code\":\"AUDITOR\",\"name\":\"审计员\",\"description\":\"负责审计\",\"status\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("角色不存在"));
    }

    @Test
    void shouldFailUpdateRoleWhenStatusIsInvalid() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(put("/api/role/update")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":3,\"code\":\"HR\",\"name\":\"人事主管\",\"description\":\"负责人事\",\"status\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("角色状态不合法"));
    }

    @Test
    void shouldDeleteRoleWhenNoUserReferencesIt() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(delete("/api/role/3")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM role WHERE id = ?",
                Integer.class,
                3L
        );
        assertEquals(0, count);
    }

    @Test
    void shouldFailDeleteRoleWhenRoleIsReferenced() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(delete("/api/role/2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("角色下存在关联用户，不能删除"));
    }

    @Test
    void shouldReturnUnauthorizedWhenAccessingRoleApiWithoutToken() throws Exception {
        mockMvc.perform(get("/api/role/list"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("unauthorized"));
    }

    @Test
    void shouldReturnForbiddenWhenEmployeeAccessesRoleApi() throws Exception {
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(get("/api/role/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("forbidden"));
    }

    @Test
    void shouldReturnBadRequestWhenRoleRequestJsonIsMalformed() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/role/add")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"code\":\"AUDITOR\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请求参数错误"));
    }

    private void insertRole(Long id, String code, String name, String description, Integer status) {
        jdbcTemplate.update(
                "INSERT INTO role (id, code, name, description, status) VALUES (?, ?, ?, ?, ?)",
                id,
                code,
                name,
                description,
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

    private void insertUser(Long id, String username, String realName, Long deptId, Long roleId, Integer status) {
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
