# BE-02 设备基础资料模块 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不调整数据库结构和接口路径的前提下，补齐 `device` 模块的列表、新增、修改、状态切换、删除能力，并保持 `deviceId` 对外契约稳定。

**Architecture:** 以 `DeviceServiceImpl` 作为设备管理门面，控制器保持薄层；新增 `DeviceValidationSupport` 承担字段归一化、唯一性、状态合法性、存在性和删除引用校验。设备持久化继续基于 MyBatis-Plus，实体内部保留 `id` 字段，对外通过 `DeviceVO` 映射为 `deviceId`。

**Tech Stack:** Spring Boot 2.7、Spring Security、MyBatis-Plus、H2、JUnit 5、MockMvc

> 当前用户要求：本计划执行阶段不包含 `git commit`、`git push`、创建 PR 或其他额外 Git 动作。

---

## 文件结构与职责

- `src/main/java/com/quyong/attendance/module/device/entity/Device.java`
  - 保持数据库字段 `id` 映射，必要时补齐 `@TableName`。
- `src/main/java/com/quyong/attendance/module/device/dto/DeviceQueryDTO.java`
  - 承载 `keyword`、`status` 查询条件。
- `src/main/java/com/quyong/attendance/module/device/dto/DeviceSaveDTO.java`
  - 对外新增/修改统一使用 `deviceId` 字段。
- `src/main/java/com/quyong/attendance/module/device/dto/DeviceStatusDTO.java`
  - 单独承载状态切换入参。
- `src/main/java/com/quyong/attendance/module/device/vo/DeviceVO.java`
  - 统一对外输出 `deviceId`。
- `src/main/java/com/quyong/attendance/module/device/mapper/DeviceMapper.java`
  - 提供设备引用计数查询。
- `src/main/java/com/quyong/attendance/module/device/support/DeviceValidationSupport.java`
  - 承担输入校验、存在性校验、状态校验、引用校验。
- `src/main/java/com/quyong/attendance/module/device/service/DeviceService.java`
  - 定义设备管理服务契约。
- `src/main/java/com/quyong/attendance/module/device/service/impl/DeviceServiceImpl.java`
  - 编排列表、新增、修改、状态切换、删除流程。
- `src/main/java/com/quyong/attendance/module/device/controller/DeviceController.java`
  - 暴露五个固定接口。
- `src/test/java/com/quyong/attendance/DeviceManagementIntegrationTest.java`
  - 覆盖设备管理端到端行为与安全边界。

### Task 1: 设备列表 RED/GREEN

**Files:**
- Create: `src/test/java/com/quyong/attendance/DeviceManagementIntegrationTest.java`
- Modify: `src/main/java/com/quyong/attendance/module/device/controller/DeviceController.java`
- Modify: `src/main/java/com/quyong/attendance/module/device/service/DeviceService.java`
- Modify: `src/main/java/com/quyong/attendance/module/device/service/impl/DeviceServiceImpl.java`
- Modify: `src/main/java/com/quyong/attendance/module/device/vo/DeviceVO.java`
- Modify: `src/main/java/com/quyong/attendance/module/device/entity/Device.java`

- [ ] **Step 1: 写出设备列表失败测试**

在 `DeviceManagementIntegrationTest` 中先落以下最小测试骨架：

```java
@Test
void shouldReturnDeviceListOrderedByDeviceId() throws Exception {
    String token = loginAndExtractToken("admin", "123456");

    mockMvc.perform(get("/api/device/list")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(3))
            .andExpect(jsonPath("$.data[0].deviceId").value("DEV-001"))
            .andExpect(jsonPath("$.data[1].deviceId").value("DEV-002"))
            .andExpect(jsonPath("$.data[2].deviceId").value("DEV-009"))
            .andExpect(jsonPath("$.data[0].id").doesNotExist());
}

@Test
void shouldFilterDeviceListByKeywordAndStatus() throws Exception {
    String token = loginAndExtractToken("admin", "123456");

    mockMvc.perform(get("/api/device/list")
                    .param("keyword", "办公区")
                    .param("status", "1")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(2));
}
```

- [ ] **Step 2: 运行 RED 测试确认当前接口未实现**

Run: `mvn "-Dtest=DeviceManagementIntegrationTest" test`

