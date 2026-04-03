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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class FaceManagementIntegrationTest {

    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM decisionTrace");
        jdbcTemplate.execute("DELETE FROM modelCallLog");
        jdbcTemplate.execute("DELETE FROM exceptionAnalysis");
        jdbcTemplate.execute("DELETE FROM attendanceException");
        jdbcTemplate.execute("DELETE FROM attendanceRepair");
        jdbcTemplate.execute("DELETE FROM faceFeature");
        jdbcTemplate.execute("DELETE FROM attendanceRecord");
        jdbcTemplate.execute("DELETE FROM user");
        jdbcTemplate.execute("DELETE FROM department");
        jdbcTemplate.execute("DELETE FROM role");

        insertRole(1L, "ADMIN", "管理员");
        insertRole(2L, "EMPLOYEE", "员工");
        insertDepartment(1L, "技术部", "负责系统研发");
        insertUser(9001L, "admin", "系统管理员", 1L, 1L, 1);
        insertUser(1001L, "zhangsan", "张三", 1L, 2L, 1);
    }

    @Test
    void shouldRegisterFaceWhenInputIsValid() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/face/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":1001,\"imageData\":\"  face-image-001  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.userId").value(1001))
                .andExpect(jsonPath("$.data.registered").value(true))
                .andExpect(jsonPath("$.data.message").value("人脸录入成功"))
                .andExpect(jsonPath("$.data.createTime").isNotEmpty());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM faceFeature WHERE userId = ?",
                Integer.class,
                1001L
        );
        String featureData = jdbcTemplate.queryForObject(
                "SELECT featureData FROM faceFeature WHERE userId = ?",
                String.class,
                1001L
        );
        String featureHash = jdbcTemplate.queryForObject(
                "SELECT featureHash FROM faceFeature WHERE userId = ?",
                String.class,
                1001L
        );
        Integer encryptFlag = jdbcTemplate.queryForObject(
                "SELECT encryptFlag FROM faceFeature WHERE userId = ?",
                Integer.class,
                1001L
        );

        assertEquals(1, count);
        assertNotNull(featureData);
        assertNotEquals("  face-image-001  ", featureData);
        assertNotEquals("face-image-001", featureData);
        assertNotNull(featureHash);
        assertNotEquals("", featureHash.trim());
        assertEquals(1, encryptFlag);
    }

    @Test
    void shouldKeepHistoryAndUseLatestFaceFeatureWhenUserRegistersAgain() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/face/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":1001,\"imageData\":\"face-image-old\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/face/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":1001,\"imageData\":\"face-image-new\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM faceFeature WHERE userId = ?",
                Integer.class,
                1001L
        );
        assertEquals(2, count);

        mockMvc.perform(post("/api/face/verify")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":1001,\"imageData\":\"face-image-old\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.registered").value(true))
                .andExpect(jsonPath("$.data.matched").value(false))
                .andExpect(jsonPath("$.data.message").value("人脸验证未通过"));

        mockMvc.perform(post("/api/face/verify")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":1001,\"imageData\":\"face-image-new\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.registered").value(true))
                .andExpect(jsonPath("$.data.matched").value(true))
                .andExpect(jsonPath("$.data.message").value("人脸验证通过"));
    }

    @Test
    void shouldReturnUnregisteredWhenUserHasNoFaceTemplate() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/face/verify")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":1001,\"imageData\":\"face-image-verify\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(1001))
                .andExpect(jsonPath("$.data.registered").value(false))
                .andExpect(jsonPath("$.data.matched").value(false))
                .andExpect(jsonPath("$.data.faceScore").value(0.0))
                .andExpect(jsonPath("$.data.threshold").value(85.0))
                .andExpect(jsonPath("$.data.message").value("该用户未录入人脸"));
    }

    @Test
    void shouldVerifyFaceWhenImageMatchesLatestTemplate() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/face/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":1001,\"imageData\":\"face-image-match\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/face/verify")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":1001,\"imageData\":\"face-image-match\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.registered").value(true))
                .andExpect(jsonPath("$.data.matched").value(true))
                .andExpect(jsonPath("$.data.faceScore").value(greaterThanOrEqualTo(85.0)))
                .andExpect(jsonPath("$.data.threshold").value(85.0))
                .andExpect(jsonPath("$.data.message").value("人脸验证通过"));
    }

    @Test
    void shouldReturnMatchedFalseWhenImageDoesNotMatchLatestTemplate() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/face/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":1001,\"imageData\":\"face-image-origin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/face/verify")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":1001,\"imageData\":\"face-image-other\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.registered").value(true))
                .andExpect(jsonPath("$.data.matched").value(false))
                .andExpect(jsonPath("$.data.faceScore").value(lessThan(85.0)))
                .andExpect(jsonPath("$.data.threshold").value(85.0))
                .andExpect(jsonPath("$.data.message").value("人脸验证未通过"));
    }

    @Test
    void shouldFailRegisterWhenUserIdIsNull() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/face/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"imageData\":\"face-image-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户编号不能为空"));
    }

    @Test
    void shouldFailRegisterWhenImageDataIsBlank() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/face/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":1001,\"imageData\":\"   \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("人脸图像不能为空"));
    }

    @Test
    void shouldFailRegisterWhenUserDoesNotExist() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/face/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":4040,\"imageData\":\"face-image-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户不存在"));
    }

    @Test
    void shouldFailVerifyWhenUserDoesNotExist() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/face/verify")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":4040,\"imageData\":\"face-image-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户不存在"));
    }

    @Test
    void shouldReturnBadRequestWhenRegisterRequestBodyIsMalformed() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/face/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":1001,\"imageData\":}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请求参数错误"));
    }

    @Test
    void shouldReturnBadRequestWhenVerifyRequestBodyIsMalformed() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(post("/api/face/verify")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":1001,\"imageData\":}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请求参数错误"));
    }

    @Test
    void shouldReturnUnauthorizedWhenAccessingFaceApiWithoutToken() throws Exception {
        mockMvc.perform(post("/api/face/register")
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":1001,\"imageData\":\"face-image-001\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("unauthorized"));
    }

    @Test
    void shouldReturnForbiddenWhenEmployeeAccessesFaceApi() throws Exception {
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(post("/api/face/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":1001,\"imageData\":\"face-image-001\"}"))
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
