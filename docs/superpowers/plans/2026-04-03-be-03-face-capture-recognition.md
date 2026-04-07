# BE-03 人脸采集与识别模块 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 `face` 模块补齐人脸录入与验证接口，稳定输出 `registered`、`matched`、`faceScore`、`threshold` 等结果，并保证后续 `BE-04` 可直接消费。

**Architecture:** 沿用现有 `controller/service/mapper/entity/dto/vo/support` 分层，在 `module/face` 下新增最小必要文件。识别能力通过 `FaceRecognitionProvider` 抽象，默认提供一个本地可重复的实现；录入时新增 `faceFeature` 记录，验证时只读取同一用户最新一条模板，不改全局安全边界。

**Tech Stack:** Spring Boot 2.7、Spring Security、MyBatis-Plus、H2 集成测试、MockMvc、JdbcTemplate

---

## File Map

### Create

- `src/main/java/com/quyong/attendance/module/face/controller/FaceController.java`
- `src/main/java/com/quyong/attendance/module/face/service/FaceService.java`
- `src/main/java/com/quyong/attendance/module/face/service/impl/FaceServiceImpl.java`
- `src/main/java/com/quyong/attendance/module/face/mapper/FaceFeatureMapper.java`
- `src/main/java/com/quyong/attendance/module/face/entity/FaceFeature.java`
- `src/main/java/com/quyong/attendance/module/face/dto/FaceRegisterDTO.java`
- `src/main/java/com/quyong/attendance/module/face/dto/FaceVerifyDTO.java`
- `src/main/java/com/quyong/attendance/module/face/vo/FaceRegisterVO.java`
- `src/main/java/com/quyong/attendance/module/face/vo/FaceVerifyVO.java`
- `src/main/java/com/quyong/attendance/module/face/support/FaceValidationSupport.java`
- `src/main/java/com/quyong/attendance/module/face/support/FaceRecognitionProvider.java`
- `src/main/java/com/quyong/attendance/module/face/support/LocalFaceRecognitionProvider.java`
- `src/test/java/com/quyong/attendance/FaceManagementIntegrationTest.java`

### Modify

- `src/test/resources/schema.sql`
- `src/test/java/com/quyong/attendance/ModuleSkeletonBeansTest.java`
- `docs/api/API接口设计文档.md`
- `docs/module-guides/BE-03-人脸采集与识别模块开发指南.md`
- `docs/test/测试用例文档.md`

### Responsibility Notes

- `FaceFeature.java`：映射 `faceFeature` 表，承载数据库字段。
- `FaceFeatureMapper.java`：提供基础 CRUD 与“按用户查询最新模板”能力。
- `FaceValidationSupport.java`：集中做参数归一化、存在性校验和最新模板查询。
- `FaceRecognitionProvider.java` / `LocalFaceRecognitionProvider.java`：封装本地可插拔识别逻辑，避免控制器和服务层绑定具体算法。
- `FaceServiceImpl.java`：编排录入、验证、阈值判断和 VO 映射。
- `FaceManagementIntegrationTest.java`：覆盖成功、失败、边界、权限和畸形 JSON。

## Task 0: 创建独立 Worktree 并确认基线

**Files:**
- Verify: `.gitignore`
- Create: `D:\Graduation project\backend\.worktrees\be-03-face-module`

- [ ] **Step 1: 确认 `.worktrees` 仍被 Git 忽略**

Run: `git check-ignore -q .worktrees`

Expected: 命令无输出且退出码为 `0`

- [ ] **Step 2: 创建独立 worktree 和开发分支**

Run: `git worktree add ".worktrees/be-03-face-module" -b "feature/be-03-face-module"`

Expected: 输出中包含 `Preparing worktree` 与新分支名 `feature/be-03-face-module`

- [ ] **Step 3: 在新 worktree 内执行最小基线测试**

Run: `mvn "-Dtest=DatabaseIntegrationBaselineTest,AuthSecurityIntegrationTest,DeviceManagementIntegrationTest,ModuleSkeletonBeansTest" test`

Expected: `BUILD SUCCESS`

## Task 1: 补齐 H2 测试库中的 `faceFeature` 表

**Files:**
- Modify: `src/test/resources/schema.sql`
- Test: `src/test/java/com/quyong/attendance/DatabaseIntegrationBaselineTest.java`

- [ ] **Step 1: 在测试 schema 中插入 `faceFeature` 表定义**