Expected: FAIL，失败原因应为 `/api/device/list` 返回空响应或断言不满足，而不是测试代码语法错误。

- [ ] **Step 3: 写最小实现让列表测试转绿**

最小实现应包含以下能力：

```java
public interface DeviceService {
    List<DeviceVO> list(DeviceQueryDTO queryDTO);
}
```

```java
@GetMapping("/list")
public Result<List<DeviceVO>> list(DeviceQueryDTO queryDTO) {
    return Result.success(deviceService.list(queryDTO));
}
```

```java
@Override
public List<DeviceVO> list(DeviceQueryDTO queryDTO) {
    String keyword = normalize(queryDTO == null ? null : queryDTO.getKeyword());
    LambdaQueryWrapper<Device> queryWrapper = new LambdaQueryWrapper<Device>();
    if (StringUtils.hasText(keyword)) {
        queryWrapper.and(wrapper -> wrapper.like(Device::getId, keyword)
                .or()
                .like(Device::getName, keyword)
                .or()
                .like(Device::getLocation, keyword));
    }
    if (queryDTO != null && queryDTO.getStatus() != null) {
        queryWrapper.eq(Device::getStatus, queryDTO.getStatus());
    }
    queryWrapper.orderByAsc(Device::getId);
    return deviceMapper.selectList(queryWrapper).stream().map(this::toVO).collect(Collectors.toList());
}
```

- [ ] **Step 4: 重跑设备列表测试**

Run: `mvn "-Dtest=DeviceManagementIntegrationTest" test`

Expected: 上述列表测试 PASS。

### Task 2: 新增、修改、状态切换 RED/GREEN

**Files:**
- Modify: `src/test/java/com/quyong/attendance/DeviceManagementIntegrationTest.java`
- Modify: `src/main/java/com/quyong/attendance/module/device/dto/DeviceSaveDTO.java`
- Create: `src/main/java/com/quyong/attendance/module/device/dto/DeviceStatusDTO.java`
- Create: `src/main/java/com/quyong/attendance/module/device/support/DeviceValidationSupport.java`
- Modify: `src/main/java/com/quyong/attendance/module/device/service/DeviceService.java`
- Modify: `src/main/java/com/quyong/attendance/module/device/service/impl/DeviceServiceImpl.java`
- Modify: `src/main/java/com/quyong/attendance/module/device/controller/DeviceController.java`

- [ ] **Step 1: 先补新增、修改、状态切换失败测试**

补以下核心测试：

```java
@Test
void shouldAddDeviceWhenInputIsValid() throws Exception { }

@Test
void shouldFailAddDeviceWhenDeviceIdAlreadyExists() throws Exception { }

@Test
void shouldUpdateDeviceWhenInputIsValid() throws Exception { }

@Test
void shouldUpdateDeviceStatusWhenInputIsValid() throws Exception { }

@Test
void shouldFailUpdateDeviceStatusWhenStatusIsInvalid() throws Exception { }
```

断言重点：

- 所有成功响应都返回 `deviceId`
- 新增默认 `status=1`
- 更新后 `deviceId` 不变
- 状态接口仅修改 `status`
- 非法状态返回 `设备状态不合法`

- [ ] **Step 2: 运行 RED 测试**

Run: `mvn "-Dtest=DeviceManagementIntegrationTest" test`

Expected: FAIL，失败原因来自接口未实现或校验缺失。

- [ ] **Step 3: 写最小实现让新增、修改、状态切换通过**

关键接口与方法签名：

```java
public interface DeviceService {
    List<DeviceVO> list(DeviceQueryDTO queryDTO);
    DeviceVO add(DeviceSaveDTO saveDTO);
    DeviceVO update(DeviceSaveDTO saveDTO);
    DeviceVO updateStatus(DeviceStatusDTO statusDTO);
    void delete(String deviceId);
}
```

```java
@PostMapping("/add")
public Result<DeviceVO> add(@RequestBody(required = false) DeviceSaveDTO saveDTO) { ... }

@PutMapping("/update")
public Result<DeviceVO> update(@RequestBody(required = false) DeviceSaveDTO saveDTO) { ... }

@PutMapping("/status")
public Result<DeviceVO> updateStatus(@RequestBody(required = false) DeviceStatusDTO statusDTO) { ... }
```

