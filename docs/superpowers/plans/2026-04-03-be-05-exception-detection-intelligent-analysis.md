# BE-05 异常检测与智能分析模块 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不越界修改 `BE-01~04` 的前提下，补齐 `BE-05` 的规则管理、基础异常判定、复杂异常智能分析、异常查询和决策追踪能力，并稳定输出可供 `BE-06/07/08` 直接复用的异常字段。

**Architecture:** 公开业务接口全部落在 `module/exceptiondetect`，沿用现有 `controller/service/mapper/entity/dto/vo/support` 分层；`rule` 作为 `exceptiondetect` 子域实现。提示词读取、模型调用日志和决策追踪通过轻量 `module/model` 内部支撑层承接，复杂分析默认走可替换的 `ModelGateway` mock 实现，并保留失败降级为 `MODEL_FALLBACK` 的闭环。

**Tech Stack:** Spring Boot 2.7、Spring Security、MyBatis-Plus、JUnit 5、MockMvc、JdbcTemplate、H2

> 当前用户要求：本计划执行阶段不包含 `git commit`、`git push`、创建 PR 或其他额外 Git 动作。

---

## 文件结构与职责

### Create

- `src/main/java/com/quyong/attendance/module/exceptiondetect/controller/ExceptionController.java`
- `src/main/java/com/quyong/attendance/module/exceptiondetect/controller/RuleController.java`
- `src/main/java/com/quyong/attendance/module/exceptiondetect/dto/RuleQueryDTO.java`
- `src/main/java/com/quyong/attendance/module/exceptiondetect/dto/RuleSaveDTO.java`
- `src/main/java/com/quyong/attendance/module/exceptiondetect/dto/RuleStatusDTO.java`
- `src/main/java/com/quyong/attendance/module/exceptiondetect/dto/RuleCheckDTO.java`
- `src/main/java/com/quyong/attendance/module/exceptiondetect/dto/ComplexCheckDTO.java`
- `src/main/java/com/quyong/attendance/module/exceptiondetect/dto/RiskFeaturesDTO.java`
- `src/main/java/com/quyong/attendance/module/exceptiondetect/dto/ExceptionQueryDTO.java`
- `src/main/java/com/quyong/attendance/module/exceptiondetect/entity/Rule.java`
- `src/main/java/com/quyong/attendance/module/exceptiondetect/entity/AttendanceException.java`
- `src/main/java/com/quyong/attendance/module/exceptiondetect/entity/ExceptionAnalysis.java`
- `src/main/java/com/quyong/attendance/module/exceptiondetect/mapper/RuleMapper.java`
- `src/main/java/com/quyong/attendance/module/exceptiondetect/mapper/AttendanceExceptionMapper.java`
- `src/main/java/com/quyong/attendance/module/exceptiondetect/mapper/ExceptionAnalysisMapper.java`
- `src/main/java/com/quyong/attendance/module/exceptiondetect/service/RuleService.java`
- `src/main/java/com/quyong/attendance/module/exceptiondetect/service/ExceptionAnalysisOrchestrator.java`
- `src/main/java/com/quyong/attendance/module/exceptiondetect/service/ExceptionQueryService.java`
- `src/main/java/com/quyong/attendance/module/exceptiondetect/service/impl/RuleServiceImpl.java`
- `src/main/java/com/quyong/attendance/module/exceptiondetect/service/impl/ExceptionAnalysisOrchestratorImpl.java`
- `src/main/java/com/quyong/attendance/module/exceptiondetect/service/impl/ExceptionQueryServiceImpl.java`
- `src/main/java/com/quyong/attendance/module/exceptiondetect/support/RuleValidationSupport.java`
- `src/main/java/com/quyong/attendance/module/exceptiondetect/support/ExceptionValidationSupport.java`
- `src/main/java/com/quyong/attendance/module/exceptiondetect/vo/RuleVO.java`
- `src/main/java/com/quyong/attendance/module/exceptiondetect/vo/ExceptionDecisionVO.java`
- `src/main/java/com/quyong/attendance/module/exceptiondetect/vo/AttendanceExceptionVO.java`
- `src/main/java/com/quyong/attendance/module/exceptiondetect/vo/ExceptionAnalysisBriefVO.java`
- `src/main/java/com/quyong/attendance/module/model/prompt/entity/PromptTemplate.java`
- `src/main/java/com/quyong/attendance/module/model/prompt/mapper/PromptTemplateMapper.java`
- `src/main/java/com/quyong/attendance/module/model/log/entity/ModelCallLog.java`
- `src/main/java/com/quyong/attendance/module/model/log/mapper/ModelCallLogMapper.java`
- `src/main/java/com/quyong/attendance/module/model/log/service/ModelCallLogService.java`
- `src/main/java/com/quyong/attendance/module/model/log/service/impl/ModelCallLogServiceImpl.java`
- `src/main/java/com/quyong/attendance/module/model/trace/entity/DecisionTrace.java`
- `src/main/java/com/quyong/attendance/module/model/trace/mapper/DecisionTraceMapper.java`
- `src/main/java/com/quyong/attendance/module/model/trace/service/DecisionTraceService.java`
- `src/main/java/com/quyong/attendance/module/model/trace/service/impl/DecisionTraceServiceImpl.java`
- `src/main/java/com/quyong/attendance/module/model/trace/vo/DecisionTraceVO.java`
- `src/main/java/com/quyong/attendance/module/model/gateway/dto/ModelInvokeRequest.java`
- `src/main/java/com/quyong/attendance/module/model/gateway/dto/ModelInvokeResponse.java`
- `src/main/java/com/quyong/attendance/module/model/gateway/service/ModelGateway.java`
- `src/main/java/com/quyong/attendance/module/model/gateway/service/MockModelGateway.java`
- `src/test/java/com/quyong/attendance/RuleControllerTest.java`
- `src/test/java/com/quyong/attendance/ExceptionControllerTest.java`

### Modify

- `src/test/resources/schema.sql`
- `src/test/java/com/quyong/attendance/AuthSecurityIntegrationTest.java`
- `src/test/java/com/quyong/attendance/AttendanceManagementIntegrationTest.java`
- `src/test/java/com/quyong/attendance/DeviceManagementIntegrationTest.java`
- `src/test/java/com/quyong/attendance/FaceManagementIntegrationTest.java`
- `src/test/java/com/quyong/attendance/DepartmentManagementIntegrationTest.java`
- `src/test/java/com/quyong/attendance/UserManagementIntegrationTest.java`
- `src/test/java/com/quyong/attendance/ModuleSkeletonBeansTest.java`
- `docs/module-guides/BE-05-异常检测与智能分析模块开发指南.md`
- `docs/test/测试用例文档.md`

### Responsibility Notes

- `RuleValidationSupport.java`：统一处理规则列表分页参数、时间字符串解析、阈值合法性和状态合法性。
- `ExceptionValidationSupport.java`：统一处理 `recordId`、`userId`、当前登录管理员身份、异常查询参数和分页参数校验。
- `ExceptionAnalysisOrchestratorImpl.java`：编排 `rule-check` 与 `complex-check` 两条主链路，复用现有 `attendanceRecord`、`device`、`AuthUser` 契约。
- `PromptTemplateMapper.java`：只读查询启用中的 `EXCEPTION_ANALYSIS` 模板，不开放管理接口。
- `ModelCallLogServiceImpl.java`：封装成功/失败日志写入，避免编排服务直接堆积日志细节。
- `DecisionTraceServiceImpl.java`：封装决策链落库和列表查询，供 `BE-05` 和后续 `BE-06/07` 继续复用。
- `RuleControllerTest.java`：固定 `rule` 的列表、新增、修改、状态切换契约。
- `ExceptionControllerTest.java`：固定规则判定、复杂分析、失败降级、异常查询和安全边界契约。

## Task 0: 创建独立 Worktree 并确认基线

