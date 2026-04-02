# BE-01 Task 3 部门管理 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有 `BE-01` 范围内补齐部门列表、新增、修改、删除接口，并实现“部门被用户引用时禁止删除”的业务校验。

**Architecture:** 沿用现有 `Spring Boot + Spring Security + MyBatis-Plus + MockMvc` 结构，只在 `department` 模块和 `UserMapper` 上做最小必要扩展。实现顺序按 TDD 推进：先补集成测试，再补控制器与服务层最小实现，最后同步 API 与测试文档并做全量回归。

**Tech Stack:** Spring Boot 2.7、Spring Security、MyBatis-Plus、JUnit 5、MockMvc、H2、JdbcTemplate

> 说明：根据当前协作约束，本计划不包含主动 `git commit` 步骤；只有用户后续明确要求时才执行提交。

---

### Task 1: 列表与新增接口的 RED-GREEN

**Files:**
- Create: `src/test/java/com/quyong/attendance/DepartmentManagementIntegrationTest.java`
- Modify: `src/main/java/com/quyong/attendance/module/department/controller/DepartmentController.java`
- Modify: `src/main/java/com/quyong/attendance/module/department/service/DepartmentService.java`
- Modify: `src/main/java/com/quyong/attendance/module/department/service/impl/DepartmentServiceImpl.java`

- [ ] **Step 1: 先写列表与新增的失败测试**

在 `src/test/java/com/quyong/attendance/DepartmentManagementIntegrationTest.java` 创建以下初始内容：

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
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
```

- [ ] **Step 2: 运行测试，确认先红灯**

Run: `mvn "-Dtest=DepartmentManagementIntegrationTest#shouldReturnDepartmentListForAdmin+shouldFilterDepartmentListByKeyword+shouldAddDepartmentWhenNameIsUnique+shouldFailAddDepartmentWhenNameIsBlankAfterTrim+shouldFailAddDepartmentWhenNameAlreadyExistsAfterTrim" test`

Expected: FAIL，至少出现新增或列表接口 `Status expected:<200> but was:<404>`，证明部门接口尚未实现。

- [ ] **Step 3: 写出让列表与新增通过的最小实现**

将 `src/main/java/com/quyong/attendance/module/department/service/DepartmentService.java` 改为：

```java
package com.quyong.attendance.module.department.service;

import com.quyong.attendance.module.department.dto.DepartmentQueryDTO;
import com.quyong.attendance.module.department.dto.DepartmentSaveDTO;
import com.quyong.attendance.module.department.vo.DepartmentVO;

import java.util.List;

public interface DepartmentService {

    List<DepartmentVO> list(DepartmentQueryDTO queryDTO);

    DepartmentVO add(DepartmentSaveDTO saveDTO);
}
```

将 `src/main/java/com/quyong/attendance/module/department/controller/DepartmentController.java` 改为：

```java
package com.quyong.attendance.module.department.controller;

