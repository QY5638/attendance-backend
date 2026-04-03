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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
class UserManagementIntegrationTest {

    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM attendanceRepair");
        jdbcTemplate.execute("DELETE FROM attendanceRecord");
        jdbcTemplate.execute("DELETE FROM device");
        jdbcTemplate.execute("DELETE FROM user");
        jdbcTemplate.execute("DELETE FROM department");
        jdbcTemplate.execute("DELETE FROM role");

        insertRole(1L, "ADMIN", "管理员");
        insertRole(2L, "EMPLOYEE", "员工");

        insertDepartment(1L, "研发部", "负责系统研发");
        insertDepartment(2L, "人事部", "负责人力资源");

        insertUser(9001L, "admin", "系统管理员", 1L, 1L, 1);
        insertUser(1003L, "charlie", "赵六", 2L, 2L, 1);
        insertUser(1001L, "alice", "王五", 1L, 2L, 1);
        insertUser(1002L, "bob", "李四", 2L, 2L, 0);
    }

    @Test
    void shouldReturnUserListOrderedByIdForAdmin() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/user/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].id").value(1001))
                .andExpect(jsonPath("$.data[0].username").value("alice"))
                .andExpect(jsonPath("$.data[1].id").value(1002))
                .andExpect(jsonPath("$.data[2].id").value(1003))
                .andExpect(jsonPath("$.data[3].id").value(9001));
    }

    @Test
    void shouldFilterUserListByUsernameKeyword() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/user/list")
                        .param("keyword", "ali")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].username").value("alice"))
                .andExpect(jsonPath("$.data[0].realName").value("王五"));
    }

    @Test
    void shouldFilterUserListByRealNameKeyword() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/user/list")
                        .param("keyword", "赵")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].username").value("charlie"))
                .andExpect(jsonPath("$.data[0].realName").value("赵六"));
    }

    @Test
    void shouldFilterUserListByDeptIdAndStatus() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/user/list")
                        .param("deptId", "2")
                        .param("status", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(1003))
                .andExpect(jsonPath("$.data[0].username").value("charlie"))
                .andExpect(jsonPath("$.data[0].deptId").value(2))
                .andExpect(jsonPath("$.data[0].status").value(1));
    }

    @Test
    void shouldAddUserWhenInputIsValid() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/user/add")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"  david  \",\"password\":\"123456\",\"realName\":\"  张三  \",\"gender\":\"男\",\"phone\":\" 13900000001 \",\"deptId\":1,\"roleId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.username").value("david"))
                .andExpect(jsonPath("$.data.realName").value("张三"))
                .andExpect(jsonPath("$.data.phone").value("13900000001"))
                .andExpect(jsonPath("$.data.deptId").value(1))
                .andExpect(jsonPath("$.data.roleId").value(2))
                .andExpect(jsonPath("$.data.status").value(1));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user WHERE username = ?",
                Integer.class,
                "david"
        );
        String encodedPassword = jdbcTemplate.queryForObject(
                "SELECT password FROM user WHERE username = ?",
                String.class,
                "david"
        );
        String realName = jdbcTemplate.queryForObject(
                "SELECT realName FROM user WHERE username = ?",
                String.class,
                "david"
        );
        String phone = jdbcTemplate.queryForObject(
                "SELECT phone FROM user WHERE username = ?",
                String.class,
                "david"
        );
        Integer statusValue = jdbcTemplate.queryForObject(
                "SELECT status FROM user WHERE username = ?",
                Integer.class,
                "david"
        );

        assertEquals(1, count);
        assertNotEquals("123456", encodedPassword);
        assertTrue(PASSWORD_ENCODER.matches("123456", encodedPassword));
        assertEquals("张三", realName);
        assertEquals("13900000001", phone);
        assertEquals(1, statusValue);
    }

    @Test
    void shouldFailAddUserWhenRequestBodyIsEmpty() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/user/add")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户名不能为空"));
    }

    @Test
    void shouldFailAddUserWhenUsernameIsBlank() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/user/add")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"   \",\"password\":\"123456\",\"realName\":\"张三\",\"deptId\":1,\"roleId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户名不能为空"));
    }

    @Test
    void shouldFailAddUserWhenPasswordIsBlank() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/user/add")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"david\",\"password\":\"   \",\"realName\":\"张三\",\"deptId\":1,\"roleId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("密码不能为空"));
    }

    @Test
    void shouldFailAddUserWhenRealNameIsBlank() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/user/add")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"david\",\"password\":\"123456\",\"realName\":\"   \",\"deptId\":1,\"roleId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("姓名不能为空"));
    }

    @Test
    void shouldFailAddUserWhenUsernameAlreadyExists() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/user/add")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"  alice  \",\"password\":\"123456\",\"realName\":\"张三\",\"deptId\":1,\"roleId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户名已存在"));
    }

    @Test
    void shouldFailAddUserWhenDepartmentDoesNotExist() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/user/add")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"david\",\"password\":\"123456\",\"realName\":\"张三\",\"deptId\":99,\"roleId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("部门不存在"));
    }

    @Test
    void shouldFailAddUserWhenRoleDoesNotExist() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/user/add")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"david\",\"password\":\"123456\",\"realName\":\"张三\",\"deptId\":1,\"roleId\":99}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("角色不存在"));
    }

    @Test
    void shouldFailAddUserWhenStatusIsInvalid() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/user/add")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"david\",\"password\":\"123456\",\"realName\":\"张三\",\"deptId\":1,\"roleId\":2,\"status\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户状态不合法"));
    }

    @Test
    void shouldFailAddUserWhenRequestJsonIsMalformed() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/user/add")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"david\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请求参数错误"));
    }

    @Test
    void shouldUpdateUserWhenInputIsValid() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(put("/api/user/update")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":1001,\"username\":\"  alice-new  \",\"realName\":\"  王小五  \",\"gender\":\"女\",\"phone\":\" 13900000009 \",\"deptId\":2,\"roleId\":1,\"status\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.id").value(1001))
                .andExpect(jsonPath("$.data.username").value("alice-new"))
                .andExpect(jsonPath("$.data.realName").value("王小五"))
                .andExpect(jsonPath("$.data.gender").value("女"))
                .andExpect(jsonPath("$.data.phone").value("13900000009"))
                .andExpect(jsonPath("$.data.deptId").value(2))
                .andExpect(jsonPath("$.data.roleId").value(1))
                .andExpect(jsonPath("$.data.status").value(0));

        String username = jdbcTemplate.queryForObject(
                "SELECT username FROM user WHERE id = ?",
                String.class,
                1001L
        );
        String realName = jdbcTemplate.queryForObject(
                "SELECT realName FROM user WHERE id = ?",
                String.class,
                1001L
        );
        String gender = jdbcTemplate.queryForObject(
                "SELECT gender FROM user WHERE id = ?",
                String.class,
                1001L
        );
        String phone = jdbcTemplate.queryForObject(
                "SELECT phone FROM user WHERE id = ?",
                String.class,
                1001L
        );
        Long deptId = jdbcTemplate.queryForObject(
                "SELECT deptId FROM user WHERE id = ?",
                Long.class,
                1001L
        );
        Long roleId = jdbcTemplate.queryForObject(
                "SELECT roleId FROM user WHERE id = ?",
                Long.class,
                1001L
        );
        Integer statusValue = jdbcTemplate.queryForObject(
                "SELECT status FROM user WHERE id = ?",
                Integer.class,
                1001L
        );

        assertEquals("alice-new", username);
        assertEquals("王小五", realName);
        assertEquals("女", gender);
        assertEquals("13900000009", phone);
        assertEquals(2L, deptId);
        assertEquals(1L, roleId);
        assertEquals(0, statusValue);
    }

    @Test
    void shouldKeepPasswordWhenPasswordIsBlank() throws Exception {
        String token = loginAndExtractToken("admin", "123456");
        String originalPassword = jdbcTemplate.queryForObject(
                "SELECT password FROM user WHERE id = ?",
                String.class,
                1001L
        );

        mockMvc.perform(put("/api/user/update")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":1001,\"username\":\"alice\",\"password\":\"   \",\"realName\":\"王五新\",\"gender\":\"男\",\"phone\":\"13800000008\",\"deptId\":1,\"roleId\":2,\"status\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        String updatedPassword = jdbcTemplate.queryForObject(
                "SELECT password FROM user WHERE id = ?",
                String.class,
                1001L
        );

        assertEquals(originalPassword, updatedPassword);
    }

    @Test
    void shouldResetPasswordWhenPasswordProvided() throws Exception {
        String token = loginAndExtractToken("admin", "123456");
        String originalPassword = jdbcTemplate.queryForObject(
                "SELECT password FROM user WHERE id = ?",
                String.class,
                1001L
        );

        mockMvc.perform(put("/api/user/update")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":1001,\"username\":\"alice\",\"password\":\"654321\",\"realName\":\"王五\",\"gender\":\"男\",\"phone\":\"13800000000\",\"deptId\":1,\"roleId\":2,\"status\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        String updatedPassword = jdbcTemplate.queryForObject(
                "SELECT password FROM user WHERE id = ?",
                String.class,
                1001L
        );

        assertNotEquals(originalPassword, updatedPassword);
        assertTrue(PASSWORD_ENCODER.matches("654321", updatedPassword));
    }

    @Test
    void shouldFailUpdateUserWhenUserDoesNotExist() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(put("/api/user/update")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":9999,\"username\":\"nobody\",\"realName\":\"不存在\",\"deptId\":1,\"roleId\":2,\"status\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户不存在"));
    }

    @Test
    void shouldFailUpdateUserWhenUsernameAlreadyExists() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(put("/api/user/update")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":1001,\"username\":\"  bob  \",\"realName\":\"王五\",\"deptId\":1,\"roleId\":2,\"status\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户名已存在"));
    }

    @Test
    void shouldFailUpdateUserWhenDepartmentDoesNotExist() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(put("/api/user/update")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":1001,\"username\":\"alice\",\"realName\":\"王五\",\"deptId\":99,\"roleId\":2,\"status\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("部门不存在"));
    }

    @Test
    void shouldFailUpdateUserWhenRoleDoesNotExist() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(put("/api/user/update")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":1001,\"username\":\"alice\",\"realName\":\"王五\",\"deptId\":1,\"roleId\":99,\"status\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("角色不存在"));
    }

    @Test
    void shouldFailUpdateUserWhenStatusIsInvalid() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(put("/api/user/update")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":1001,\"username\":\"alice\",\"realName\":\"王五\",\"deptId\":1,\"roleId\":2,\"status\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户状态不合法"));
    }

    @Test
    void shouldFailUpdateUserWhenRequestJsonIsMalformed() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(put("/api/user/update")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请求参数错误"));
    }

    @Test
    void shouldDeleteUserWhenUserExists() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(delete("/api/user/{id}", 1003L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user WHERE id = ?",
                Integer.class,
                1003L
        );

        assertEquals(0, count);
    }

    @Test
    void shouldFailDeleteUserWhenUserDoesNotExist() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(delete("/api/user/{id}", 9999L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户不存在"));
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