**Files:**
- Verify: `.gitignore`
- Create: `D:\Graduation project\backend\.worktrees\be-05-exception-analysis`

- [ ] **Step 1: 确认 `.worktrees` 仍被 Git 忽略**

Run: `git check-ignore -q .worktrees`

Expected: 命令无输出且退出码为 `0`

- [ ] **Step 2: 创建独立 worktree 和开发分支**

Run: `git worktree add ".worktrees/be-05-exception-analysis" -b "feature/be-05-exception-analysis"`

Expected: 输出中包含 `Preparing worktree` 与新分支名 `feature/be-05-exception-analysis`

- [ ] **Step 3: 在新 worktree 内执行当前基线测试**

Run: `mvn "-Dtest=DatabaseIntegrationBaselineTest,AuthSecurityIntegrationTest,AttendanceManagementIntegrationTest,ModuleSkeletonBeansTest" test`

Expected: `BUILD SUCCESS`

## Task 1: 扩展 H2 测试基线并兼容现有测试清理顺序

**Files:**
- Modify: `src/test/resources/schema.sql`
- Modify: `src/test/java/com/quyong/attendance/AuthSecurityIntegrationTest.java`
- Modify: `src/test/java/com/quyong/attendance/AttendanceManagementIntegrationTest.java`
- Modify: `src/test/java/com/quyong/attendance/DeviceManagementIntegrationTest.java`
- Modify: `src/test/java/com/quyong/attendance/FaceManagementIntegrationTest.java`
- Modify: `src/test/java/com/quyong/attendance/DepartmentManagementIntegrationTest.java`
- Modify: `src/test/java/com/quyong/attendance/UserManagementIntegrationTest.java`

- [ ] **Step 1: 在 `schema.sql` 中补齐 `BE-05` 相关表定义**

将下面 SQL 插入到 `attendanceRepair` 表之后：

```sql
CREATE TABLE rule (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    startTime TIME NOT NULL,
    endTime TIME NOT NULL,
    lateThreshold INT NOT NULL DEFAULT 10,
    earlyThreshold INT NOT NULL DEFAULT 10,
    repeatLimit INT NOT NULL DEFAULT 3,
    status INT NOT NULL DEFAULT 1
);

CREATE TABLE attendanceException (
    id BIGINT PRIMARY KEY,
    recordId BIGINT NOT NULL,
    userId BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    riskLevel VARCHAR(20) NOT NULL,
    sourceType VARCHAR(20) NOT NULL DEFAULT 'RULE',
    description VARCHAR(255),
    processStatus VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    createTime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attendance_exception_record FOREIGN KEY (recordId) REFERENCES attendanceRecord (id),
    CONSTRAINT fk_attendance_exception_user FOREIGN KEY (userId) REFERENCES user (id)
);

CREATE TABLE promptTemplate (
    id BIGINT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    sceneType VARCHAR(50) NOT NULL,
    version VARCHAR(50) NOT NULL,
    content CLOB NOT NULL,
    status VARCHAR(20) NOT NULL,
    remark VARCHAR(255),
    createTime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updateTime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE exceptionAnalysis (
    id BIGINT PRIMARY KEY,
    exceptionId BIGINT NOT NULL,
    promptTemplateId BIGINT,
    inputSummary CLOB,
    modelResult CLOB,
    modelConclusion VARCHAR(100),
    confidenceScore DECIMAL(5,2),
    decisionReason CLOB,
    suggestion VARCHAR(255),
    reasonSummary CLOB,
    actionSuggestion VARCHAR(255),
    similarCaseSummary CLOB,
    promptVersion VARCHAR(50),
    createTime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_exception_analysis_exception FOREIGN KEY (exceptionId) REFERENCES attendanceException (id)
);

CREATE TABLE modelCallLog (
    id BIGINT PRIMARY KEY,
    businessType VARCHAR(50) NOT NULL,
    businessId BIGINT NOT NULL,
    promptTemplateId BIGINT,
    inputSummary CLOB,
    outputSummary CLOB,
    status VARCHAR(20) NOT NULL,
    latencyMs INT,
    errorMessage VARCHAR(255),
    createTime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE decisionTrace (
    id BIGINT PRIMARY KEY,
    businessType VARCHAR(50) NOT NULL,
    businessId BIGINT NOT NULL,
    ruleResult CLOB,
    modelResult CLOB,
    finalDecision CLOB,
    confidenceScore DECIMAL(5,2),
    decisionReason CLOB,
    createTime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

- [ ] **Step 2: 在现有集成测试的清理方法前面补上 `BE-05` 子表删除顺序**

在下列类的 `setUp()` 或 `resetAuthTestData()` 方法开头统一插入：

```java
jdbcTemplate.execute("DELETE FROM decisionTrace");
jdbcTemplate.execute("DELETE FROM modelCallLog");
jdbcTemplate.execute("DELETE FROM exceptionAnalysis");
jdbcTemplate.execute("DELETE FROM attendanceException");
```

需要修改的类：

```text
src/test/java/com/quyong/attendance/AuthSecurityIntegrationTest.java
src/test/java/com/quyong/attendance/AttendanceManagementIntegrationTest.java
src/test/java/com/quyong/attendance/DeviceManagementIntegrationTest.java
src/test/java/com/quyong/attendance/FaceManagementIntegrationTest.java
src/test/java/com/quyong/attendance/DepartmentManagementIntegrationTest.java
src/test/java/com/quyong/attendance/UserManagementIntegrationTest.java
```

- [ ] **Step 3: 运行基线回归，确认新表和外键不会破坏现有测试**

Run: `mvn "-Dtest=DatabaseIntegrationBaselineTest,AuthSecurityIntegrationTest,AttendanceManagementIntegrationTest,DeviceManagementIntegrationTest,FaceManagementIntegrationTest,DepartmentManagementIntegrationTest,UserManagementIntegrationTest" test`

Expected: `BUILD SUCCESS`

## Task 2: 规则管理 RED/GREEN

**Files:**
- Create: `src/test/java/com/quyong/attendance/RuleControllerTest.java`
- Create: `src/main/java/com/quyong/attendance/module/exceptiondetect/entity/Rule.java`
- Create: `src/main/java/com/quyong/attendance/module/exceptiondetect/dto/RuleQueryDTO.java`
- Create: `src/main/java/com/quyong/attendance/module/exceptiondetect/dto/RuleSaveDTO.java`
- Create: `src/main/java/com/quyong/attendance/module/exceptiondetect/dto/RuleStatusDTO.java`
- Create: `src/main/java/com/quyong/attendance/module/exceptiondetect/vo/RuleVO.java`
- Create: `src/main/java/com/quyong/attendance/module/exceptiondetect/mapper/RuleMapper.java`
- Create: `src/main/java/com/quyong/attendance/module/exceptiondetect/support/RuleValidationSupport.java`
- Create: `src/main/java/com/quyong/attendance/module/exceptiondetect/service/RuleService.java`
- Create: `src/main/java/com/quyong/attendance/module/exceptiondetect/service/impl/RuleServiceImpl.java`
- Create: `src/main/java/com/quyong/attendance/module/exceptiondetect/controller/RuleController.java`

- [ ] **Step 1: 先写 `RuleControllerTest` 固定规则接口契约**

在 `RuleControllerTest.java` 中先落以下测试方法：

```java
@Test
void shouldReturnRuleListWithPageResult() throws Exception {
    String adminToken = loginAndExtractToken("admin", "123456");

    jdbcTemplate.update(
            "INSERT INTO rule (id, name, startTime, endTime, lateThreshold, earlyThreshold, repeatLimit, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            1L, "默认考勤规则", "09:00:00", "18:00:00", 10, 10, 3, 1
    );

    mockMvc.perform(get("/api/rule/list")
                    .param("pageNum", "1")
                    .param("pageSize", "10")
                    .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("success"))
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.records[0].name").value("默认考勤规则"));
}

