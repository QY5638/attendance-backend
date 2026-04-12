package com.quyong.attendance;

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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class OperationLogIntegrationTest {

    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM operationLog");
        jdbcTemplate.execute("DELETE FROM user");
        jdbcTemplate.execute("DELETE FROM department");
        jdbcTemplate.execute("DELETE FROM role");

        insertRole(1L, "ADMIN", "管理员");
        insertDepartment(1L, "技术部", "负责系统研发");
        insertUser(9001L, "admin", "系统管理员", 1L, 1L);

        jdbcTemplate.update(
                "INSERT INTO operationLog (id, userId, type, content, operationTime) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
                1L,
                9001L,
                "FACE_LIVENESS_PASS",
                "系统管理员完成活体挑战"
        );
        jdbcTemplate.update(
                "INSERT INTO operationLog (id, userId, type, content, operationTime) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
                2L,
                9001L,
                "LOGIN",
                "系统管理员登录系统"
        );
    }

    @Test
    void shouldFilterOperationLogsByGroupedTypesAndReturnActorInfo() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/log/operation/list")
                        .header("Authorization", "Bearer " + token)
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("types", "FACE_LIVENESS_SESSION,FACE_LIVENESS_PASS,FACE_LIVENESS_REJECT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].type").value("FACE_LIVENESS_PASS"))
                .andExpect(jsonPath("$.data.records[0].realName").value("系统管理员"))
                .andExpect(jsonPath("$.data.records[0].username").value("admin"));
    }

    @Test
    void shouldSummarizeOperationLogsByGroupedTypes() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/log/operation/summary")
                        .header("Authorization", "Bearer " + token)
                        .param("types", "LOGIN,FACE_LIVENESS_PASS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.typeCounts.LOGIN").value(2))
                .andExpect(jsonPath("$.data.typeCounts.FACE_LIVENESS_PASS").value(1));
    }

    @Test
    void shouldExportAuditLogsByGroupedTypes() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/statistics/export")
                        .header("Authorization", "Bearer " + token)
                        .param("exportType", "AUDIT")
                        .param("types", "FACE_LIVENESS_SESSION,FACE_LIVENESS_PASS,FACE_LIVENESS_REJECT"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment; filename=")))
                .andExpect(content().string(containsString("账号,姓名,办理动作")))
                .andExpect(content().string(containsString("admin")))
                .andExpect(content().string(containsString("系统管理员")))
                .andExpect(content().string(containsString("活体挑战通过")));
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

        String content = mvcResult.getResponse().getContentAsString();
        int tokenIndex = content.indexOf("\"token\":\"");
        int start = tokenIndex + 9;
        int end = content.indexOf('"', start);
        return content.substring(start, end);
    }
}
