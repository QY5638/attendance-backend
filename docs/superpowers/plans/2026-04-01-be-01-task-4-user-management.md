# BE-01 Task 4 用户管理 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有 `BE-01` 范围内补齐用户列表、新增、修改、删除接口，并保持认证、安全、文档与测试一致。

**Architecture:** 以 `UserServiceImpl` 作为用户管理门面，控制器继续保持薄层；新增 `UserValidationSupport` 和 `UserPasswordSupport` 两个轻量支撑组件，把字段校验、用户名唯一、部门/角色存在校验与密码处理从主流程中抽离。用户持久化继续基于 MyBatis-Plus 和现有 `UserMapper`，通过实体注解解决 `user` 表映射和 ID 生成问题。

**Tech Stack:** Spring Boot 2.7、Spring Security、MyBatis-Plus、H2、JUnit 5、MockMvc、BCrypt

> 当前用户要求：本计划执行阶段不包含 `git commit`、`git push`、创建 PR 或其他额外 Git 动作。

---

## 文件结构与职责

- `src/main/java/com/quyong/attendance/module/user/entity/User.java`
  - 为 `user` 表补充 MyBatis-Plus 映射信息，确保 `BaseMapper` 在保留表名和新增场景下工作稳定。
- `src/main/java/com/quyong/attendance/module/user/controller/UserController.java`
  - 暴露 `list/add/update/delete` 四个接口，保持和 `DepartmentController` 一样薄。
- `src/main/java/com/quyong/attendance/module/user/service/UserService.java`
  - 定义用户管理服务契约。
- `src/main/java/com/quyong/attendance/module/user/service/impl/UserServiceImpl.java`
  - 编排列表、新增、修改、删除流程，并完成 `User` 到 `UserVO` 映射。
- `src/main/java/com/quyong/attendance/module/user/support/UserValidationSupport.java`
  - 负责字符串归一化、必填校验、用户名唯一、部门存在、角色存在、用户存在、状态合法性校验。
- `src/main/java/com/quyong/attendance/module/user/support/UserPasswordSupport.java`
  - 负责新增密码校验、BCrypt 编码、修改密码保留或覆盖逻辑。
- `src/test/java/com/quyong/attendance/UserManagementIntegrationTest.java`
  - 覆盖用户管理 CRUD 的端到端集成测试。
- `src/test/java/com/quyong/attendance/AuthSecurityIntegrationTest.java`
  - 更新管理员访问 `/api/user/list` 的预期，避免继续断言旧的 `404` 行为。
- `docs/api/API接口设计文档.md`
  - 补充用户管理接口的请求参数、示例和更新密码语义。
- `docs/test/测试用例文档.md`
  - 补充用户管理的功能用例与接口用例。

### Task 1: 用户列表接口与安全回归

**Files:**
- Create: `src/test/java/com/quyong/attendance/UserManagementIntegrationTest.java`
- Modify: `src/test/java/com/quyong/attendance/AuthSecurityIntegrationTest.java`
- Modify: `src/main/java/com/quyong/attendance/module/user/entity/User.java`
- Modify: `src/main/java/com/quyong/attendance/module/user/controller/UserController.java`
- Modify: `src/main/java/com/quyong/attendance/module/user/service/UserService.java`
- Modify: `src/main/java/com/quyong/attendance/module/user/service/impl/UserServiceImpl.java`

- [ ] **Step 1: 写出用户列表 RED 测试**