@Test
void shouldAddRuleWhenInputIsValid() throws Exception {
    String adminToken = loginAndExtractToken("admin", "123456");

    mockMvc.perform(post("/api/rule/add")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(APPLICATION_JSON)
                    .content("{\"name\":\"标准工作日规则\",\"startTime\":\"09:00:00\",\"endTime\":\"18:00:00\",\"lateThreshold\":10,\"earlyThreshold\":10,\"repeatLimit\":3}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
}

@Test
void shouldUpdateRuleWhenInputIsValid() throws Exception {
    String adminToken = loginAndExtractToken("admin", "123456");
    jdbcTemplate.update(
            "INSERT INTO rule (id, name, startTime, endTime, lateThreshold, earlyThreshold, repeatLimit, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            1L, "默认考勤规则", "09:00:00", "18:00:00", 10, 10, 3, 1
    );

    mockMvc.perform(put("/api/rule/update")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(APPLICATION_JSON)
                    .content("{\"id\":1,\"name\":\"默认考勤规则-更新\",\"startTime\":\"09:00:00\",\"endTime\":\"18:00:00\",\"lateThreshold\":15,\"earlyThreshold\":10,\"repeatLimit\":3}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
}

@Test
void shouldUpdateRuleStatusWhenInputIsValid() throws Exception {
    String adminToken = loginAndExtractToken("admin", "123456");
    jdbcTemplate.update(
            "INSERT INTO rule (id, name, startTime, endTime, lateThreshold, earlyThreshold, repeatLimit, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            1L, "默认考勤规则", "09:00:00", "18:00:00", 10, 10, 3, 1
    );

    mockMvc.perform(put("/api/rule/status")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(APPLICATION_JSON)
                    .content("{\"id\":1,\"status\":0}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
}

@Test
void shouldFailWhenRuleStatusIsInvalid() throws Exception {
    String adminToken = loginAndExtractToken("admin", "123456");

    mockMvc.perform(put("/api/rule/status")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(APPLICATION_JSON)
                    .content("{\"id\":1,\"status\":2}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("规则状态不合法"));
}

@Test
void shouldReturnForbiddenWhenEmployeeAccessesRuleApi() throws Exception {
    String employeeToken = loginAndExtractToken("zhangsan", "123456");

    mockMvc.perform(get("/api/rule/list")
                    .header("Authorization", "Bearer " + employeeToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(403));
}
```

断言重点固定为：

```java
.andExpect(jsonPath("$.code").value(200))
.andExpect(jsonPath("$.message").value("success"))
.andExpect(jsonPath("$.data.total").value(1))
.andExpect(jsonPath("$.data.records[0].name").value("默认考勤规则"))
```

新增成功后，用 `JdbcTemplate` 断言：

```java
Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM rule WHERE name = ?",
        Integer.class,
        "标准工作日规则"
);
assertEquals(Integer.valueOf(1), count);
```

- [ ] **Step 2: 运行 RED 测试确认当前 `rule` 接口尚未实现**

Run: `mvn "-Dtest=RuleControllerTest" test`

Expected: FAIL，失败原因应为 `RuleController` / `RuleService` 不存在或接口返回不符合预期，而不是测试代码语法错误。

- [ ] **Step 3: 写最小实现让规则管理测试转绿**

`Rule.java` 使用与现有实体一致的 MyBatis-Plus 写法：

```java
@TableName("rule")
public class Rule {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("startTime")
    private LocalTime startTime;

    @TableField("endTime")
    private LocalTime endTime;

    @TableField("lateThreshold")
    private Integer lateThreshold;

    @TableField("earlyThreshold")
    private Integer earlyThreshold;

    @TableField("repeatLimit")
    private Integer repeatLimit;

    @TableField("status")
    private Integer status;
}
```

`RuleMapper.java` 直接实现手写分页：

```java
@Mapper
public interface RuleMapper extends BaseMapper<Rule> {

    @Select({
            "<script>",
            "SELECT COUNT(*) FROM rule",
            "WHERE 1 = 1",
            "<if test='keyword != null and keyword != \"\"'> AND name LIKE CONCAT('%', #{keyword}, '%')</if>",
            "<if test='status != null'> AND status = #{status}</if>",
            "</script>"
    })
    long countByQuery(@Param("keyword") String keyword, @Param("status") Integer status);

    @Select({
            "<script>",
            "SELECT id, name, startTime, endTime, lateThreshold, earlyThreshold, repeatLimit, status",
            "FROM rule",
            "WHERE 1 = 1",
            "<if test='keyword != null and keyword != \"\"'> AND name LIKE CONCAT('%', #{keyword}, '%')</if>",
            "<if test='status != null'> AND status = #{status}</if>",
            "ORDER BY id ASC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<Rule> selectPageByQuery(@Param("keyword") String keyword,
                                 @Param("status") Integer status,
                                 @Param("limit") int limit,
                                 @Param("offset") int offset);
}
```

`RuleValidationSupport.java` 保持小而专注：

```java
@Component
public class RuleValidationSupport {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public RuleQueryDTO validateQuery(RuleQueryDTO dto) {
        RuleQueryDTO safe = dto == null ? new RuleQueryDTO() : dto;
        safe.setKeyword(normalize(safe.getKeyword()));
        safe.setPageNum(safe.getPageNum() == null || safe.getPageNum() < 1 ? 1 : safe.getPageNum());
        safe.setPageSize(safe.getPageSize() == null || safe.getPageSize() < 1 ? 10 : safe.getPageSize());
        if (safe.getStatus() != null && safe.getStatus() != 0 && safe.getStatus() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "规则状态不合法");
        }
        return safe;
    }

    public RuleSaveDTO validateSave(RuleSaveDTO dto, boolean requireId) {
        if (dto == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请求参数错误");
        }
        if (requireId && dto.getId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "规则ID不能为空");
        }
        dto.setName(normalize(dto.getName()));
        dto.setStartTime(normalize(dto.getStartTime()));
        dto.setEndTime(normalize(dto.getEndTime()));
        if (!StringUtils.hasText(dto.getName())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "规则名称不能为空");
        }
        if (!StringUtils.hasText(dto.getStartTime()) || !StringUtils.hasText(dto.getEndTime())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "上下班时间不能为空");
        }
        if (dto.getLateThreshold() == null || dto.getLateThreshold() < 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "迟到阈值不合法");
        }
        if (dto.getEarlyThreshold() == null || dto.getEarlyThreshold() < 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "早退阈值不合法");
        }
        if (dto.getRepeatLimit() == null || dto.getRepeatLimit() < 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "重复打卡阈值不合法");
        }
        parseTime(dto.getStartTime(), "上班时间");
        parseTime(dto.getEndTime(), "下班时间");
        return dto;
    }

    public RuleStatusDTO validateStatus(RuleStatusDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "规则ID不能为空");
        }
        if (dto.getStatus() == null || (dto.getStatus() != 0 && dto.getStatus() != 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "规则状态不合法");
        }
        return dto;
    }

    public LocalTime parseTime(String value, String fieldName) {
        try {
            return LocalTime.parse(value, TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), fieldName + "格式错误");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
```

`RuleService.java` 和 `RuleController.java` 的核心签名固定为：

```java
public interface RuleService {
    PageResult<RuleVO> list(RuleQueryDTO queryDTO);
    void add(RuleSaveDTO saveDTO);
    void update(RuleSaveDTO saveDTO);
    void updateStatus(RuleStatusDTO statusDTO);
    Rule getEnabledRule();
}
```

```java
@RestController
@RequestMapping("/api/rule")
public class RuleController {

    @GetMapping("/list")
    public Result<PageResult<RuleVO>> list(RuleQueryDTO queryDTO) {
        return Result.success(ruleService.list(queryDTO));
    }

    @PostMapping("/add")
    public Result<Void> add(@RequestBody(required = false) RuleSaveDTO saveDTO) {
        ruleService.add(saveDTO);
        return Result.success(null);
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody(required = false) RuleSaveDTO saveDTO) {
        ruleService.update(saveDTO);
        return Result.success(null);
    }

    @PutMapping("/status")
    public Result<Void> updateStatus(@RequestBody(required = false) RuleStatusDTO statusDTO) {
        ruleService.updateStatus(statusDTO);
        return Result.success(null);
    }
}
```

- [ ] **Step 4: 重跑规则管理测试**

Run: `mvn "-Dtest=RuleControllerTest" test`

Expected: `BUILD SUCCESS`

## Task 3: 基础规则判定与决策追踪 RED/GREEN

**Files:**
- Create: `src/main/java/com/quyong/attendance/module/exceptiondetect/dto/RuleCheckDTO.java`
- Create: `src/main/java/com/quyong/attendance/module/exceptiondetect/entity/AttendanceException.java`
- Create: `src/main/java/com/quyong/attendance/module/exceptiondetect/mapper/AttendanceExceptionMapper.java`
- Create: `src/main/java/com/quyong/attendance/module/exceptiondetect/vo/ExceptionDecisionVO.java`
- Create: `src/main/java/com/quyong/attendance/module/exceptiondetect/support/ExceptionValidationSupport.java`
- Create: `src/main/java/com/quyong/attendance/module/model/trace/entity/DecisionTrace.java`
- Create: `src/main/java/com/quyong/attendance/module/model/trace/mapper/DecisionTraceMapper.java`
- Create: `src/main/java/com/quyong/attendance/module/model/trace/service/DecisionTraceService.java`
- Create: `src/main/java/com/quyong/attendance/module/model/trace/service/impl/DecisionTraceServiceImpl.java`
- Create: `src/main/java/com/quyong/attendance/module/model/trace/vo/DecisionTraceVO.java`
- Create: `src/main/java/com/quyong/attendance/module/exceptiondetect/service/ExceptionAnalysisOrchestrator.java`
- Create: `src/main/java/com/quyong/attendance/module/exceptiondetect/service/impl/ExceptionAnalysisOrchestratorImpl.java`
- Create: `src/main/java/com/quyong/attendance/module/exceptiondetect/controller/ExceptionController.java`
- Create: `src/test/java/com/quyong/attendance/ExceptionControllerTest.java`

- [ ] **Step 1: 先写 `rule-check` 的失败测试**

在 `ExceptionControllerTest.java` 中先补以下测试：

```java
@Test
void shouldCreateLateExceptionByRuleCheck() throws Exception {
    String adminToken = loginAndExtractToken("admin", "123456");

    mockMvc.perform(post("/api/exception/rule-check")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(APPLICATION_JSON)
                    .content("{\"recordId\":2004}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.type").value("LATE"));
}

@Test
void shouldReturnNullWhenRuleCheckMissesAnyException() throws Exception {
    String adminToken = loginAndExtractToken("admin", "123456");

    mockMvc.perform(post("/api/exception/rule-check")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(APPLICATION_JSON)
                    .content("{\"recordId\":2001}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").doesNotExist());
}

@Test
void shouldReuseExistingRuleExceptionForSameRecord() throws Exception {
    String adminToken = loginAndExtractToken("admin", "123456");

    mockMvc.perform(post("/api/exception/rule-check")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(APPLICATION_JSON)
                    .content("{\"recordId\":2004}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

    mockMvc.perform(post("/api/exception/rule-check")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(APPLICATION_JSON)
                    .content("{\"recordId\":2004}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

    Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM attendanceException WHERE recordId = 2004 AND sourceType = 'RULE'",
            Integer.class
    );
    assertEquals(Integer.valueOf(1), count);
}
```

其中 `shouldCreateLateExceptionByRuleCheck()` 的核心断言固定为：

```java
mockMvc.perform(post("/api/exception/rule-check")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(APPLICATION_JSON)
                .content("{\"recordId\":2004}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.type").value("LATE"))
        .andExpect(jsonPath("$.data.riskLevel").value("MEDIUM"))
        .andExpect(jsonPath("$.data.sourceType").value("RULE"))
        .andExpect(jsonPath("$.data.processStatus").value("PENDING"));

Integer exceptionCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM attendanceException WHERE recordId = 2004 AND sourceType = 'RULE'",
        Integer.class
);
assertEquals(Integer.valueOf(1), exceptionCount);

String recordStatus = jdbcTemplate.queryForObject(
        "SELECT status FROM attendanceRecord WHERE id = 2004",
        String.class
);
assertEquals("ABNORMAL", recordStatus);
```

未命中用例固定断言：

```java
.andExpect(jsonPath("$.code").value(200))
.andExpect(jsonPath("$.data").doesNotExist());
```

- [ ] **Step 2: 运行 RED 测试**

Run: `mvn "-Dtest=ExceptionControllerTest" test`

Expected: FAIL，失败原因应为 `/api/exception/rule-check` 不存在或返回结构不符。

- [ ] **Step 3: 写最小实现让 `rule-check` 测试通过**

`AttendanceException.java` 和 `DecisionTrace.java` 与现有实体风格保持一致：

```java
@TableName("attendanceException")
public class AttendanceException {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("recordId")
    private Long recordId;

    @TableField("userId")
    private Long userId;

    @TableField("type")
    private String type;

    @TableField("riskLevel")
    private String riskLevel;

    @TableField("sourceType")
    private String sourceType;

    @TableField("description")
    private String description;

    @TableField("processStatus")
    private String processStatus;

    @TableField("createTime")
    private LocalDateTime createTime;
}
```

```java
@TableName("decisionTrace")
public class DecisionTrace {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("businessType")
    private String businessType;

    @TableField("businessId")
    private Long businessId;

    @TableField("ruleResult")
    private String ruleResult;

    @TableField("modelResult")
    private String modelResult;

    @TableField("finalDecision")
    private String finalDecision;

    @TableField("confidenceScore")
    private BigDecimal confidenceScore;

    @TableField("decisionReason")
    private String decisionReason;

    @TableField("createTime")
    private LocalDateTime createTime;
}
```

`DecisionTraceService.java` 固定为：

```java
public interface DecisionTraceService {
    void save(String businessType,
              Long businessId,
              String ruleResult,
              String modelResult,
              String finalDecision,
              BigDecimal confidenceScore,
              String decisionReason);

    List<DecisionTraceVO> list(String businessType, Long businessId);
}
```

`ExceptionController.java` 先只放 `rule-check`：

```java
@RestController
@RequestMapping("/api/exception")
public class ExceptionController {

    private final ExceptionAnalysisOrchestrator exceptionAnalysisOrchestrator;

    @PostMapping("/rule-check")
    public Result<ExceptionDecisionVO> ruleCheck(@RequestBody(required = false) RuleCheckDTO dto) {
        return Result.success(exceptionAnalysisOrchestrator.ruleCheck(dto));
    }
}
```

`ExceptionAnalysisOrchestratorImpl.ruleCheck(RuleCheckDTO dto)` 的最小逻辑固定为：

```java
@Override
@Transactional
public ExceptionDecisionVO ruleCheck(RuleCheckDTO dto) {
    RuleCheckDTO validatedDTO = exceptionValidationSupport.validateRuleCheck(dto);
    AttendanceRecord record = attendanceRecordMapper.selectById(validatedDTO.getRecordId());
    if (record == null) {
        throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "考勤记录不存在");
    }

    Rule rule = ruleService.getEnabledRule();
    String type = detectRuleType(record, rule);
    if (type == null) {
        return null;
    }

    AttendanceException existing = attendanceExceptionMapper.selectOne(
            Wrappers.<AttendanceException>lambdaQuery()
                    .eq(AttendanceException::getRecordId, record.getId())
                    .eq(AttendanceException::getSourceType, "RULE")
                    .last("LIMIT 1")
    );
    if (existing != null) {
        ensureRuleDecisionTrace(existing);
        return toDecisionVO(existing, null);
    }

    AttendanceException attendanceException = new AttendanceException();
    attendanceException.setRecordId(record.getId());
    attendanceException.setUserId(record.getUserId());
    attendanceException.setType(type);
    attendanceException.setRiskLevel(resolveRuleRiskLevel(type));
    attendanceException.setSourceType("RULE");
    attendanceException.setDescription(resolveRuleDescription(type));
    attendanceException.setProcessStatus("PENDING");
    attendanceExceptionMapper.insert(attendanceException);

    record.setStatus("ABNORMAL");
    attendanceRecordMapper.updateById(record);

    decisionTraceService.save(
            "ATTENDANCE_EXCEPTION",
            attendanceException.getId(),
            attendanceException.getDescription(),
            null,
            attendanceException.getType(),
            null,
            attendanceException.getDescription()
    );
    return toDecisionVO(attendanceException, null);
}
```

规则判定函数只覆盖当前设计确认的最小集合：

```java
private String detectRuleType(AttendanceRecord record, Rule rule) {
    if (isIllegalTime(record)) {
        return "ILLEGAL_TIME";
    }
    if (isLate(record, rule)) {
        return "LATE";
    }
    if (isEarlyLeave(record, rule)) {
        return "EARLY_LEAVE";
    }
    if (isRepeatCheck(record, rule)) {
        return "REPEAT_CHECK";
    }
    return null;
}

private boolean isIllegalTime(AttendanceRecord record) {
    LocalTime time = record.getCheckTime().toLocalTime();
    return time.isBefore(LocalTime.of(5, 0)) || time.isAfter(LocalTime.of(23, 0));
}

private boolean isLate(AttendanceRecord record, Rule rule) {
    return "IN".equals(record.getCheckType())
            && record.getCheckTime().toLocalTime().isAfter(rule.getStartTime().plusMinutes(rule.getLateThreshold()));
}

private boolean isEarlyLeave(AttendanceRecord record, Rule rule) {
    return "OUT".equals(record.getCheckType())
            && record.getCheckTime().toLocalTime().isBefore(rule.getEndTime().minusMinutes(rule.getEarlyThreshold()));
}

private boolean isRepeatCheck(AttendanceRecord record, Rule rule) {
    Long count = attendanceRecordMapper.selectCount(
            Wrappers.<AttendanceRecord>lambdaQuery()
                    .eq(AttendanceRecord::getUserId, record.getUserId())
                    .eq(AttendanceRecord::getCheckType, record.getCheckType())
                    .ge(AttendanceRecord::getCheckTime, record.getCheckTime().minusMinutes(rule.getRepeatLimit()))
                    .lt(AttendanceRecord::getCheckTime, record.getCheckTime())
    );
    return count != null && count > 0;
}

private String resolveRuleRiskLevel(String type) {
    if ("ILLEGAL_TIME".equals(type)) {
        return "HIGH";
    }
    if ("LATE".equals(type) || "EARLY_LEAVE".equals(type)) {
        return "MEDIUM";
    }
    return "LOW";
}

private String resolveRuleDescription(String type) {
    if ("LATE".equals(type)) {
        return "超过上班时间阈值，判定为迟到";
    }
    if ("EARLY_LEAVE".equals(type)) {
        return "早于下班时间阈值，判定为早退";
    }
    if ("ILLEGAL_TIME".equals(type)) {
        return "非法时间段打卡";
    }
    return "短时间内重复打卡";
}

private void ensureRuleDecisionTrace(AttendanceException attendanceException) {
    if (!decisionTraceService.list("ATTENDANCE_EXCEPTION", attendanceException.getId()).isEmpty()) {
        return;
    }
    decisionTraceService.save(
            "ATTENDANCE_EXCEPTION",
            attendanceException.getId(),
            attendanceException.getDescription(),
            null,
            attendanceException.getType(),
            null,
            attendanceException.getDescription()
    );
}
```

- [ ] **Step 4: 重跑 `rule-check` 测试**

Run: `mvn "-Dtest=ExceptionControllerTest" test`

Expected: `rule-check` 相关测试 PASS。

## Task 4: 复杂异常分析与模型支撑 RED/GREEN

**Files:**
- Create: `src/main/java/com/quyong/attendance/module/exceptiondetect/dto/ComplexCheckDTO.java`
- Create: `src/main/java/com/quyong/attendance/module/exceptiondetect/dto/RiskFeaturesDTO.java`
- Create: `src/main/java/com/quyong/attendance/module/exceptiondetect/entity/ExceptionAnalysis.java`
- Create: `src/main/java/com/quyong/attendance/module/exceptiondetect/mapper/ExceptionAnalysisMapper.java`
- Create: `src/main/java/com/quyong/attendance/module/model/prompt/entity/PromptTemplate.java`
- Create: `src/main/java/com/quyong/attendance/module/model/prompt/mapper/PromptTemplateMapper.java`
- Create: `src/main/java/com/quyong/attendance/module/model/log/entity/ModelCallLog.java`
- Create: `src/main/java/com/quyong/attendance/module/model/log/mapper/ModelCallLogMapper.java`
- Create: `src/main/java/com/quyong/attendance/module/model/log/service/ModelCallLogService.java`
- Create: `src/main/java/com/quyong/attendance/module/model/log/service/impl/ModelCallLogServiceImpl.java`
- Create: `src/main/java/com/quyong/attendance/module/model/gateway/dto/ModelInvokeRequest.java`
- Create: `src/main/java/com/quyong/attendance/module/model/gateway/dto/ModelInvokeResponse.java`
- Create: `src/main/java/com/quyong/attendance/module/model/gateway/service/ModelGateway.java`
- Create: `src/main/java/com/quyong/attendance/module/model/gateway/service/MockModelGateway.java`
- Modify: `src/main/java/com/quyong/attendance/module/exceptiondetect/service/impl/ExceptionAnalysisOrchestratorImpl.java`
- Modify: `src/main/java/com/quyong/attendance/module/exceptiondetect/controller/ExceptionController.java`
- Modify: `src/test/java/com/quyong/attendance/ExceptionControllerTest.java`

- [ ] **Step 1: 为 `complex-check` 和失败降级补失败测试**

在 `ExceptionControllerTest.java` 中继续补以下测试：

```java
@MockBean
private ModelGateway modelGateway;

@Test
void shouldCreateProxyCheckinExceptionByComplexCheck() throws Exception {
    String adminToken = loginAndExtractToken("admin", "123456");
    when(modelGateway.invoke(any(ModelInvokeRequest.class))).thenReturn(mockResponse("PROXY_CHECKIN", "HIGH", "设备与地点异常共同提升风险"));

    mockMvc.perform(post("/api/exception/complex-check")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(APPLICATION_JSON)
                    .content("{\"recordId\":2006,\"userId\":1002,\"riskFeatures\":{\"faceScore\":81.2,\"deviceChanged\":true,\"locationChanged\":true,\"historyAbnormalCount\":2}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.type").value("PROXY_CHECKIN"));
}

@Test
void shouldFallbackWhenModelGatewayFailsDuringComplexCheck() throws Exception {
    String adminToken = loginAndExtractToken("admin", "123456");
    when(modelGateway.invoke(any(ModelInvokeRequest.class))).thenThrow(new BusinessException(400, "外部模型调用失败"));

    mockMvc.perform(post("/api/exception/complex-check")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(APPLICATION_JSON)
                    .content("{\"recordId\":2006,\"userId\":1002,\"riskFeatures\":{\"faceScore\":81.2,\"deviceChanged\":true,\"locationChanged\":true,\"historyAbnormalCount\":2}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.sourceType").value("MODEL_FALLBACK"));
}

private ModelInvokeResponse mockResponse(String conclusion, String riskLevel, String reasonSummary) {
    ModelInvokeResponse response = new ModelInvokeResponse();
    response.setConclusion(conclusion);
    response.setRiskLevel(riskLevel);
    response.setConfidenceScore(new BigDecimal("92.50"));
    response.setDecisionReason("设备异常、地点异常且行为模式偏离历史规律");
    response.setReasonSummary(reasonSummary);
    response.setActionSuggestion("建议优先人工复核");
    response.setSimilarCaseSummary("存在相似设备异常与低分值组合案例");
    response.setRawResponse("{\"conclusion\":\"" + conclusion + "\"}");
    return response;
}
```

成功用例固定断言：

```java
when(modelGateway.invoke(any(ModelInvokeRequest.class))).thenReturn(mockResponse("PROXY_CHECKIN", "HIGH", "设备与地点异常共同提升风险"));

mockMvc.perform(post("/api/exception/complex-check")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(APPLICATION_JSON)
                .content("{\"recordId\":2006,\"userId\":1002,\"riskFeatures\":{\"faceScore\":81.2,\"deviceChanged\":true,\"locationChanged\":true,\"historyAbnormalCount\":2}}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.type").value("PROXY_CHECKIN"))
        .andExpect(jsonPath("$.data.riskLevel").value("HIGH"))
        .andExpect(jsonPath("$.data.sourceType").value("MODEL"))
        .andExpect(jsonPath("$.data.modelConclusion").value("PROXY_CHECKIN"))
        .andExpect(jsonPath("$.data.reasonSummary").value("设备与地点异常共同提升风险"))
        .andExpect(jsonPath("$.data.actionSuggestion").value("建议优先人工复核"))
        .andExpect(jsonPath("$.data.confidenceScore").value(92.5));
```

降级用例固定断言：

```java
when(modelGateway.invoke(any(ModelInvokeRequest.class)))
        .thenThrow(new BusinessException(400, "外部模型调用失败"));

mockMvc.perform(post("/api/exception/complex-check")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(APPLICATION_JSON)
                .content("{\"recordId\":2006,\"userId\":1002,\"riskFeatures\":{\"faceScore\":81.2,\"deviceChanged\":true,\"locationChanged\":true,\"historyAbnormalCount\":2}}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.sourceType").value("MODEL_FALLBACK"))
        .andExpect(jsonPath("$.data.processStatus").value("PENDING"));

Integer failedLogCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM modelCallLog WHERE businessType = 'EXCEPTION_ANALYSIS' AND status = 'FAILED' AND errorMessage = '外部模型调用失败'",
        Integer.class
);
assertEquals(Integer.valueOf(1), failedLogCount);
```

- [ ] **Step 2: 运行 RED 测试**

Run: `mvn "-Dtest=ExceptionControllerTest" test`

Expected: FAIL，失败原因应为 `complex-check`、模板查询、模型日志或分析结果写入链路尚未实现。

- [ ] **Step 3: 写最小实现让复杂分析测试通过**

`ModelInvokeRequest.java` 和 `ModelInvokeResponse.java` 固定为结构化对象，不在 orchestrator 中直接拼接松散 `Map`：

```java
public class ModelInvokeRequest {
    private String sceneType;
    private Long businessId;
    private Long promptTemplateId;
    private String promptVersion;
    private String promptContent;
    private String inputSummary;
}
```

```java
public class ModelInvokeResponse {
    private String conclusion;
    private String riskLevel;
    private BigDecimal confidenceScore;
    private String decisionReason;
    private String reasonSummary;
    private String actionSuggestion;
    private String similarCaseSummary;
    private String rawResponse;
}
```

`MockModelGateway.java` 给出稳定默认值，保证应用在未接第三方模型时也可运行：

```java
@Service
public class MockModelGateway implements ModelGateway {

    @Override
    public ModelInvokeResponse invoke(ModelInvokeRequest request) {
        ModelInvokeResponse response = new ModelInvokeResponse();
        response.setConclusion("PROXY_CHECKIN");
        response.setRiskLevel("HIGH");
        response.setConfidenceScore(new BigDecimal("92.50"));
        response.setDecisionReason("设备异常、地点异常且行为模式偏离历史规律");
        response.setReasonSummary("设备与地点异常共同提升风险");
        response.setActionSuggestion("建议优先人工复核");
        response.setSimilarCaseSummary("存在相似设备异常与低分值组合案例");
        response.setRawResponse("{\"conclusion\":\"PROXY_CHECKIN\"}");
        return response;
    }
}
```

`ModelCallLogService.java` 只封装本轮实际需要的两个写入入口：

```java
public interface ModelCallLogService {
    void logSuccess(String businessType, Long businessId, Long promptTemplateId, String inputSummary, String outputSummary, Integer latencyMs);
    void logFailure(String businessType, Long businessId, Long promptTemplateId, String inputSummary, String errorMessage, Integer latencyMs);
}
```

`ExceptionAnalysisOrchestratorImpl.complexCheck(ComplexCheckDTO dto)` 的最小逻辑固定为：

```java
@Override
@Transactional
public ExceptionDecisionVO complexCheck(ComplexCheckDTO dto) {
    ComplexCheckDTO validatedDTO = exceptionValidationSupport.validateComplexCheck(dto);
    AttendanceRecord record = attendanceRecordMapper.selectById(validatedDTO.getRecordId());
    if (record == null) {
        throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "考勤记录不存在");
    }
    if (!record.getUserId().equals(validatedDTO.getUserId())) {
        throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "考勤记录用户与请求用户不一致");
    }

    PromptTemplate promptTemplate = promptTemplateMapper.selectEnabledBySceneType("EXCEPTION_ANALYSIS");
    if (promptTemplate == null) {
        throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "未找到启用中的异常分析模板");
    }

    String inputSummary = buildInputSummary(record, validatedDTO.getRiskFeatures());
    long startAt = System.currentTimeMillis();
    try {
        ModelInvokeResponse response = modelGateway.invoke(buildModelRequest(record, promptTemplate, inputSummary));
        AttendanceException attendanceException = saveComplexException(record, response, "MODEL");
        saveAnalysis(attendanceException.getId(), promptTemplate, inputSummary, response);
        modelCallLogService.logSuccess("EXCEPTION_ANALYSIS", attendanceException.getId(), promptTemplate.getId(), inputSummary, response.getRawResponse(), (int) (System.currentTimeMillis() - startAt));
        decisionTraceService.save("ATTENDANCE_EXCEPTION", attendanceException.getId(), buildRuleFeatureSummary(record, validatedDTO.getRiskFeatures()), response.getRawResponse(), response.getConclusion(), response.getConfidenceScore(), response.getDecisionReason());
        record.setStatus("ABNORMAL");
        attendanceRecordMapper.updateById(record);
        return toDecisionVO(attendanceException, response);
    } catch (Exception exception) {
        AttendanceException fallbackException = saveComplexException(record, null, "MODEL_FALLBACK");
        modelCallLogService.logFailure("EXCEPTION_ANALYSIS", fallbackException.getId(), promptTemplate.getId(), inputSummary, exception.getMessage(), (int) (System.currentTimeMillis() - startAt));
        decisionTraceService.save("ATTENDANCE_EXCEPTION", fallbackException.getId(), buildRuleFeatureSummary(record, validatedDTO.getRiskFeatures()), null, fallbackException.getType(), null, "模型调用失败，已转入保守降级结果");
        record.setStatus("ABNORMAL");
        attendanceRecordMapper.updateById(record);
        return toFallbackDecisionVO(fallbackException);
    }
}
```

`buildInputSummary(AttendanceRecord record, RiskFeaturesDTO riskFeatures)` 至少包含以下内容，确保下游字段可复用且不直接落敏感原文：

```java
private String buildInputSummary(AttendanceRecord record, RiskFeaturesDTO riskFeatures) {
    long historyAbnormalCount = attendanceExceptionMapper.selectCount(
            Wrappers.<AttendanceException>lambdaQuery().eq(AttendanceException::getUserId, record.getUserId())
    );
    return "recordId=" + record.getId()
            + ", userId=" + record.getUserId()
            + ", checkType=" + record.getCheckType()
            + ", checkTime=" + record.getCheckTime()
            + ", deviceId=" + record.getDeviceId()
            + ", location=" + record.getLocation()
            + ", faceScore=" + record.getFaceScore()
            + ", clientHistoryAbnormalCount=" + (riskFeatures == null ? null : riskFeatures.getHistoryAbnormalCount())
            + ", actualHistoryAbnormalCount=" + historyAbnormalCount;
}