将下面 SQL 放到 `user` 表之后、`attendanceRecord` 表之前：

```sql
CREATE TABLE faceFeature (
    id BIGINT PRIMARY KEY,
    userId BIGINT NOT NULL,
    featureData CLOB NOT NULL,
    featureHash VARCHAR(128) NOT NULL,
    encryptFlag INT NOT NULL DEFAULT 1,
    createTime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_face_feature_user FOREIGN KEY (userId) REFERENCES user (id)
);

CREATE UNIQUE INDEX uk_face_feature_hash ON faceFeature (featureHash);
CREATE INDEX idx_face_feature_user_time ON faceFeature (userId, createTime);
```

- [ ] **Step 2: 单独运行数据库基线测试，确认新表不会破坏现有测试环境**

Run: `mvn -Dtest=DatabaseIntegrationBaselineTest test`

Expected: `BUILD SUCCESS`

## Task 2: 先写失败的 BE-03 集成测试

**Files:**
- Create: `src/test/java/com/quyong/attendance/FaceManagementIntegrationTest.java`
- Test: `src/test/java/com/quyong/attendance/FaceManagementIntegrationTest.java`

- [ ] **Step 1: 创建完整的失败测试文件，先固定接口契约和行为**

```java
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

        assertEquals(1, count);
        assertNotNull(featureData);
        assertNotEquals("face-image-001", featureData);
    }

    @Test
    void shouldUseLatestFaceFeatureWhenUserRegistersAgain() throws Exception {
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
                .andExpect(jsonPath("$.data.faceScore").value(99.99))
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
                .andExpect(jsonPath("$.data.faceScore").value(0.0))
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
```

- [ ] **Step 2: 运行测试，确认当前仓库在 BE-03 行为上确实失败**

Run: `mvn -Dtest=FaceManagementIntegrationTest test`

Expected: `BUILD FAILURE`，并出现类似 `Status expected:<200> but was:<404>` 的断言失败

## Task 3: 创建 `face` 模块的数据模型与本地识别内核

**Files:**
- Create: `src/main/java/com/quyong/attendance/module/face/entity/FaceFeature.java`
- Create: `src/main/java/com/quyong/attendance/module/face/dto/FaceRegisterDTO.java`
- Create: `src/main/java/com/quyong/attendance/module/face/dto/FaceVerifyDTO.java`
- Create: `src/main/java/com/quyong/attendance/module/face/vo/FaceRegisterVO.java`
- Create: `src/main/java/com/quyong/attendance/module/face/vo/FaceVerifyVO.java`
- Create: `src/main/java/com/quyong/attendance/module/face/mapper/FaceFeatureMapper.java`
- Create: `src/main/java/com/quyong/attendance/module/face/support/FaceRecognitionProvider.java`
- Create: `src/main/java/com/quyong/attendance/module/face/support/LocalFaceRecognitionProvider.java`

- [ ] **Step 1: 新建 `FaceFeature` 实体并映射 `faceFeature` 表**

```java
package com.quyong.attendance.module.face.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("faceFeature")
public class FaceFeature {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("userId")
    private Long userId;

    @TableField("featureData")
    private String featureData;

    @TableField("featureHash")
    private String featureHash;

    @TableField("encryptFlag")
    private Integer encryptFlag;

    @TableField("createTime")
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFeatureData() {
        return featureData;
    }

    public void setFeatureData(String featureData) {
        this.featureData = featureData;
    }

    public String getFeatureHash() {
        return featureHash;
    }

    public void setFeatureHash(String featureHash) {
        this.featureHash = featureHash;
    }

    public Integer getEncryptFlag() {
        return encryptFlag;
    }

    public void setEncryptFlag(Integer encryptFlag) {
        this.encryptFlag = encryptFlag;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
```

- [ ] **Step 2: 新建录入/验证 DTO 与 VO，固定接口输入输出字段**

`FaceRegisterDTO.java`

```java
package com.quyong.attendance.module.face.dto;

public class FaceRegisterDTO {

    private Long userId;
    private String imageData;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getImageData() {
        return imageData;
    }

    public void setImageData(String imageData) {
        this.imageData = imageData;
    }
}
```

`FaceVerifyDTO.java`

```java
package com.quyong.attendance.module.face.dto;

public class FaceVerifyDTO {

    private Long userId;
    private String imageData;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getImageData() {
        return imageData;
    }

    public void setImageData(String imageData) {
        this.imageData = imageData;
    }
}
```