在 `src/test/java/com/quyong/attendance/UserManagementIntegrationTest.java` 新建以下完整测试类：

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

        insertUser(9001L, "admin", "系统管理员", 1L, 1L, 1, "123456");
        insertUser(1001L, "zhangsan", "张三", 2L, 2L, 1, "123456");
        insertUser(1002L, "lisi", "李四", 3L, 2L, 0, "123456");
    }

    @Test
    void shouldReturnUserListForAdmin() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/user/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].username").value("zhangsan"))
                .andExpect(jsonPath("$.data[1].username").value("lisi"))
                .andExpect(jsonPath("$.data[2].username").value("admin"));
    }

    @Test
    void shouldFilterUserListByKeyword() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/user/list")
                        .param("keyword", "张")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].username").value("zhangsan"))
                .andExpect(jsonPath("$.data[0].realName").value("张三"));
    }

    @Test
    void shouldFilterUserListByDepartmentAndStatus() throws Exception {
        String token = loginAndExtractToken("admin", "123456");

        mockMvc.perform(get("/api/user/list")
                        .param("deptId", "3")
                        .param("status", "0")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].username").value("lisi"))
                .andExpect(jsonPath("$.data[0].status").value(0));
    }

    private void insertDepartment(Long id, String name, String description) {
        jdbcTemplate.update(
                "INSERT INTO department (id, name, description) VALUES (?, ?, ?)",
                id,
                name,
                description
        );
    }

    private void insertUser(Long id, String username, String realName, Long deptId, Long roleId, int status, String rawPassword) {
        jdbcTemplate.update(
                "INSERT INTO user (id, username, password, realName, gender, phone, deptId, roleId, status, createTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                id,
                username,
                PASSWORD_ENCODER.encode(rawPassword),
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

同时把 `src/test/java/com/quyong/attendance/AuthSecurityIntegrationTest.java` 中管理员访问用户接口的断言改成：

```java
@Test
void shouldNotBeBlockedByUnauthorizedOrForbiddenWhenAdminAccessesProtectedUserApi() throws Exception {
    String token = loginAndExtractToken("admin", "123456");

    mockMvc.perform(get("/api/user/list")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("success"));
}
```

- [ ] **Step 2: 运行 RED 测试，确认当前用户列表接口未实现**

Run: `mvn "-Dtest=UserManagementIntegrationTest,AuthSecurityIntegrationTest" test`

Expected: FAIL，`UserManagementIntegrationTest` 和更新后的安全测试会因为 `/api/user/list` 仍返回 `404` 而失败。

- [ ] **Step 3: 实现最小用户列表能力**

把 `src/main/java/com/quyong/attendance/module/user/entity/User.java` 替换为：

```java
package com.quyong.attendance.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("`user`")
public class User {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String gender;
    private String phone;
    private Long deptId;
    private Long roleId;
    private Integer status;
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
```

把 `src/main/java/com/quyong/attendance/module/user/service/UserService.java` 替换为：

```java
package com.quyong.attendance.module.user.service;

import com.quyong.attendance.module.user.dto.UserQueryDTO;
import com.quyong.attendance.module.user.vo.UserVO;

import java.util.List;

public interface UserService {

    List<UserVO> list(UserQueryDTO queryDTO);
}
```

把 `src/main/java/com/quyong/attendance/module/user/controller/UserController.java` 替换为：

```java
package com.quyong.attendance.module.user.controller;

import com.quyong.attendance.common.api.Result;
import com.quyong.attendance.module.user.dto.UserQueryDTO;
import com.quyong.attendance.module.user.service.UserService;
import com.quyong.attendance.module.user.vo.UserVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/list")
    public Result<List<UserVO>> list(UserQueryDTO queryDTO) {
        return Result.success(userService.list(queryDTO));
    }
}
```

把 `src/main/java/com/quyong/attendance/module/user/service/impl/UserServiceImpl.java` 替换为：

```java
package com.quyong.attendance.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quyong.attendance.module.user.dto.UserQueryDTO;
import com.quyong.attendance.module.user.entity.User;
import com.quyong.attendance.module.user.mapper.UserMapper;
import com.quyong.attendance.module.user.service.UserService;
import com.quyong.attendance.module.user.vo.UserVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public List<UserVO> list(UserQueryDTO queryDTO) {
        String keyword = normalize(queryDTO == null ? null : queryDTO.getKeyword());
        Long deptId = queryDTO == null ? null : queryDTO.getDeptId();
        Integer status = queryDTO == null ? null : queryDTO.getStatus();

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>();
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper.like(User::getUsername, keyword)
                    .or()
                    .like(User::getRealName, keyword));
        }
        if (deptId != null) {
            queryWrapper.eq(User::getDeptId, deptId);
        }
        if (status != null) {
            queryWrapper.eq(User::getStatus, status);
        }
        queryWrapper.orderByAsc(User::getId);

        return userMapper.selectList(queryWrapper)
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setGender(user.getGender());
        vo.setPhone(user.getPhone());
        vo.setDeptId(user.getDeptId());
        vo.setRoleId(user.getRoleId());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
```

- [ ] **Step 4: 运行列表与安全回归测试，确认 GREEN**

Run: `mvn "-Dtest=UserManagementIntegrationTest,AuthSecurityIntegrationTest" test`

Expected: PASS，用户列表测试通过，`AuthSecurityIntegrationTest` 中管理员访问 `/api/user/list` 变为 `200`。

### Task 2: 新增用户与支撑组件

**Files:**
- Modify: `src/test/java/com/quyong/attendance/UserManagementIntegrationTest.java`
- Create: `src/main/java/com/quyong/attendance/module/user/support/UserValidationSupport.java`
- Create: `src/main/java/com/quyong/attendance/module/user/support/UserPasswordSupport.java`
- Modify: `src/main/java/com/quyong/attendance/module/user/controller/UserController.java`
- Modify: `src/main/java/com/quyong/attendance/module/user/service/UserService.java`
- Modify: `src/main/java/com/quyong/attendance/module/user/service/impl/UserServiceImpl.java`

- [ ] **Step 1: 在集成测试中追加新增用户 RED 用例**

把以下测试方法追加到 `src/test/java/com/quyong/attendance/UserManagementIntegrationTest.java` 中：

```java
@Test
void shouldAddUserWhenInputIsValid() throws Exception {
    String token = loginAndExtractToken("admin", "123456");

    mockMvc.perform(post("/api/user/add")
                    .header("Authorization", "Bearer " + token)
                    .contentType(APPLICATION_JSON)
                    .content("{\"username\":\"wangwu\",\"password\":\"123456\",\"realName\":\"王五\",\"gender\":\"男\",\"phone\":\"13800000001\",\"deptId\":1,\"roleId\":2}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.username").value("wangwu"))
            .andExpect(jsonPath("$.data.realName").value("王五"))
            .andExpect(jsonPath("$.data.status").value(1));

    Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user WHERE username = ?",
            Integer.class,
            "wangwu"
    );
    assertEquals(1, count);

    String encodedPassword = jdbcTemplate.queryForObject(
            "SELECT password FROM user WHERE username = ?",
            String.class,
            "wangwu"
    );
    assertNotEquals("123456", encodedPassword);
    assertTrue(PASSWORD_ENCODER.matches("123456", encodedPassword));
}

@Test
void shouldFailAddUserWhenUsernameIsBlank() throws Exception {
    String token = loginAndExtractToken("admin", "123456");

    mockMvc.perform(post("/api/user/add")
                    .header("Authorization", "Bearer " + token)
                    .contentType(APPLICATION_JSON)
                    .content("{\"username\":\"   \",\"password\":\"123456\",\"realName\":\"王五\",\"deptId\":1,\"roleId\":2}"))
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
                    .content("{\"username\":\"wangwu\",\"password\":\"   \",\"realName\":\"王五\",\"deptId\":1,\"roleId\":2}"))
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
                    .content("{\"username\":\"wangwu\",\"password\":\"123456\",\"realName\":\"   \",\"deptId\":1,\"roleId\":2}"))
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
                    .content("{\"username\":\" zhangsan \",\"password\":\"123456\",\"realName\":\"王五\",\"deptId\":1,\"roleId\":2}"))
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
                    .content("{\"username\":\"wangwu\",\"password\":\"123456\",\"realName\":\"王五\",\"deptId\":99,\"roleId\":2}"))
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
                    .content("{\"username\":\"wangwu\",\"password\":\"123456\",\"realName\":\"王五\",\"deptId\":1,\"roleId\":99}"))
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
                    .content("{\"username\":\"wangwu\",\"password\":\"123456\",\"realName\":\"王五\",\"deptId\":1,\"roleId\":2,\"status\":2}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("用户状态不合法"));
}
```

- [ ] **Step 2: 运行 RED 测试，确认新增接口尚未实现**

Run: `mvn "-Dtest=UserManagementIntegrationTest" test`

Expected: FAIL，新增相关用例会因为 `/api/user/add` 仍返回 `404` 而失败。

- [ ] **Step 3: 创建校验与密码支撑组件，并实现新增接口**

新建 `src/main/java/com/quyong/attendance/module/user/support/UserValidationSupport.java`：

```java
package com.quyong.attendance.module.user.support;

import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.department.mapper.DepartmentMapper;
import com.quyong.attendance.module.role.entity.Role;
import com.quyong.attendance.module.role.mapper.RoleMapper;
import com.quyong.attendance.module.user.entity.User;
import com.quyong.attendance.module.user.mapper.UserMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class UserValidationSupport {

    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;
    private final RoleMapper roleMapper;

    public UserValidationSupport(UserMapper userMapper,
                                 DepartmentMapper departmentMapper,
                                 RoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.departmentMapper = departmentMapper;
        this.roleMapper = roleMapper;
    }

    public String requireUsername(String username) {
        String normalized = normalize(username);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户名不能为空");
        }
        return normalized;
    }

    public String requireRealName(String realName) {
        String normalized = normalize(realName);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "姓名不能为空");
        }
        return normalized;
    }

    public String normalizePhone(String phone) {
        return normalize(phone);
    }

    public Integer resolveStatusForAdd(Integer status) {
        return status == null ? 1 : requireValidStatus(status);
    }

    public Integer requireValidStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户状态不合法");
        }
        return status;
    }

    public void ensureUsernameUnique(String username, Long currentId) {
        User existing = userMapper.selectByUsername(username);
        if (existing != null && (currentId == null || !existing.getId().equals(currentId))) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户名已存在");
        }
    }

    public void ensureDepartmentExists(Long deptId) {
        if (deptId == null || departmentMapper.selectById(deptId) == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "部门不存在");
        }
    }

    public void ensureRoleExists(Long roleId) {
        Role role = roleId == null ? null : roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "角色不存在");
        }
    }

    public User requireExistingUser(Long id) {
        User user = id == null ? null : userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户不存在");
        }
        return user;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
```

新建 `src/main/java/com/quyong/attendance/module/user/support/UserPasswordSupport.java`：

```java
package com.quyong.attendance.module.user.support;

import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class UserPasswordSupport {

    private final PasswordEncoder passwordEncoder;

    public UserPasswordSupport(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String encodeForCreate(String rawPassword) {
        String normalized = normalize(rawPassword);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "密码不能为空");
        }
        return passwordEncoder.encode(normalized);
    }

    public String resolvePasswordForUpdate(String rawPassword, String currentPassword) {
        String normalized = normalize(rawPassword);
        if (!StringUtils.hasText(normalized)) {
            return currentPassword;
        }
        return passwordEncoder.encode(normalized);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
```

把 `src/main/java/com/quyong/attendance/module/user/service/UserService.java` 替换为：

```java
package com.quyong.attendance.module.user.service;

import com.quyong.attendance.module.user.dto.UserQueryDTO;
import com.quyong.attendance.module.user.dto.UserSaveDTO;
import com.quyong.attendance.module.user.vo.UserVO;

import java.util.List;

public interface UserService {

    List<UserVO> list(UserQueryDTO queryDTO);

    UserVO add(UserSaveDTO saveDTO);
}
```

把 `src/main/java/com/quyong/attendance/module/user/controller/UserController.java` 替换为：

```java
package com.quyong.attendance.module.user.controller;

import com.quyong.attendance.common.api.Result;
import com.quyong.attendance.module.user.dto.UserQueryDTO;
import com.quyong.attendance.module.user.dto.UserSaveDTO;
import com.quyong.attendance.module.user.service.UserService;
import com.quyong.attendance.module.user.vo.UserVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/list")
    public Result<List<UserVO>> list(UserQueryDTO queryDTO) {
        return Result.success(userService.list(queryDTO));
    }

    @PostMapping("/add")
    public Result<UserVO> add(@RequestBody UserSaveDTO saveDTO) {
        return Result.success(userService.add(saveDTO));
    }
}
```

把 `src/main/java/com/quyong/attendance/module/user/service/impl/UserServiceImpl.java` 替换为：

```java
package com.quyong.attendance.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quyong.attendance.module.user.dto.UserQueryDTO;
import com.quyong.attendance.module.user.dto.UserSaveDTO;
import com.quyong.attendance.module.user.entity.User;
import com.quyong.attendance.module.user.mapper.UserMapper;
import com.quyong.attendance.module.user.service.UserService;
import com.quyong.attendance.module.user.support.UserPasswordSupport;
import com.quyong.attendance.module.user.support.UserValidationSupport;
import com.quyong.attendance.module.user.vo.UserVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserValidationSupport userValidationSupport;
    private final UserPasswordSupport userPasswordSupport;

    public UserServiceImpl(UserMapper userMapper,
                           UserValidationSupport userValidationSupport,
                           UserPasswordSupport userPasswordSupport) {
        this.userMapper = userMapper;
        this.userValidationSupport = userValidationSupport;
        this.userPasswordSupport = userPasswordSupport;
    }

    @Override
    public List<UserVO> list(UserQueryDTO queryDTO) {
        String keyword = normalize(queryDTO == null ? null : queryDTO.getKeyword());
        Long deptId = queryDTO == null ? null : queryDTO.getDeptId();
        Integer status = queryDTO == null ? null : queryDTO.getStatus();

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>();
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper.like(User::getUsername, keyword)
                    .or()
                    .like(User::getRealName, keyword));
        }
        if (deptId != null) {
            queryWrapper.eq(User::getDeptId, deptId);
        }
        if (status != null) {
            queryWrapper.eq(User::getStatus, status);
        }
        queryWrapper.orderByAsc(User::getId);

        return userMapper.selectList(queryWrapper)
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public UserVO add(UserSaveDTO saveDTO) {
        String username = userValidationSupport.requireUsername(saveDTO == null ? null : saveDTO.getUsername());
        String realName = userValidationSupport.requireRealName(saveDTO == null ? null : saveDTO.getRealName());
        Long deptId = saveDTO == null ? null : saveDTO.getDeptId();
        Long roleId = saveDTO == null ? null : saveDTO.getRoleId();
        Integer status = saveDTO == null ? null : saveDTO.getStatus();

        userValidationSupport.ensureUsernameUnique(username, null);
        userValidationSupport.ensureDepartmentExists(deptId);
        userValidationSupport.ensureRoleExists(roleId);

        User user = new User();
        user.setUsername(username);
        user.setPassword(userPasswordSupport.encodeForCreate(saveDTO == null ? null : saveDTO.getPassword()));
        user.setRealName(realName);
        user.setGender(saveDTO == null ? null : saveDTO.getGender());
        user.setPhone(userValidationSupport.normalizePhone(saveDTO == null ? null : saveDTO.getPhone()));
        user.setDeptId(deptId);
        user.setRoleId(roleId);
        user.setStatus(userValidationSupport.resolveStatusForAdd(status));
        userMapper.insert(user);

        return toVO(userMapper.selectById(user.getId()));
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setGender(user.getGender());
        vo.setPhone(user.getPhone());
        vo.setDeptId(user.getDeptId());
        vo.setRoleId(user.getRoleId());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
```

- [ ] **Step 4: 运行新增测试，确认 GREEN**

Run: `mvn "-Dtest=UserManagementIntegrationTest" test`

Expected: PASS，新增成功、字段校验、用户名唯一、部门/角色存在校验和密码加密断言全部通过。

### Task 3: 修改用户、删除用户与密码更新回归

**Files:**
- Modify: `src/test/java/com/quyong/attendance/UserManagementIntegrationTest.java`
- Modify: `src/main/java/com/quyong/attendance/module/user/controller/UserController.java`
- Modify: `src/main/java/com/quyong/attendance/module/user/service/UserService.java`
- Modify: `src/main/java/com/quyong/attendance/module/user/service/impl/UserServiceImpl.java`

- [ ] **Step 1: 在集成测试中追加修改与删除 RED 用例**

把以下测试方法追加到 `src/test/java/com/quyong/attendance/UserManagementIntegrationTest.java` 中：

```java
@Test
void shouldUpdateUserWhenInputIsValid() throws Exception {
    String token = loginAndExtractToken("admin", "123456");

    mockMvc.perform(put("/api/user/update")
                    .header("Authorization", "Bearer " + token)
                    .contentType(APPLICATION_JSON)
                    .content("{\"id\":1001,\"username\":\"zhangsan\",\"realName\":\"张三三\",\"gender\":\"男\",\"phone\":\"13900000000\",\"deptId\":1,\"roleId\":2,\"status\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.realName").value("张三三"))
            .andExpect(jsonPath("$.data.deptId").value(1));

    String realName = jdbcTemplate.queryForObject(
            "SELECT realName FROM user WHERE id = ?",
            String.class,
            1001L
    );
    assertEquals("张三三", realName);
}

@Test
void shouldKeepPasswordWhenPasswordIsBlank() throws Exception {
    String token = loginAndExtractToken("admin", "123456");
    String oldPassword = jdbcTemplate.queryForObject(
            "SELECT password FROM user WHERE id = ?",
            String.class,
            1001L
    );

    mockMvc.perform(put("/api/user/update")
                    .header("Authorization", "Bearer " + token)
                    .contentType(APPLICATION_JSON)
                    .content("{\"id\":1001,\"username\":\"zhangsan\",\"password\":\"   \",\"realName\":\"张三\",\"gender\":\"男\",\"phone\":\"13800000000\",\"deptId\":2,\"roleId\":2,\"status\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

    String newPassword = jdbcTemplate.queryForObject(
            "SELECT password FROM user WHERE id = ?",
            String.class,
            1001L
    );
    assertEquals(oldPassword, newPassword);
}

@Test
void shouldResetPasswordWhenPasswordProvided() throws Exception {
    String token = loginAndExtractToken("admin", "123456");

    mockMvc.perform(put("/api/user/update")
                    .header("Authorization", "Bearer " + token)
                    .contentType(APPLICATION_JSON)
                    .content("{\"id\":1001,\"username\":\"zhangsan\",\"password\":\"654321\",\"realName\":\"张三\",\"gender\":\"男\",\"phone\":\"13800000000\",\"deptId\":2,\"roleId\":2,\"status\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

    mockMvc.perform(post("/api/auth/login")
                    .contentType(APPLICATION_JSON)
                    .content("{\"username\":\"zhangsan\",\"password\":\"654321\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
}

@Test
void shouldFailUpdateUserWhenUserDoesNotExist() throws Exception {
    String token = loginAndExtractToken("admin", "123456");

    mockMvc.perform(put("/api/user/update")
                    .header("Authorization", "Bearer " + token)
                    .contentType(APPLICATION_JSON)
                    .content("{\"id\":99,\"username\":\"missing\",\"realName\":\"不存在\",\"deptId\":1,\"roleId\":2,\"status\":1}"))
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
                    .content("{\"id\":1001,\"username\":\" admin \",\"realName\":\"张三\",\"deptId\":2,\"roleId\":2,\"status\":1}"))
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
                    .content("{\"id\":1001,\"username\":\"zhangsan\",\"realName\":\"张三\",\"deptId\":99,\"roleId\":2,\"status\":1}"))
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
                    .content("{\"id\":1001,\"username\":\"zhangsan\",\"realName\":\"张三\",\"deptId\":2,\"roleId\":99,\"status\":1}"))
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
                    .content("{\"id\":1001,\"username\":\"zhangsan\",\"realName\":\"张三\",\"deptId\":2,\"roleId\":2,\"status\":2}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("用户状态不合法"));
}

@Test
void shouldDeleteUserWhenUserExists() throws Exception {
    String token = loginAndExtractToken("admin", "123456");

    mockMvc.perform(delete("/api/user/1002")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("success"));

    Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user WHERE id = ?",
            Integer.class,
            1002L
    );
    assertEquals(0, count);
}

@Test
void shouldFailDeleteUserWhenUserDoesNotExist() throws Exception {
    String token = loginAndExtractToken("admin", "123456");

    mockMvc.perform(delete("/api/user/99")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("用户不存在"));
}
```

- [ ] **Step 2: 运行 RED 测试，确认修改与删除接口尚未实现**

Run: `mvn "-Dtest=UserManagementIntegrationTest" test`

Expected: FAIL，更新和删除相关用例会因为 `/api/user/update`、`/api/user/{id}` 尚未实现而失败。

- [ ] **Step 3: 实现修改、删除与密码更新逻辑**

把 `src/main/java/com/quyong/attendance/module/user/service/UserService.java` 替换为：

```java
package com.quyong.attendance.module.user.service;

import com.quyong.attendance.module.user.dto.UserQueryDTO;
import com.quyong.attendance.module.user.dto.UserSaveDTO;
import com.quyong.attendance.module.user.vo.UserVO;

import java.util.List;

public interface UserService {

    List<UserVO> list(UserQueryDTO queryDTO);

    UserVO add(UserSaveDTO saveDTO);

    UserVO update(UserSaveDTO saveDTO);

    void delete(Long id);
}
```

把 `src/main/java/com/quyong/attendance/module/user/controller/UserController.java` 替换为：

```java
package com.quyong.attendance.module.user.controller;

import com.quyong.attendance.common.api.Result;
import com.quyong.attendance.module.user.dto.UserQueryDTO;
import com.quyong.attendance.module.user.dto.UserSaveDTO;
import com.quyong.attendance.module.user.service.UserService;
import com.quyong.attendance.module.user.vo.UserVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/list")
    public Result<List<UserVO>> list(UserQueryDTO queryDTO) {
        return Result.success(userService.list(queryDTO));
    }

    @PostMapping("/add")
    public Result<UserVO> add(@RequestBody UserSaveDTO saveDTO) {
        return Result.success(userService.add(saveDTO));
    }

    @PutMapping("/update")
    public Result<UserVO> update(@RequestBody UserSaveDTO saveDTO) {
        return Result.success(userService.update(saveDTO));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success(null);
    }
}
```

把 `src/main/java/com/quyong/attendance/module/user/service/impl/UserServiceImpl.java` 替换为：

```java
package com.quyong.attendance.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quyong.attendance.module.user.dto.UserQueryDTO;
import com.quyong.attendance.module.user.dto.UserSaveDTO;
import com.quyong.attendance.module.user.entity.User;
import com.quyong.attendance.module.user.mapper.UserMapper;
import com.quyong.attendance.module.user.service.UserService;
import com.quyong.attendance.module.user.support.UserPasswordSupport;
import com.quyong.attendance.module.user.support.UserValidationSupport;
import com.quyong.attendance.module.user.vo.UserVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserValidationSupport userValidationSupport;
    private final UserPasswordSupport userPasswordSupport;

    public UserServiceImpl(UserMapper userMapper,
                           UserValidationSupport userValidationSupport,
                           UserPasswordSupport userPasswordSupport) {
        this.userMapper = userMapper;
        this.userValidationSupport = userValidationSupport;
        this.userPasswordSupport = userPasswordSupport;
    }

    @Override
    public List<UserVO> list(UserQueryDTO queryDTO) {
        String keyword = normalize(queryDTO == null ? null : queryDTO.getKeyword());
        Long deptId = queryDTO == null ? null : queryDTO.getDeptId();
        Integer status = queryDTO == null ? null : queryDTO.getStatus();

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>();
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper.like(User::getUsername, keyword)
                    .or()
                    .like(User::getRealName, keyword));
        }
        if (deptId != null) {
            queryWrapper.eq(User::getDeptId, deptId);
        }
        if (status != null) {
            queryWrapper.eq(User::getStatus, status);
        }
        queryWrapper.orderByAsc(User::getId);

        return userMapper.selectList(queryWrapper)
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public UserVO add(UserSaveDTO saveDTO) {
        String username = userValidationSupport.requireUsername(saveDTO == null ? null : saveDTO.getUsername());
        String realName = userValidationSupport.requireRealName(saveDTO == null ? null : saveDTO.getRealName());
        Long deptId = saveDTO == null ? null : saveDTO.getDeptId();
        Long roleId = saveDTO == null ? null : saveDTO.getRoleId();
        Integer status = saveDTO == null ? null : saveDTO.getStatus();

        userValidationSupport.ensureUsernameUnique(username, null);
        userValidationSupport.ensureDepartmentExists(deptId);
        userValidationSupport.ensureRoleExists(roleId);

        User user = new User();
        user.setUsername(username);
        user.setPassword(userPasswordSupport.encodeForCreate(saveDTO == null ? null : saveDTO.getPassword()));
        user.setRealName(realName);
        user.setGender(saveDTO == null ? null : saveDTO.getGender());
        user.setPhone(userValidationSupport.normalizePhone(saveDTO == null ? null : saveDTO.getPhone()));
        user.setDeptId(deptId);
        user.setRoleId(roleId);
        user.setStatus(userValidationSupport.resolveStatusForAdd(status));
        userMapper.insert(user);

        return toVO(userMapper.selectById(user.getId()));
    }

    @Override
    public UserVO update(UserSaveDTO saveDTO) {
        User existingUser = userValidationSupport.requireExistingUser(saveDTO == null ? null : saveDTO.getId());
        String username = userValidationSupport.requireUsername(saveDTO == null ? null : saveDTO.getUsername());
        String realName = userValidationSupport.requireRealName(saveDTO == null ? null : saveDTO.getRealName());
        Long deptId = saveDTO == null ? null : saveDTO.getDeptId();
        Long roleId = saveDTO == null ? null : saveDTO.getRoleId();
        Integer status = saveDTO == null ? null : saveDTO.getStatus();
        userValidationSupport.ensureUsernameUnique(username, existingUser.getId());
        userValidationSupport.ensureDepartmentExists(deptId);
        userValidationSupport.ensureRoleExists(roleId);

        existingUser.setUsername(username);
        existingUser.setPassword(userPasswordSupport.resolvePasswordForUpdate(saveDTO == null ? null : saveDTO.getPassword(), existingUser.getPassword()));
        existingUser.setRealName(realName);
        existingUser.setGender(saveDTO == null ? null : saveDTO.getGender());
        existingUser.setPhone(userValidationSupport.normalizePhone(saveDTO == null ? null : saveDTO.getPhone()));
        existingUser.setDeptId(deptId);
        existingUser.setRoleId(roleId);
        existingUser.setStatus(userValidationSupport.requireValidStatus(status));
        userMapper.updateById(existingUser);

        return toVO(userMapper.selectById(existingUser.getId()));
    }

    @Override
    public void delete(Long id) {
        User user = userValidationSupport.requireExistingUser(id);
        userMapper.deleteById(user.getId());
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setGender(user.getGender());
        vo.setPhone(user.getPhone());
        vo.setDeptId(user.getDeptId());
        vo.setRoleId(user.getRoleId());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
```

- [ ] **Step 4: 运行用户管理与鉴权回归，确认 GREEN**

Run: `mvn "-Dtest=UserManagementIntegrationTest,AuthSecurityIntegrationTest" test`

Expected: PASS，修改、删除、密码更新验证以及既有 `401/403` 鉴权语义全部通过。

### Task 4: 文档同步与全量验证

**Files:**
- Modify: `docs/api/API接口设计文档.md`
- Modify: `docs/test/测试用例文档.md`

- [ ] **Step 1: 同步 API 接口文档**

把 `docs/api/API接口设计文档.md` 中 `3.1` 到 `3.4` 用户接口小节替换为：

````md
### 3.1 查询员工列表
- 路径：`GET /api/user/list`
- 查询参数：
  - `keyword`：可选，按账号或姓名模糊查询
  - `deptId`：可选，按部门筛选
  - `status`：可选，按状态筛选，`1` 为启用，`0` 为禁用

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1001,
      "username": "zhangsan",
      "realName": "张三",
      "gender": "男",
      "phone": "13800000000",
      "deptId": 2,
      "roleId": 2,
      "status": 1,
      "createTime": "2026-04-01T09:00:00"
    }
  ]
}
```

### 3.2 新增员工
- 路径：`POST /api/user/add`

```json
{
  "username": "wangwu",
  "password": "123456",
  "realName": "王五",
  "gender": "男",
  "phone": "13800000001",
  "deptId": 1,
  "roleId": 2,
  "status": 1
}
```

### 3.3 修改员工
- 路径：`PUT /api/user/update`
- 说明：`password` 为可选字段；未传或为空白时保留原密码，传入有效值时按新密码更新。

```json
{
  "id": 1001,
  "username": "zhangsan",
  "password": "654321",
  "realName": "张三",
  "gender": "男",
  "phone": "13800000000",
  "deptId": 2,
  "roleId": 2,
  "status": 1
}
```

### 3.4 删除员工
- 路径：`DELETE /api/user/{id}`
````

- [ ] **Step 2: 同步测试用例文档**

把以下内容追加到 `docs/test/测试用例文档.md` 对应表格中：

```md
| TC026 | 用户管理 | 查询员工列表成功 | 管理员已登录，存在员工数据 | 访问员工列表接口 | 返回员工列表数据 |
| TC027 | 用户管理 | 新增员工失败（用户名重复） | 管理员已登录，目标账号已存在 | 提交重复用户名 | 提示用户名已存在 |
| TC028 | 用户管理 | 修改员工成功 | 管理员已登录，目标员工存在 | 提交合法修改信息 | 员工信息更新成功 |
| TC029 | 用户管理 | 修改密码后登录成功 | 管理员已登录，目标员工存在 | 更新员工密码后使用新密码登录 | 登录成功 |
| TC030 | 用户管理 | 删除员工成功 | 管理员已登录，目标员工存在 | 删除目标员工 | 删除成功 |
```

```md
| API012 | `GET /api/user/list` | 查询员工列表 | 可选关键字、部门、状态参数 | 返回员工列表 |
| API013 | `POST /api/user/add` | 新增员工 | 合法员工参数 | 返回新增员工信息 |
| API014 | `PUT /api/user/update` | 修改员工 | 合法员工参数 | 返回更新后的员工信息 |
| API015 | `DELETE /api/user/{id}` | 删除员工 | 合法员工ID | 返回删除成功 |
```

- [ ] **Step 3: 运行用户与部门相关回归**

Run: `mvn "-Dtest=UserManagementIntegrationTest,AuthSecurityIntegrationTest,DepartmentManagementIntegrationTest" test`

Expected: PASS，用户管理新增能力没有破坏部门管理和既有鉴权行为。

- [ ] **Step 4: 运行全量测试，确认没有破坏 BE-01 既有结果**

Run: `mvn test`

Expected: `BUILD SUCCESS`，并且总测试结果无 `Failures`、无 `Errors`。

- [ ] **Step 5: 记录交付说明**

交付时明确说明以下结论：

```text
Task 4 已完成用户列表、新增、修改、删除接口，并补齐用户名唯一、部门/角色存在校验与密码更新规则；验证命令已执行，未引入数据库结构变更，也未执行额外 Git 动作。
```

## Plan Self-Review

### 1. Spec coverage

- 用户列表：Task 1 已覆盖查询、关键字、部门、状态筛选。
- 用户新增：Task 2 已覆盖必填校验、用户名唯一、部门/角色存在、状态默认值与密码加密。
- 用户修改：Task 3 已覆盖存在性、唯一性、状态校验、密码保留与密码更新。
- 用户删除：Task 3 已覆盖删除成功与删除不存在用户。
- 安全语义：Task 1 与 Task 3 均回归 `AuthSecurityIntegrationTest`。
- 文档同步与全量验证：Task 4 已覆盖。

### 2. Placeholder scan

- 计划中未保留 `TODO`、`TBD`、`implement later`、`similar to` 等占位描述。
- 所有代码改动步骤均给出了可直接落地的文件内容或测试方法。

### 3. Type consistency

- `UserValidationSupport`、`UserPasswordSupport`、`UserService`、`UserServiceImpl` 的方法名和返回类型在各任务中保持一致。
- 用户接口路径始终保持为 `/api/user/list`、`/api/user/add`、`/api/user/update`、`/api/user/{id}`。
- 业务错误消息与设计文档一致，没有出现同义但不同文案的冲突。