private ModelInvokeRequest buildModelRequest(AttendanceRecord record, PromptTemplate promptTemplate, String inputSummary) {
    ModelInvokeRequest request = new ModelInvokeRequest();
    request.setSceneType("EXCEPTION_ANALYSIS");
    request.setBusinessId(record.getId());
    request.setPromptTemplateId(promptTemplate.getId());
    request.setPromptVersion(promptTemplate.getVersion());
    request.setPromptContent(promptTemplate.getContent());
    request.setInputSummary(inputSummary);
    return request;
}

private AttendanceException saveComplexException(AttendanceRecord record, ModelInvokeResponse response, String sourceType) {
    AttendanceException attendanceException = new AttendanceException();
    attendanceException.setRecordId(record.getId());
    attendanceException.setUserId(record.getUserId());
    attendanceException.setType(response == null ? "COMPLEX_ATTENDANCE_RISK" : response.getConclusion());
    attendanceException.setRiskLevel(response == null ? "MEDIUM" : response.getRiskLevel());
    attendanceException.setSourceType(sourceType);
    attendanceException.setDescription(response == null ? "模型调用失败，已转人工处理" : response.getDecisionReason());
    attendanceException.setProcessStatus("PENDING");
    attendanceExceptionMapper.insert(attendanceException);
    return attendanceException;
}

