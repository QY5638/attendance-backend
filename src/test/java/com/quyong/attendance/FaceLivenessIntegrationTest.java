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

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.face.require-liveness=true")
@ActiveProfiles("test")
@AutoConfigureMockMvc
class FaceLivenessIntegrationTest {

    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM faceFeature");
        jdbcTemplate.execute("DELETE FROM attendanceRecord");
        jdbcTemplate.execute("DELETE FROM user");
        jdbcTemplate.execute("DELETE FROM department");
        jdbcTemplate.execute("DELETE FROM role");

        insertRole(1L, "ADMIN", "管理员");
        insertRole(2L, "EMPLOYEE", "员工");
        insertDepartment(1L, "技术部", "负责系统研发");
        insertUser(1001L, "zhangsan", "张三", 1L, 2L, 1);
    }

    @Test
    void shouldRegisterFaceAfterLivenessChallengeCompletes() throws Exception {
        String token = loginAndExtractToken("zhangsan", "123456");
        JsonNode livenessProof = completeLiveness(token, "face-image-liveness-success");

        mockMvc.perform(post("/api/face/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"imageData\":\"face-image-liveness-success\",\"livenessToken\":\"" + livenessProof.path("livenessToken").asText() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.registered").value(true))
                .andExpect(jsonPath("$.data.livenessPassed").value(true));

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM faceFeature WHERE userId = ?", Integer.class, 1001L);
        assertEquals(1, count);
    }

    @Test
    void shouldRejectRegisterWhenLivenessTokenIsMissing() throws Exception {
        String token = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(post("/api/face/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"imageData\":\"face-image-without-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("当前操作需要先完成活体校验，请使用摄像头完成挑战"));
    }

    @Test
    void shouldRejectVerifyWhenLivenessProofDoesNotMatchSubmittedImage() throws Exception {
        String token = loginAndExtractToken("zhangsan", "123456");
        JsonNode registerProof = completeLiveness(token, "face-image-origin");

        mockMvc.perform(post("/api/face/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"imageData\":\"face-image-origin\",\"livenessToken\":\"" + registerProof.path("livenessToken").asText() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        JsonNode verifyProof = completeLiveness(token, "face-image-origin");

        mockMvc.perform(post("/api/face/verify")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"imageData\":\"face-image-other\",\"livenessToken\":\"" + verifyProof.path("livenessToken").asText() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("活体校验图像与当前提交图像不一致，请重新完成挑战"));
    }

    @Test
    void shouldAllowReuseProofAfterPreviewVerifyWithoutConsumption() throws Exception {
        String token = loginAndExtractToken("zhangsan", "123456");
        JsonNode proof = completeLiveness(token, "face-image-preview");

        mockMvc.perform(post("/api/face/verify")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"imageData\":\"face-image-preview\",\"livenessToken\":\"" + proof.path("livenessToken").asText() + "\",\"consumeLiveness\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.registered").value(false));

        mockMvc.perform(post("/api/face/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"imageData\":\"face-image-preview\",\"livenessToken\":\"" + proof.path("livenessToken").asText() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.registered").value(true));
    }

    @Test
    void shouldRejectReplayAfterProofIsConsumed() throws Exception {
        String token = loginAndExtractToken("zhangsan", "123456");
        JsonNode proof = completeLiveness(token, "face-image-consume-once");

        mockMvc.perform(post("/api/face/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"imageData\":\"face-image-consume-once\",\"livenessToken\":\"" + proof.path("livenessToken").asText() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/face/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"imageData\":\"face-image-consume-once\",\"livenessToken\":\"" + proof.path("livenessToken").asText() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("活体校验证明已失效，请重新完成挑战"));
    }

    private JsonNode completeLiveness(String token, String imageData) throws Exception {
        MvcResult sessionResult = mockMvc.perform(post("/api/face/liveness/session")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        JsonNode sessionData = objectMapper.readTree(sessionResult.getResponse().getContentAsString()).path("data");
        String sessionId = sessionData.path("sessionId").asText();
        JsonNode actions = sessionData.path("actions");
        assertTrue(actions.isArray());

        StringBuilder completedActions = new StringBuilder("[");
        StringBuilder actionScores = new StringBuilder("{");
        Iterator<JsonNode> iterator = actions.elements();
        boolean first = true;
        while (iterator.hasNext()) {
            String action = iterator.next().asText();
            if (!first) {
                completedActions.append(',');
                actionScores.append(',');
            }
            completedActions.append('"').append(action).append('"');
            actionScores.append('"').append(action).append('"').append(':').append("0.95");
            first = false;
        }
        completedActions.append(']');
        actionScores.append('}');

        long startedAt = System.currentTimeMillis();
        long completedAt = startedAt + 5000L;

        MvcResult completeResult = mockMvc.perform(post("/api/face/liveness/complete")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{"
                                + "\"sessionId\":\"" + sessionId + "\","
                                + "\"imageData\":\"" + imageData + "\","
                                + "\"startedAt\":" + startedAt + ","
                                + "\"completedAt\":" + completedAt + ","
                                + "\"sampleCount\":20,"
                                + "\"stableFaceFrames\":18,"
                                + "\"completedActions\":" + completedActions + ","
                                + "\"actionScores\":" + actionScores
                                + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.livenessToken").isString())
                .andReturn();

        return objectMapper.readTree(completeResult.getResponse().getContentAsString()).path("data");
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

        return objectMapper.readTree(mvcResult.getResponse().getContentAsString()).path("data").path("token").asText();
    }
}
