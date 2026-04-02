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
class DepartmentManagementIntegrationTest {

    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM user");
        jdbcTemplate.execute("DELETE FROM department");
        jdbcTemplate.execute("DELETE FROM role");

        jdbcTemplate.update(
                "INSERT INTO role (id, code, name, description, status) VALUES (?, ?, ?, ?, ?)",
                1L,
                "ADMIN",
                "管理员",
                "系统管理员",
                1
        );
        jdbcTemplate.update(
                "INSERT INTO role (id, code, name, description, status) VALUES (?, ?, ?, ?, ?)",
                2L,
                "EMPLOYEE",
                "员工",
                "普通员工",
                1
        );

        insertDepartment(1L, "研发部", "负责系统研发");
        insertDepartment(2L, "人事部", "负责人力资源");
        insertDepartment(3L, "行政部", "负责日常行政");

        insertUser(9001L, "admin", "系统管理员", 1L, 1L);
        insertUser(1001L, "zhangsan", "张三", 2L, 2L);
    }

    @Test
    void shouldReturnDepartmentListForAdmin() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/department/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].name").value("研发部"));
    }

    @Test
    void shouldFilterDepartmentListByKeyword() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/department/list")
                        .param("keyword", "人")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("人事部"));
    }

    @Test
    void shouldAddDepartmentWhenNameIsUnique() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/department/add")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"财务部\",\"description\":\"负责财务管理\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("财务部"))
                .andExpect(jsonPath("$.data.description").value("负责财务管理"));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM department WHERE name = ?",
                Integer.class,
                "财务部"
        );
        assertEquals(1, count);
    }

    @Test
    void shouldFailAddDepartmentWhenNameIsBlankAfterTrim() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/department/add")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"   \",\"description\":\"无效部门\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("部门名称不能为空"));
    }

    @Test
    void shouldFailAddDepartmentWhenNameAlreadyExistsAfterTrim() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/department/add")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"  研发部  \",\"description\":\"重复部门\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("部门名称已存在"));
    }

    @Test
    void shouldUpdateDepartmentWhenInputIsValid() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(put("/api/department/update")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":2,\"name\":\"人力资源部\",\"description\":\"负责人力资源与培训\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.name").value("人力资源部"))
                .andExpect(jsonPath("$.data.description").value("负责人力资源与培训"));

        String name = jdbcTemplate.queryForObject(
                "SELECT name FROM department WHERE id = ?",
                String.class,
                2L
        );
        assertEquals("人力资源部", name);
    }

    @Test
    void shouldFailUpdateDepartmentWhenDepartmentDoesNotExist() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(put("/api/department/update")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":99,\"name\":\"不存在的部门\",\"description\":\"无效\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("部门不存在"));
    }

    @Test
    void shouldFailUpdateDepartmentWhenNameIsBlankAfterTrim() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(put("/api/department/update")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":2,\"name\":\"   \",\"description\":\"无效\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("部门名称不能为空"));
    }

    @Test
    void shouldFailUpdateDepartmentWhenNameAlreadyExistsAfterTrim() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(put("/api/department/update")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":2,\"name\":\"  研发部  \",\"description\":\"重复\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("部门名称已存在"));
    }

    @Test
    void shouldDeleteDepartmentWhenNoUserReferencesIt() throws Exception {
        String token = loginAndExtractToken("admin", "123456");
        insertDepartment(4L, "财务部", "负责财务管理");

        mockMvc.perform(delete("/api/department/4")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM department WHERE id = ?",
                Integer.class,
                4L
        );
        assertEquals(0, count);
    }

    @Test
    void shouldFailDeleteDepartmentWhenDepartmentDoesNotExist() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(delete("/api/department/99")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("部门不存在"));
    }

    @Test
    void shouldFailDeleteDepartmentWhenDepartmentIsReferenced() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(delete("/api/department/2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("部门下存在关联用户，不能删除"));
    }

    @Test
    void shouldReturnUnauthorizedWhenAccessingDepartmentApiWithoutToken() throws Exception {
        mockMvc.perform(get("/api/department/list"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("unauthorized"));
    }

    @Test
    void shouldReturnForbiddenWhenEmployeeAccessesDepartmentApi() throws Exception {
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(get("/api/department/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("forbidden"));
    }

    private void insertDepartment(Long id, String name, String description) {
        jdbcTemplate.update(
                "INSERT INTO department (id, name, description) VALUES (?, ?, ?)",
                id,
                name,
                description
        );
    }

    private void insertUser(Long id, String username, String realName, Long deptId, Long roleId) {
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
                1
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