private void saveAnalysis(Long exceptionId, PromptTemplate promptTemplate, String inputSummary, ModelInvokeResponse response) {
    ExceptionAnalysis analysis = new ExceptionAnalysis();
    analysis.setExceptionId(exceptionId);
    analysis.setPromptTemplateId(promptTemplate.getId());
    analysis.setInputSummary(inputSummary);
    analysis.setModelResult(response.getRawResponse());
    analysis.setModelConclusion(response.getConclusion());
    analysis.setConfidenceScore(response.getConfidenceScore());
    analysis.setDecisionReason(response.getDecisionReason());
    analysis.setSuggestion(response.getActionSuggestion());
    analysis.setReasonSummary(response.getReasonSummary());
    analysis.setActionSuggestion(response.getActionSuggestion());
    analysis.setSimilarCaseSummary(response.getSimilarCaseSummary());
    analysis.setPromptVersion(promptTemplate.getVersion());
    exceptionAnalysisMapper.insert(analysis);
}

private String buildRuleFeatureSummary(AttendanceRecord record, RiskFeaturesDTO riskFeatures) {
    return "deviceId=" + record.getDeviceId()
            + ", location=" + record.getLocation()
            + ", faceScore=" + record.getFaceScore()
            + ", clientDeviceChanged=" + (riskFeatures == null ? null : riskFeatures.getDeviceChanged())
            + ", clientLocationChanged=" + (riskFeatures == null ? null : riskFeatures.getLocationChanged());
}