import com.quyong.attendance.common.api.Result;
import com.quyong.attendance.module.department.dto.DepartmentQueryDTO;
import com.quyong.attendance.module.department.dto.DepartmentSaveDTO;
import com.quyong.attendance.module.department.service.DepartmentService;
import com.quyong.attendance.module.department.vo.DepartmentVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/department")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping("/list")
    public Result<List<DepartmentVO>> list(DepartmentQueryDTO queryDTO) {
        return Result.success(departmentService.list(queryDTO));
    }

    @PostMapping("/add")
    public Result<DepartmentVO> add(@RequestBody DepartmentSaveDTO saveDTO) {
        return Result.success(departmentService.add(saveDTO));
    }
}
```

将 `src/main/java/com/quyong/attendance/module/department/service/impl/DepartmentServiceImpl.java` 改为：

```java
package com.quyong.attendance.module.department.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.department.dto.DepartmentQueryDTO;
import com.quyong.attendance.module.department.dto.DepartmentSaveDTO;
import com.quyong.attendance.module.department.entity.Department;
import com.quyong.attendance.module.department.mapper.DepartmentMapper;
import com.quyong.attendance.module.department.service.DepartmentService;
import com.quyong.attendance.module.department.vo.DepartmentVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentMapper departmentMapper;

    public DepartmentServiceImpl(DepartmentMapper departmentMapper) {
        this.departmentMapper = departmentMapper;
    }

    @Override
    public List<DepartmentVO> list(DepartmentQueryDTO queryDTO) {
        String keyword = queryDTO == null ? null : normalize(queryDTO.getKeyword());

        LambdaQueryWrapper<Department> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            queryWrapper.like(Department::getName, keyword);
        }
        queryWrapper.orderByAsc(Department::getId);

        return departmentMapper.selectList(queryWrapper)
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public DepartmentVO add(DepartmentSaveDTO saveDTO) {
        String name = normalize(saveDTO == null ? null : saveDTO.getName());
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "部门名称不能为空");
        }

        LambdaQueryWrapper<Department> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Department::getName, name);
        if (departmentMapper.selectCount(queryWrapper) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "部门名称已存在");
        }

        Department department = new Department();
        department.setName(name);
        department.setDescription(saveDTO == null ? null : saveDTO.getDescription());
        departmentMapper.insert(department);

        return toVO(department);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private DepartmentVO toVO(Department department) {
        DepartmentVO vo = new DepartmentVO();
        vo.setId(department.getId());
        vo.setName(department.getName());
        vo.setDescription(department.getDescription());
        return vo;
    }
}
```

- [ ] **Step 4: 再跑一次列表与新增测试，确认转绿**

Run: `mvn "-Dtest=DepartmentManagementIntegrationTest#shouldReturnDepartmentListForAdmin+shouldFilterDepartmentListByKeyword+shouldAddDepartmentWhenNameIsUnique+shouldFailAddDepartmentWhenNameIsBlankAfterTrim+shouldFailAddDepartmentWhenNameAlreadyExistsAfterTrim" test`

Expected: PASS，5 个测试全部通过。

### Task 2: 修改与删除接口的 RED-GREEN

**Files:**
- Modify: `src/test/java/com/quyong/attendance/DepartmentManagementIntegrationTest.java`
- Modify: `src/main/java/com/quyong/attendance/module/department/controller/DepartmentController.java`
- Modify: `src/main/java/com/quyong/attendance/module/department/service/DepartmentService.java`
- Modify: `src/main/java/com/quyong/attendance/module/department/service/impl/DepartmentServiceImpl.java`
- Modify: `src/main/java/com/quyong/attendance/module/user/mapper/UserMapper.java`

- [ ] **Step 1: 追加修改与删除的失败测试**

在 `src/test/java/com/quyong/attendance/DepartmentManagementIntegrationTest.java` 中新增以下 import：

```java
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
```

再追加以下测试方法：

```java
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
```

- [ ] **Step 2: 运行测试，确认修改与删除先红灯**

Run: `mvn "-Dtest=DepartmentManagementIntegrationTest#shouldUpdateDepartmentWhenInputIsValid+shouldFailUpdateDepartmentWhenDepartmentDoesNotExist+shouldFailUpdateDepartmentWhenNameIsBlankAfterTrim+shouldFailUpdateDepartmentWhenNameAlreadyExistsAfterTrim+shouldDeleteDepartmentWhenNoUserReferencesIt+shouldFailDeleteDepartmentWhenDepartmentDoesNotExist+shouldFailDeleteDepartmentWhenDepartmentIsReferenced" test`

Expected: FAIL，至少出现更新或删除接口 `Status expected:<200> but was:<404>`，证明这两个接口仍未实现。

- [ ] **Step 3: 写出让修改与删除通过的最小实现**

将 `src/main/java/com/quyong/attendance/module/department/service/DepartmentService.java` 改为：

```java
package com.quyong.attendance.module.department.service;

import com.quyong.attendance.module.department.dto.DepartmentQueryDTO;
import com.quyong.attendance.module.department.dto.DepartmentSaveDTO;
import com.quyong.attendance.module.department.vo.DepartmentVO;

import java.util.List;

public interface DepartmentService {

    List<DepartmentVO> list(DepartmentQueryDTO queryDTO);

    DepartmentVO add(DepartmentSaveDTO saveDTO);

    DepartmentVO update(DepartmentSaveDTO saveDTO);

    void delete(Long id);
}
```

将 `src/main/java/com/quyong/attendance/module/user/mapper/UserMapper.java` 改为：

```java
package com.quyong.attendance.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quyong.attendance.module.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT id, username, password, realName, gender, phone, deptId, roleId, status, createTime FROM `user` WHERE username = #{username} LIMIT 1")
    User selectByUsername(@Param("username") String username);

    @Select("SELECT COUNT(*) FROM `user` WHERE deptId = #{deptId}")
    long countByDeptId(@Param("deptId") Long deptId);
}
```

将 `src/main/java/com/quyong/attendance/module/department/controller/DepartmentController.java` 改为：

```java
package com.quyong.attendance.module.department.controller;