`FaceRegisterVO.java`

```java
package com.quyong.attendance.module.face.vo;

import java.time.LocalDateTime;

public class FaceRegisterVO {

    private Long userId;
    private Boolean registered;
    private String message;
    private LocalDateTime createTime;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Boolean getRegistered() {
        return registered;
    }

    public void setRegistered(Boolean registered) {
        this.registered = registered;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
```

`FaceVerifyVO.java`

```java
package com.quyong.attendance.module.face.vo;

import java.math.BigDecimal;

public class FaceVerifyVO {

    private Long userId;
    private Boolean registered;
    private Boolean matched;
    private BigDecimal faceScore;
    private BigDecimal threshold;
    private String message;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Boolean getRegistered() {
        return registered;
    }

    public void setRegistered(Boolean registered) {
        this.registered = registered;
    }

    public Boolean getMatched() {
        return matched;
    }

    public void setMatched(Boolean matched) {
        this.matched = matched;
    }

    public BigDecimal getFaceScore() {
        return faceScore;
    }

    public void setFaceScore(BigDecimal faceScore) {
        this.faceScore = faceScore;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }

    public void setThreshold(BigDecimal threshold) {
        this.threshold = threshold;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
```

- [ ] **Step 3: 新建 `FaceFeatureMapper`，只补最新模板查询**

```java
package com.quyong.attendance.module.face.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quyong.attendance.module.face.entity.FaceFeature;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FaceFeatureMapper extends BaseMapper<FaceFeature> {

    @Select("SELECT id, userId, featureData, featureHash, encryptFlag, createTime "
            + "FROM faceFeature "
            + "WHERE userId = #{userId} "
            + "ORDER BY createTime DESC, id DESC "
            + "LIMIT 1")
    FaceFeature selectLatestByUserId(@Param("userId") Long userId);
}
```

- [ ] **Step 4: 新建识别提供器接口和默认本地实现**

`FaceRecognitionProvider.java`

```java
package com.quyong.attendance.module.face.support;

import java.math.BigDecimal;

public interface FaceRecognitionProvider {

    String extractFeature(String imageData);

    BigDecimal compare(String imageData, String storedFeatureData);
}
```

`LocalFaceRecognitionProvider.java`

```java
package com.quyong.attendance.module.face.support;

import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@Component
public class LocalFaceRecognitionProvider implements FaceRecognitionProvider {

    @Override
    public String extractFeature(String imageData) {
        String normalizedImageData = normalize(imageData);
        return DigestUtils.md5DigestAsHex(normalizedImageData.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public BigDecimal compare(String imageData, String storedFeatureData) {
        String currentFeature = extractFeature(imageData);
        if (currentFeature.equals(storedFeatureData)) {
            return new BigDecimal("99.99");
        }
        return new BigDecimal("0.00");
    }

    private String normalize(String imageData) {
        return imageData == null ? "" : imageData.replaceAll("\\s+", "");
    }
}
```

- [ ] **Step 5: 先做一次主代码编译，确保新建模型和提供器无语法错误**

Run: `mvn -DskipTests compile`

Expected: `BUILD SUCCESS`

## Task 4: 实现校验、服务、控制器，并让测试转绿

**Files:**
- Create: `src/main/java/com/quyong/attendance/module/face/support/FaceValidationSupport.java`
- Create: `src/main/java/com/quyong/attendance/module/face/service/FaceService.java`
- Create: `src/main/java/com/quyong/attendance/module/face/service/impl/FaceServiceImpl.java`
- Create: `src/main/java/com/quyong/attendance/module/face/controller/FaceController.java`
- Modify: `src/test/java/com/quyong/attendance/ModuleSkeletonBeansTest.java`

- [ ] **Step 1: 创建 `FaceValidationSupport`，统一处理参数与用户存在性校验**