private ExceptionDecisionVO toFallbackDecisionVO(AttendanceException attendanceException) {
    ExceptionDecisionVO vo = new ExceptionDecisionVO();
    vo.setExceptionId(attendanceException.getId());
    vo.setType(attendanceException.getType());
    vo.setRiskLevel(attendanceException.getRiskLevel());
    vo.setSourceType(attendanceException.getSourceType());
    vo.setProcessStatus(attendanceException.getProcessStatus());
    vo.setReasonSummary("模型调用失败，已转人工复核");
    vo.setActionSuggestion("建议管理员查看原始记录并人工确认");
    return vo;
}
```

- [ ] **Step 4: 重跑复杂分析测试**

Run: `mvn "-Dtest=ExceptionControllerTest" test`

Expected: `complex-check` 成功和失败降级用例 PASS。

## Task 5: 异常查询接口、分页与 Bean 装配 RED/GREEN

**Files:**
- Create: `src/main/java/com/quyong/attendance/module/exceptiondetect/dto/ExceptionQueryDTO.java`
- Create: `src/main/java/com/quyong/attendance/module/exceptiondetect/vo/AttendanceExceptionVO.java`
- Create: `src/main/java/com/quyong/attendance/module/exceptiondetect/vo/ExceptionAnalysisBriefVO.java`
- Create: `src/main/java/com/quyong/attendance/module/exceptiondetect/service/ExceptionQueryService.java`
- Create: `src/main/java/com/quyong/attendance/module/exceptiondetect/service/impl/ExceptionQueryServiceImpl.java`
- Modify: `src/main/java/com/quyong/attendance/module/exceptiondetect/mapper/AttendanceExceptionMapper.java`
- Modify: `src/main/java/com/quyong/attendance/module/exceptiondetect/mapper/ExceptionAnalysisMapper.java`
- Modify: `src/main/java/com/quyong/attendance/module/exceptiondetect/controller/ExceptionController.java`
- Modify: `src/test/java/com/quyong/attendance/ExceptionControllerTest.java`
- Modify: `src/test/java/com/quyong/attendance/ModuleSkeletonBeansTest.java`

- [ ] **Step 1: 先补异常查询与安全边界失败测试**

在 `ExceptionControllerTest.java` 中继续补以下测试：

```java
@Test
void shouldReturnExceptionList() throws Exception {
    String adminToken = loginAndExtractToken("admin", "123456");

    mockMvc.perform(get("/api/exception/list")
                    .param("pageNum", "1")
                    .param("pageSize", "10")
                    .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.total").value(1));
}

@Test
void shouldReturnExceptionDetail() throws Exception {
    String adminToken = loginAndExtractToken("admin", "123456");

    mockMvc.perform(get("/api/exception/3001")
                    .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value(3001));
}

@Test
void shouldReturnDecisionTrace() throws Exception {
    String adminToken = loginAndExtractToken("admin", "123456");

    mockMvc.perform(get("/api/exception/3001/decision-trace")
                    .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
}

@Test
void shouldReturnAnalysisBrief() throws Exception {
    String adminToken = loginAndExtractToken("admin", "123456");

    mockMvc.perform(get("/api/exception/3001/analysis-brief")
                    .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
}

@Test
void shouldReturnUnauthorizedWhenAccessingExceptionApiWithoutToken() throws Exception {
    mockMvc.perform(get("/api/exception/list"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401));
}

@Test
void shouldReturnForbiddenWhenEmployeeAccessesExceptionApi() throws Exception {
    String employeeToken = loginAndExtractToken("zhangsan", "123456");

    mockMvc.perform(get("/api/exception/list")
                    .header("Authorization", "Bearer " + employeeToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(403));
}
```

列表接口断言固定为：

```java
mockMvc.perform(get("/api/exception/list")
                .param("pageNum", "1")
                .param("pageSize", "10")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.records[0].id").value(3001))
        .andExpect(jsonPath("$.data.records[0].type").value("PROXY_CHECKIN"));
```

`ModuleSkeletonBeansTest.java` 需要追加以下断言：

```java
assertBeanPresent("com.quyong.attendance.module.exceptiondetect.controller.ExceptionController");
assertBeanPresent("com.quyong.attendance.module.exceptiondetect.controller.RuleController");
assertBeanPresent("com.quyong.attendance.module.exceptiondetect.service.impl.ExceptionAnalysisOrchestratorImpl");
assertBeanPresent("com.quyong.attendance.module.exceptiondetect.service.impl.ExceptionQueryServiceImpl");
assertBeanPresent("com.quyong.attendance.module.exceptiondetect.service.impl.RuleServiceImpl");
assertBeanPresent("com.quyong.attendance.module.model.trace.service.impl.DecisionTraceServiceImpl");
```

- [ ] **Step 2: 运行 RED 测试**

Run: `mvn "-Dtest=ExceptionControllerTest,ModuleSkeletonBeansTest" test`

Expected: FAIL，失败原因应为查询 DTO/VO、分页查询或 GET 接口尚未实现。

- [ ] **Step 3: 写最小实现让查询与 Bean 测试转绿**

`AttendanceExceptionMapper.java` 补齐分页查询方法：

```java
@Select({
        "<script>",
        "SELECT COUNT(*) FROM attendanceException",
        "WHERE 1 = 1",
        "<if test='type != null and type != \"\"'> AND type = #{type}</if>",
        "<if test='riskLevel != null and riskLevel != \"\"'> AND riskLevel = #{riskLevel}</if>",
        "<if test='processStatus != null and processStatus != \"\"'> AND processStatus = #{processStatus}</if>",
        "<if test='userId != null'> AND userId = #{userId}</if>",
        "</script>"
})
long countByQuery(@Param("type") String type,
                  @Param("riskLevel") String riskLevel,
                  @Param("processStatus") String processStatus,
                  @Param("userId") Long userId);

@Select({
        "<script>",
        "SELECT id, recordId, userId, type, riskLevel, sourceType, description, processStatus, createTime",
        "FROM attendanceException",
        "WHERE 1 = 1",
        "<if test='type != null and type != \"\"'> AND type = #{type}</if>",
        "<if test='riskLevel != null and riskLevel != \"\"'> AND riskLevel = #{riskLevel}</if>",
        "<if test='processStatus != null and processStatus != \"\"'> AND processStatus = #{processStatus}</if>",
        "<if test='userId != null'> AND userId = #{userId}</if>",
        "ORDER BY createTime DESC, id DESC",
        "LIMIT #{limit} OFFSET #{offset}",
        "</script>"
})
List<AttendanceException> selectPageByQuery(@Param("type") String type,
                                            @Param("riskLevel") String riskLevel,
                                            @Param("processStatus") String processStatus,
                                            @Param("userId") Long userId,
                                            @Param("limit") int limit,
                                            @Param("offset") int offset);
```

`ExceptionQueryServiceImpl.java` 固定为简单查询门面：

```java
@Service
public class ExceptionQueryServiceImpl implements ExceptionQueryService {

    @Override
    public PageResult<AttendanceExceptionVO> list(ExceptionQueryDTO queryDTO) {
        ExceptionQueryDTO validatedDTO = exceptionValidationSupport.validateQuery(queryDTO);
        int offset = (validatedDTO.getPageNum() - 1) * validatedDTO.getPageSize();
        long total = attendanceExceptionMapper.countByQuery(
                validatedDTO.getType(),
                validatedDTO.getRiskLevel(),
                validatedDTO.getProcessStatus(),
                validatedDTO.getUserId()
        );
        List<AttendanceExceptionVO> records = attendanceExceptionMapper.selectPageByQuery(
                        validatedDTO.getType(),
                        validatedDTO.getRiskLevel(),
                        validatedDTO.getProcessStatus(),
                        validatedDTO.getUserId(),
                        validatedDTO.getPageSize(),
                        offset
                ).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return new PageResult<AttendanceExceptionVO>(total, records);
    }

    @Override
    public AttendanceExceptionVO getById(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "异常记录不存在");
        }
        AttendanceException entity = attendanceExceptionMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "异常记录不存在");
        }
        return toVO(entity);
    }

    @Override
    public ExceptionAnalysisBriefVO getAnalysisBrief(Long id) {
        ExceptionAnalysis analysis = exceptionAnalysisMapper.selectOne(
                Wrappers.<ExceptionAnalysis>lambdaQuery()
                        .eq(ExceptionAnalysis::getExceptionId, id)
                        .orderByDesc(ExceptionAnalysis::getCreateTime)
                        .last("LIMIT 1")
        );
        if (analysis == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "异常分析摘要不存在");
        }
        ExceptionAnalysisBriefVO vo = new ExceptionAnalysisBriefVO();
        vo.setModelConclusion(analysis.getModelConclusion());
        vo.setReasonSummary(analysis.getReasonSummary());
        vo.setActionSuggestion(analysis.getActionSuggestion());
        vo.setSimilarCaseSummary(analysis.getSimilarCaseSummary());
        vo.setPromptVersion(analysis.getPromptVersion());
        vo.setConfidenceScore(analysis.getConfidenceScore());
        return vo;
    }
}
```

`ExceptionController.java` 在已有 `rule-check` / `complex-check` 基础上补齐查询接口：

```java
@GetMapping("/list")
public Result<PageResult<AttendanceExceptionVO>> list(ExceptionQueryDTO queryDTO) {
    return Result.success(exceptionQueryService.list(queryDTO));
}

