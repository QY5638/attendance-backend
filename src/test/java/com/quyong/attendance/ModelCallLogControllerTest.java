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

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ModelCallLogControllerTest {

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
        insertPromptTemplate(8001L, "COMPLEX_EXCEPTION", "复杂异常分析模板", "EXCEPTION_ANALYSIS", "v1.0", "模板内容", "ENABLED");
    }

    @Test
    void shouldReturnModelCallLogListWithFilters() throws Exception {
        insertModelCallLog(9001L, "EXCEPTION_ANALYSIS", 3001L, 8001L, "输入摘要1", "输出摘要1", "SUCCESS", 1260, null, "2026-03-26 08:59:08");
        insertModelCallLog(9002L, "WARNING_ADVICE", 5001L, 8001L, "输入摘要2", "输出摘要2", "FAILED", 880, "调用失败", "2026-03-26 09:10:08");
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/system/model-log/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("businessType", "EXCEPTION_ANALYSIS")
                        .param("status", "SUCCESS")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(9001))
                .andExpect(jsonPath("$.data.records[0].businessType").value("EXCEPTION_ANALYSIS"))
                .andExpect(jsonPath("$.data.records[0].status").value("SUCCESS"));
    }

    @Test
    void shouldFilterModelCallLogListByTimeRangeAndBusinessId() throws Exception {
        insertModelCallLog(9001L, "EXCEPTION_ANALYSIS", 3001L, 8001L, "输入摘要1", "输出摘要1", "SUCCESS", 1260, null, "2026-03-26 08:59:08");
        insertModelCallLog(9002L, "EXCEPTION_ANALYSIS", 3002L, 8001L, "输入摘要2", "输出摘要2", "SUCCESS", 980, null, "2026-03-27 08:59:08");
        String adminToken = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/system/model-log/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("businessId", "3002")
                        .param("startDate", "2026-03-27")
                        .param("endDate", "2026-03-27")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(9002))
                .andExpect(jsonPath("$.data.records[0].businessId").value(3002));
    }

    @Test
    void shouldReturnForbiddenWhenEmployeeAccessesModelLogApi() throws Exception {
        String employeeToken = loginAndExtractToken("zhangsan", "123456");

        mockMvc.perform(get("/api/system/model-log/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void shouldReturnUnauthorizedWhenRequestHasNoTokenForModelLogApi() throws Exception {
        mockMvc.perform(get("/api/system/model-log/list")
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
                                      String status) {
        jdbcTemplate.update(
                "INSERT INTO promptTemplate (id, code, name, sceneType, version, content, status, remark, createTime, updateTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                id,
                code,
                name,
                sceneType,
                version,
                content,
                status,
                "默认模板"
        );
    }

    private void insertModelCallLog(Long id,
                                    String businessType,
                                    Long businessId,
                                    Long promptTemplateId,
                                    String inputSummary,
                                    String outputSummary,
                                    String status,
                                    Integer latencyMs,
                                    String errorMessage,
                                    String createTime) {
        jdbcTemplate.update(
                "INSERT INTO modelCallLog (id, businessType, businessId, promptTemplateId, inputSummary, outputSummary, status, latencyMs, errorMessage, createTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id,
                businessType,
                businessId,
                promptTemplateId,
                inputSummary,
                outputSummary,
                status,
                latencyMs,
                errorMessage,
                createTime
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