```java
package com.quyong.attendance.module.face.support;

import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.face.dto.FaceRegisterDTO;
import com.quyong.attendance.module.face.dto.FaceVerifyDTO;
import com.quyong.attendance.module.face.entity.FaceFeature;
import com.quyong.attendance.module.face.mapper.FaceFeatureMapper;
import com.quyong.attendance.module.user.mapper.UserMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class FaceValidationSupport {

    private final UserMapper userMapper;
    private final FaceFeatureMapper faceFeatureMapper;

    public FaceValidationSupport(UserMapper userMapper,
                                 FaceFeatureMapper faceFeatureMapper) {
        this.userMapper = userMapper;
        this.faceFeatureMapper = faceFeatureMapper;
    }

    public FaceRegisterDTO validateForRegister(FaceRegisterDTO registerDTO) {
        FaceRegisterDTO target = registerDTO == null ? new FaceRegisterDTO() : registerDTO;
        target.setUserId(requireUserId(target.getUserId()));
        target.setImageData(requireImageData(target.getImageData()));
        return target;
    }

    public FaceVerifyDTO validateForVerify(FaceVerifyDTO verifyDTO) {
        FaceVerifyDTO target = verifyDTO == null ? new FaceVerifyDTO() : verifyDTO;
        target.setUserId(requireUserId(target.getUserId()));
        target.setImageData(requireImageData(target.getImageData()));
        return target;
    }

    public void ensureUserExists(Long userId) {
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户不存在");
        }
    }

    public FaceFeature findLatestFaceFeature(Long userId) {
        return faceFeatureMapper.selectLatestByUserId(userId);
    }

    private Long requireUserId(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户编号不能为空");
        }
        return userId;
    }

    private String requireImageData(String imageData) {
        String normalizedImageData = normalizeImageData(imageData);
        if (!StringUtils.hasText(normalizedImageData)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "人脸图像不能为空");
        }
        return normalizedImageData;
    }

    private String normalizeImageData(String imageData) {
        return imageData == null ? null : imageData.replaceAll("\\s+", "");
    }
}
```

- [ ] **Step 2: 创建 `FaceService` 接口**

```java
package com.quyong.attendance.module.face.service;

import com.quyong.attendance.module.face.dto.FaceRegisterDTO;
import com.quyong.attendance.module.face.dto.FaceVerifyDTO;
import com.quyong.attendance.module.face.vo.FaceRegisterVO;
import com.quyong.attendance.module.face.vo.FaceVerifyVO;

public interface FaceService {

    FaceRegisterVO register(FaceRegisterDTO registerDTO);

    FaceVerifyVO verify(FaceVerifyDTO verifyDTO);
}
```

- [ ] **Step 3: 创建 `FaceServiceImpl`，实现录入、最新模板读取和阈值判断**

```java
package com.quyong.attendance.module.face.service.impl;

import com.quyong.attendance.module.face.dto.FaceRegisterDTO;
import com.quyong.attendance.module.face.dto.FaceVerifyDTO;
import com.quyong.attendance.module.face.entity.FaceFeature;
import com.quyong.attendance.module.face.mapper.FaceFeatureMapper;
import com.quyong.attendance.module.face.service.FaceService;
import com.quyong.attendance.module.face.support.FaceRecognitionProvider;
import com.quyong.attendance.module.face.support.FaceValidationSupport;
import com.quyong.attendance.module.face.vo.FaceRegisterVO;
import com.quyong.attendance.module.face.vo.FaceVerifyVO;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class FaceServiceImpl implements FaceService {

    private static final BigDecimal MATCH_THRESHOLD = new BigDecimal("85.00");

    private final FaceFeatureMapper faceFeatureMapper;
    private final FaceValidationSupport faceValidationSupport;
    private final FaceRecognitionProvider faceRecognitionProvider;

    public FaceServiceImpl(FaceFeatureMapper faceFeatureMapper,
                           FaceValidationSupport faceValidationSupport,
                           FaceRecognitionProvider faceRecognitionProvider) {
        this.faceFeatureMapper = faceFeatureMapper;
        this.faceValidationSupport = faceValidationSupport;
        this.faceRecognitionProvider = faceRecognitionProvider;
    }

    @Override
    public FaceRegisterVO register(FaceRegisterDTO registerDTO) {
        FaceRegisterDTO validatedDTO = faceValidationSupport.validateForRegister(registerDTO);
        faceValidationSupport.ensureUserExists(validatedDTO.getUserId());

        String featureData = faceRecognitionProvider.extractFeature(validatedDTO.getImageData());

        FaceFeature faceFeature = new FaceFeature();
        faceFeature.setUserId(validatedDTO.getUserId());
        faceFeature.setFeatureData(featureData);
        faceFeature.setFeatureHash(buildFeatureHash(validatedDTO.getUserId(), featureData));
        faceFeature.setEncryptFlag(1);
        faceFeatureMapper.insert(faceFeature);

        FaceFeature createdFaceFeature = faceFeatureMapper.selectById(faceFeature.getId());

        FaceRegisterVO registerVO = new FaceRegisterVO();
        registerVO.setUserId(createdFaceFeature.getUserId());
        registerVO.setRegistered(true);
        registerVO.setMessage("人脸录入成功");
        registerVO.setCreateTime(createdFaceFeature.getCreateTime());
        return registerVO;
    }

    @Override
    public FaceVerifyVO verify(FaceVerifyDTO verifyDTO) {
        FaceVerifyDTO validatedDTO = faceValidationSupport.validateForVerify(verifyDTO);
        faceValidationSupport.ensureUserExists(validatedDTO.getUserId());

        FaceVerifyVO verifyVO = new FaceVerifyVO();
        verifyVO.setUserId(validatedDTO.getUserId());
        verifyVO.setThreshold(MATCH_THRESHOLD);

        FaceFeature latestFaceFeature = faceValidationSupport.findLatestFaceFeature(validatedDTO.getUserId());
        if (latestFaceFeature == null) {
            verifyVO.setRegistered(false);
            verifyVO.setMatched(false);
            verifyVO.setFaceScore(new BigDecimal("0.00"));
            verifyVO.setMessage("该用户未录入人脸");
            return verifyVO;
        }

        BigDecimal faceScore = faceRecognitionProvider.compare(
                validatedDTO.getImageData(),
                latestFaceFeature.getFeatureData()
        );

        verifyVO.setRegistered(true);
        verifyVO.setFaceScore(faceScore);
        verifyVO.setMatched(faceScore.compareTo(MATCH_THRESHOLD) >= 0);
        verifyVO.setMessage(verifyVO.getMatched() ? "人脸验证通过" : "人脸验证未通过");
        return verifyVO;
    }

    private String buildFeatureHash(Long userId, String featureData) {
        String source = userId + ":" + featureData + ":" + UUID.randomUUID().toString();
        return DigestUtils.md5DigestAsHex(source.getBytes(StandardCharsets.UTF_8));
    }
}
```