import com.quyong.attendance.common.api.Result;
import com.quyong.attendance.module.department.dto.DepartmentQueryDTO;
import com.quyong.attendance.module.department.dto.DepartmentSaveDTO;
import com.quyong.attendance.module.department.service.DepartmentService;
import com.quyong.attendance.module.department.vo.DepartmentVO;
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
@RequestMapping("/api/department")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping("/list")
    public Result<List<DepartmentVO>> list(DepartmentQueryDTO queryDTO) {
        return Result.success(departmentService.list(queryDTO));
    }

    @PostMapping("/add")
    public Result<DepartmentVO> add(@RequestBody DepartmentSaveDTO saveDTO) {
        return Result.success(departmentService.add(saveDTO));
    }

    @PutMapping("/update")
    public Result<DepartmentVO> update(@RequestBody DepartmentSaveDTO saveDTO) {
        return Result.success(departmentService.update(saveDTO));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return Result.success(null);
    }
}
```

将 `src/main/java/com/quyong/attendance/module/department/service/impl/DepartmentServiceImpl.java` 改为：

```java
package com.quyong.attendance.module.department.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.department.dto.DepartmentQueryDTO;
import com.quyong.attendance.module.department.dto.DepartmentSaveDTO;
import com.quyong.attendance.module.department.entity.Department;
import com.quyong.attendance.module.department.mapper.DepartmentMapper;
import com.quyong.attendance.module.department.service.DepartmentService;
import com.quyong.attendance.module.department.vo.DepartmentVO;
import com.quyong.attendance.module.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentMapper departmentMapper;
    private final UserMapper userMapper;

    public DepartmentServiceImpl(DepartmentMapper departmentMapper, UserMapper userMapper) {
        this.departmentMapper = departmentMapper;
        this.userMapper = userMapper;
    }

    @Override
    public List<DepartmentVO> list(DepartmentQueryDTO queryDTO) {
        String keyword = queryDTO == null ? null : normalize(queryDTO.getKeyword());

        LambdaQueryWrapper<Department> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            queryWrapper.like(Department::getName, keyword);
        }
        queryWrapper.orderByAsc(Department::getId);

        return departmentMapper.selectList(queryWrapper)
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public DepartmentVO add(DepartmentSaveDTO saveDTO) {
        String name = normalize(saveDTO == null ? null : saveDTO.getName());
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "部门名称不能为空");
        }

        ensureNameUnique(name, null);

        Department department = new Department();
        department.setName(name);
        department.setDescription(saveDTO == null ? null : saveDTO.getDescription());
        departmentMapper.insert(department);
        return toVO(department);
    }

    @Override
    public DepartmentVO update(DepartmentSaveDTO saveDTO) {
        Long id = saveDTO == null ? null : saveDTO.getId();
        Department department = id == null ? null : departmentMapper.selectById(id);
        if (department == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "部门不存在");
        }

        String name = normalize(saveDTO.getName());
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "部门名称不能为空");
        }

        ensureNameUnique(name, id);

        department.setName(name);
        department.setDescription(saveDTO.getDescription());
        departmentMapper.updateById(department);
        return toVO(department);
    }

    @Override
    public void delete(Long id) {
        Department department = departmentMapper.selectById(id);
        if (department == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "部门不存在");
        }

        if (userMapper.countByDeptId(id) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "部门下存在关联用户，不能删除");
        }

        departmentMapper.deleteById(id);
    }

    private void ensureNameUnique(String name, Long currentId) {
        LambdaQueryWrapper<Department> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Department::getName, name);
        if (currentId != null) {
            queryWrapper.ne(Department::getId, currentId);
        }

        if (departmentMapper.selectCount(queryWrapper) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "部门名称已存在");
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private DepartmentVO toVO(Department department) {
        DepartmentVO vo = new DepartmentVO();
        vo.setId(department.getId());
        vo.setName(department.getName());
        vo.setDescription(department.getDescription());
        return vo;
    }
}
```

- [ ] **Step 4: 再跑一次修改与删除测试，确认转绿**

Run: `mvn "-Dtest=DepartmentManagementIntegrationTest#shouldUpdateDepartmentWhenInputIsValid+shouldFailUpdateDepartmentWhenDepartmentDoesNotExist+shouldFailUpdateDepartmentWhenNameIsBlankAfterTrim+shouldFailUpdateDepartmentWhenNameAlreadyExistsAfterTrim+shouldDeleteDepartmentWhenNoUserReferencesIt+shouldFailDeleteDepartmentWhenDepartmentDoesNotExist+shouldFailDeleteDepartmentWhenDepartmentIsReferenced" test`

Expected: PASS，7 个测试全部通过。

### Task 3: 补部门安全回归测试并同步文档

**Files:**
- Modify: `src/test/java/com/quyong/attendance/DepartmentManagementIntegrationTest.java`
- Modify: `docs/api/API接口设计文档.md`
- Modify: `docs/test/测试用例文档.md`

- [ ] **Step 1: 追加部门接口的安全回归测试**

在 `src/test/java/com/quyong/attendance/DepartmentManagementIntegrationTest.java` 追加以下测试方法：

```java
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
```

- [ ] **Step 2: 运行部门模块最小回归测试**

Run: `mvn "-Dtest=DepartmentManagementIntegrationTest" test`

Expected: PASS，包含列表、新增、修改、删除和 `401/403` 回归的所有部门集成测试全部通过。

- [ ] **Step 3: 同步 API 接口文档**

把 `docs/api/API接口设计文档.md` 中的标题 `## 3. 用户管理接口` 改成 `## 3. 用户与部门管理接口`，并在用户管理小节后追加以下内容：