```java
public DeviceSaveDTO validateForCreate(DeviceSaveDTO saveDTO) { ... }
public DeviceSaveDTO validateForUpdate(DeviceSaveDTO saveDTO) { ... }
public Device requireExistingDevice(String deviceId) { ... }
public Integer requireValidStatus(Integer status) { ... }
```

- [ ] **Step 4: 重跑设备测试确认 GREEN**

Run: `mvn "-Dtest=DeviceManagementIntegrationTest" test`

Expected: 新增、修改、状态切换相关测试 PASS。

### Task 3: 删除与安全边界 RED/GREEN

**Files:**
- Modify: `src/test/java/com/quyong/attendance/DeviceManagementIntegrationTest.java`
- Modify: `src/main/java/com/quyong/attendance/module/device/mapper/DeviceMapper.java`
- Modify: `src/main/java/com/quyong/attendance/module/device/support/DeviceValidationSupport.java`
- Modify: `src/main/java/com/quyong/attendance/module/device/service/impl/DeviceServiceImpl.java`

- [ ] **Step 1: 先补删除和安全失败测试**

补以下测试：

```java
@Test
void shouldDeleteDeviceWhenNotReferenced() throws Exception { }

@Test
void shouldFailDeleteDeviceWhenReferencedByAttendanceRecord() throws Exception { }

@Test
void shouldReturnUnauthorizedWhenAccessingDeviceApiWithoutToken() throws Exception { }

@Test
void shouldReturnForbiddenWhenEmployeeAccessesDeviceApi() throws Exception { }
```

删除失败断言消息固定为：`设备已关联打卡记录，不能删除`。

- [ ] **Step 2: 运行 RED 测试**

Run: `mvn "-Dtest=DeviceManagementIntegrationTest,AuthSecurityIntegrationTest" test`

Expected: FAIL，删除引用校验和安全断言当前尚未完整满足。

- [ ] **Step 3: 写最小实现让删除与安全测试通过**

在 `DeviceMapper` 增加引用计数查询：

```java
@Select("SELECT COUNT(*) FROM attendanceRecord WHERE deviceId = #{deviceId}")
long countAttendanceRecordByDeviceId(@Param("deviceId") String deviceId);
```

在删除逻辑中补充：

```java
@Override
public void delete(String deviceId) {
    deviceValidationSupport.requireDeletableDevice(deviceId);
    deviceMapper.deleteById(deviceId);
}
```

- [ ] **Step 4: 重跑相关测试**

Run: `mvn "-Dtest=DeviceManagementIntegrationTest,AuthSecurityIntegrationTest" test`

Expected: PASS，未登录 `401`、员工越权 `403`、删除引用失败语义正确。

### Task 4: 回归验证

**Files:**
- Modify: `docs/api/API接口设计文档.md`（仅在实现发现必须补说明时）
- Modify: `docs/test/测试用例文档.md`（仅在实现发现必须补说明时）

- [ ] **Step 1: 运行设备模块最小验证**

Run: `mvn "-Dtest=DeviceManagementIntegrationTest,AuthSecurityIntegrationTest,ModuleSkeletonBeansTest" test`

Expected: PASS，设备模块能力已闭环且未破坏既有安全基线。

- [ ] **Step 2: 运行编译验证**

Run: `mvn -DskipTests compile`

Expected: `BUILD SUCCESS`。

- [ ] **Step 3: 输出交付说明**

交付说明必须明确：

```text
BE-02 已补齐设备列表、新增、修改、状态切换、删除能力；接口统一对外输出 deviceId；删除已被打卡记录引用的设备会返回业务错误；本次未改数据库结构，验证已执行。
```

## Plan Self-Review

### 1. Spec coverage

- 设备列表：Task 1 覆盖关键字、状态筛选与 `deviceId` 输出语义。
- 设备新增/修改/状态切换：Task 2 覆盖必填、唯一、状态合法性和固定主键约束。
- 删除设备：Task 3 覆盖存在性和引用阻断规则。
- 安全边界：Task 3 与 Task 4 覆盖未登录与越权场景。
- 验证：Task 4 覆盖最小测试和编译验证。

### 2. Placeholder scan

- 无 `TODO`、`TBD` 或“后续实现”占位描述。

### 3. Type consistency

- 对外统一使用 `deviceId`；实体内部统一使用 `id`；状态切换统一使用 `DeviceStatusDTO`。