@GetMapping("/{id}")
public Result<AttendanceExceptionVO> getById(@PathVariable Long id) {
    return Result.success(exceptionQueryService.getById(id));
}

@GetMapping("/{id}/decision-trace")
public Result<List<DecisionTraceVO>> decisionTrace(@PathVariable Long id) {
    return Result.success(decisionTraceService.list("ATTENDANCE_EXCEPTION", id));
}

@GetMapping("/{id}/analysis-brief")
public Result<ExceptionAnalysisBriefVO> analysisBrief(@PathVariable Long id) {
    return Result.success(exceptionQueryService.getAnalysisBrief(id));
}
```

- [ ] **Step 4: 重跑异常查询与 Bean 测试**

Run: `mvn "-Dtest=ExceptionControllerTest,ModuleSkeletonBeansTest" test`

Expected: `BUILD SUCCESS`

## Task 6: 文档同步与最小验证

**Files:**
- Modify: `docs/module-guides/BE-05-异常检测与智能分析模块开发指南.md`
- Modify: `docs/test/测试用例文档.md`

- [ ] **Step 1: 回写 `BE-05` 模块指南中的 MVP 规则范围与最小验证命令**

把模块指南中的基础规则描述从泛化表述收敛为当前实现范围，并同步最小验证命令。需要落入文档的核心内容如下：

```md
- 本轮 MVP 的基础规则判定范围：`LATE`、`EARLY_LEAVE`、`REPEAT_CHECK`、`ILLEGAL_TIME`。
- `ABSENT` 缺勤自动判定暂不纳入本轮实现，待后续排班/日历/定时任务能力具备后补齐。
- 最小验证命令：`mvn -Dtest=RuleControllerTest,ExceptionControllerTest test`
```

- [ ] **Step 2: 在测试文档中补充 `BE-05` 直接相关的最小验证用例**

向 `docs/test/测试用例文档.md` 补充以下几类条目：

```md
| TC042 | 规则管理 | 查询规则列表 | 已存在启用规则 | 调用 `GET /api/rule/list` | 返回 `total` 和 `records` 分页结构 |
| TC043 | 基础异常 | 迟到识别 | 已存在晚于阈值的上班打卡 | 调用 `POST /api/exception/rule-check` | 生成 `LATE` 异常并写入决策链 |
| TC044 | 复杂异常 | 代打卡识别 | 已存在异常设备/地点打卡记录 | 调用 `POST /api/exception/complex-check` | 返回 `PROXY_CHECKIN`、`HIGH` 和结构化分析结果 |
| TC045 | 复杂异常 | 模型失败降级 | 模型网关抛错 | 调用 `POST /api/exception/complex-check` | 返回 `MODEL_FALLBACK` 并记录失败日志 |
| TC046 | 智能异常分析 | 查询异常决策链 | 已存在异常与决策链记录 | 调用 `GET /api/exception/{id}/decision-trace` | 返回规则结果、模型结果和最终决策 |
```

- [ ] **Step 3: 执行本次 `BE-05` 的最小最终验证**

Run: `mvn "-Dtest=RuleControllerTest,ExceptionControllerTest" test`

Expected: `BUILD SUCCESS`