~~~md
### 3.5 查询部门列表
- 路径：`GET /api/department/list`
- 查询参数：`keyword`，可选，用于按部门名称模糊查询

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "研发部",
      "description": "负责系统研发"
    }
  ]
}
```

### 3.6 新增部门
- 路径：`POST /api/department/add`

```json
{
  "name": "财务部",
  "description": "负责财务管理"
}
```

### 3.7 修改部门
- 路径：`PUT /api/department/update`

```json
{
  "id": 2,
  "name": "人力资源部",
  "description": "负责人力资源与培训"
}
```

### 3.8 删除部门
- 路径：`DELETE /api/department/{id}`
- 业务约束：当部门仍被 `user.deptId` 引用时，返回 `code=400`，消息为 `部门下存在关联用户，不能删除`
~~~

- [ ] **Step 4: 同步测试用例文档**

在 `docs/test/测试用例文档.md` 中追加以下内容：

功能测试表追加：

```md
| TC021 | 部门管理 | 查询部门列表成功 | 管理员已登录，存在部门数据 | 访问部门列表接口 | 返回部门列表数据 |
| TC022 | 部门管理 | 新增部门成功 | 管理员已登录 | 提交合法部门名称与描述 | 部门新增成功 |
| TC023 | 部门管理 | 修改部门成功 | 管理员已登录，目标部门存在 | 提交合法修改信息 | 部门修改成功 |
| TC024 | 部门管理 | 删除无引用部门成功 | 管理员已登录，目标部门无用户引用 | 删除目标部门 | 删除成功 |
| TC025 | 部门管理 | 删除被引用部门失败 | 管理员已登录，目标部门存在关联用户 | 删除目标部门 | 提示部门下存在关联用户，不能删除 |
```

接口测试表追加：

```md
| API008 | `GET /api/department/list` | 查询部门列表 | 可选关键字参数 | 返回部门列表 |
| API009 | `POST /api/department/add` | 新增部门 | 合法部门参数 | 返回新增部门信息 |
| API010 | `PUT /api/department/update` | 修改部门 | 合法部门参数 | 返回更新后的部门信息 |
| API011 | `DELETE /api/department/{id}` | 删除部门 | 无引用的部门ID | 返回删除成功 |
```

安全测试表追加：

```md
| ST010 | 部门接口越权访问拦截 | 员工 token 访问 `/api/department/list` | 系统返回禁止访问 |
```

### Task 4: 全量验证与收口

**Files:**
- Verify only: `src/test/java/com/quyong/attendance/DepartmentManagementIntegrationTest.java`
- Verify only: `src/test/java/com/quyong/attendance/AuthSecurityIntegrationTest.java`

- [ ] **Step 1: 运行部门与鉴权相关回归**

Run: `mvn "-Dtest=DepartmentManagementIntegrationTest,AuthSecurityIntegrationTest" test`

Expected: PASS，部门管理与既有鉴权回归全部通过。

- [ ] **Step 2: 运行全量测试，确认没有破坏 Task 2**

Run: `mvn test`

Expected: `BUILD SUCCESS`，并且总测试结果无 `Failures`、无 `Errors`。

- [ ] **Step 3: 记录结果并准备进入下一任务**

完成后在交付说明中明确：

```text
Task 3 已完成部门 CRUD 与引用删除校验；验证命令已执行，未引入数据库结构变更。
```