- [ ] **Step 4: 创建 `FaceController` 并接入统一返回模型**

```java
package com.quyong.attendance.module.face.controller;

import com.quyong.attendance.common.api.Result;
import com.quyong.attendance.module.face.dto.FaceRegisterDTO;
import com.quyong.attendance.module.face.dto.FaceVerifyDTO;
import com.quyong.attendance.module.face.service.FaceService;
import com.quyong.attendance.module.face.vo.FaceRegisterVO;
import com.quyong.attendance.module.face.vo.FaceVerifyVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/face")
public class FaceController {

    private final FaceService faceService;

    public FaceController(FaceService faceService) {
        this.faceService = faceService;
    }

    @PostMapping("/register")
    public Result<FaceRegisterVO> register(@RequestBody(required = false) FaceRegisterDTO registerDTO) {
        return Result.success(faceService.register(registerDTO));
    }

    @PostMapping("/verify")
    public Result<FaceVerifyVO> verify(@RequestBody(required = false) FaceVerifyDTO verifyDTO) {
        return Result.success(faceService.verify(verifyDTO));
    }
}
```

- [ ] **Step 5: 将 `face` 模块加入 Bean 烟雾测试，防止组件未注册**

在 `ModuleSkeletonBeansTest.java` 中新增以下 import 和断言：

```java
import com.quyong.attendance.module.face.controller.FaceController;
import com.quyong.attendance.module.face.service.impl.FaceServiceImpl;
```

```java
assertNotNull(applicationContext.getBean(FaceController.class));
assertNotNull(applicationContext.getBean(FaceServiceImpl.class));
```

- [ ] **Step 6: 跑目标测试，确认 BE-03 行为和 Bean 注册全部转绿**

Run: `mvn "-Dtest=FaceManagementIntegrationTest,ModuleSkeletonBeansTest" test`

Expected: `BUILD SUCCESS`

## Task 5: 回写 API、模块指南与测试文档

**Files:**
- Modify: `docs/api/API接口设计文档.md`
- Modify: `docs/module-guides/BE-03-人脸采集与识别模块开发指南.md`
- Modify: `docs/test/测试用例文档.md`

- [ ] **Step 1: 在 API 文档的人脸章节补齐请求、约束和响应字段**

将 `docs/api/API接口设计文档.md` 中 `4.1` 和 `4.2` 两节改成下面内容：

````md
### 4.1 人脸录入
- 路径：`POST /api/face/register`
- 业务约束：
  - `userId` 必填，且必须对应已存在员工
  - `imageData` 必填
  - 同一员工重复录入时新增一条 `faceFeature` 记录，验证始终只取最新一条模板
  - 不直接返回原始特征内容

```json
{
  "userId": 1001,
  "imageData": "base64..."
}
```

响应字段要点：
- `userId`
- `registered`
- `message`
- `createTime`

### 4.2 人脸验证
- 路径：`POST /api/face/verify`
- 业务约束：
  - `userId` 必填，且必须对应已存在员工
  - `imageData` 必填
  - 未录入人脸时返回 `registered=false`
  - 验证失败时返回 `matched=false`

```json
{
  "userId": 1001,
  "imageData": "base64..."
}
```

响应字段要点：
- `userId`
- `registered`
- `matched`
- `faceScore`
- `threshold`
- `message`
````

- [ ] **Step 2: 更新 BE-03 模块指南中的固定输出和最小验证命令**

将 `docs/module-guides/BE-03-人脸采集与识别模块开发指南.md` 中相关内容补成下面版本：

````md
4. 输出验证分数、是否通过、人脸是否已注册等结果，并固定返回 `userId`、`registered`、`matched`、`faceScore`、`threshold`、`message`。
5. 预留第三方识别服务封装点，默认采用本地可插拔实现。
6. 重复录入时新增一条 `faceFeature` 记录，验证只使用最新一条模板。
7. 处理录入失败、未注册、验证失败等异常情况。
8. 补齐录入与验证测试。
````

````md
## 9. 最小验证命令

```bash
mvn -Dtest=FaceManagementIntegrationTest test
```
````

- [ ] **Step 3: 在测试文档追加 BE-03 的接口测试用例**

在 `docs/test/测试用例文档.md` 的接口测试表中 `API057` 后面追加：

```md
| API058 | `POST /api/face/register` | 人脸录入成功 | 管理员 token + 合法 `userId`、`imageData` | 返回录入成功结果，不暴露原始特征 |
| API059 | `POST /api/face/register` | 人脸录入用户编号必填校验 | `userId` 缺失 | 返回 `用户编号不能为空` |
| API060 | `POST /api/face/register` | 人脸录入图像必填校验 | `imageData` 为空白 | 返回 `人脸图像不能为空` |
| API061 | `POST /api/face/register` | 人脸录入用户存在性校验 | 不存在的 `userId` | 返回 `用户不存在` |
| API062 | `POST /api/face/register` | 人脸录入接口畸形 JSON 校验 | 畸形 JSON 请求体 | 返回 `请求参数错误` |
| API063 | `POST /api/face/verify` | 未录入用户的人脸验证 | 合法 `userId` + 合法 `imageData` | 返回 `registered=false`、`matched=false` |
| API064 | `POST /api/face/verify` | 已录入用户验证通过 | 已录入模板 + 相同 `imageData` | 返回 `registered=true`、`matched=true` 和 `faceScore` |
| API065 | `POST /api/face/verify` | 已录入用户验证失败 | 已录入模板 + 不同 `imageData` | 返回 `registered=true`、`matched=false` |
| API066 | `POST /api/face/verify` | 人脸验证接口畸形 JSON 校验 | 畸形 JSON 请求体 | 返回 `请求参数错误` |
```

## Task 6: 做最终聚焦验证并准备进入编码交付

**Files:**
- Verify: `src/test/java/com/quyong/attendance/FaceManagementIntegrationTest.java`
- Verify: `src/test/java/com/quyong/attendance/ModuleSkeletonBeansTest.java`
- Verify: `src/test/resources/schema.sql`

- [ ] **Step 1: 运行最终最小验证集**

Run: `mvn "-Dtest=DatabaseIntegrationBaselineTest,FaceManagementIntegrationTest,ModuleSkeletonBeansTest" test`

Expected: `BUILD SUCCESS`

- [ ] **Step 2: 再跑一次主代码编译，确认无测试外编译问题**

Run: `mvn -DskipTests compile`

Expected: `BUILD SUCCESS`

- [ ] **Step 3: 查看工作区，确认只包含 BE-03 相关改动且不执行 commit**

Run: `git status --short`

Expected: 只看到 `module/face`、`FaceManagementIntegrationTest`、`schema.sql` 和 BE-03 相关文档改动；按用户要求不执行 `git commit`
