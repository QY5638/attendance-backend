# BE-02 设备删除失败提示优化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不修改设备删除规则、响应结构和错误码语义的前提下，把“已关联打卡记录”场景的删除失败提示升级为“请先停用设备”，并同步测试与文档。

**Architecture:** 沿用现有 `DeviceValidationSupport` 作为删除前业务校验入口，不改控制器、服务编排和接口路径。先用 `DeviceManagementIntegrationTest` 锁定新文案，再最小修改 `BusinessException` 消息，最后同步 API 文档与测试用例文档，并执行最小回归验证。

**Tech Stack:** Spring Boot 2.7、Spring Security、MyBatis-Plus、H2、JUnit 5、MockMvc、Markdown

> 当前用户要求：本计划执行阶段不包含 `git commit`、`git push`、创建 PR 或其他额外 Git 动作。

---

## 文件结构与职责

- `src/main/java/com/quyong/attendance/module/device/support/DeviceValidationSupport.java`
  - 设备删除前的存在性校验与引用校验入口；本次唯一业务代码改动点在这里。
- `src/test/java/com/quyong/attendance/DeviceManagementIntegrationTest.java`
  - 设备管理端到端集成测试；本次用于先锁定删除已引用设备时的新提示文案。
- `docs/api/API接口设计文档.md`
  - 对外接口契约文档；同步删除设备接口的失败消息说明。
- `docs/test/测试用例文档.md`
  - 接口测试用例表；同步 `API030` 的预期结果。
- `src/main/java/com/quyong/attendance/module/device/service/impl/DeviceServiceImpl.java`
  - 删除流程入口仍保持 `requireDeletableDevice` -> `deleteById`，本次无需修改，用于帮助实现人员确认不要误改服务编排。

### Task 1: 删除失败提示 RED/GREEN

**Files:**
- Modify: `src/test/java/com/quyong/attendance/DeviceManagementIntegrationTest.java:378-388`
- Modify: `src/main/java/com/quyong/attendance/module/device/support/DeviceValidationSupport.java:84-88`

- [ ] **Step 1: 先把删除已引用设备测试改成新文案断言**

在 `DeviceManagementIntegrationTest` 中把该测试方法调整为以下内容，只改最后一条 `message` 断言：

```java
@Test
void shouldFailDeleteDeviceWhenReferencedByAttendanceRecord() throws Exception {
    String token = loginAndExtractToken("admin", "123456");
    insertAttendanceRecord(2001L, 1001L, "DEV-001");

    mockMvc.perform(delete("/api/device/DEV-001")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("设备已关联打卡记录，不能删除，请先停用设备"));
}
```

- [ ] **Step 2: 运行 RED 测试确认当前实现仍返回旧文案**

Run: `mvn "-Dtest=DeviceManagementIntegrationTest#shouldFailDeleteDeviceWhenReferencedByAttendanceRecord" test`

Expected: FAIL，断言差异应明确显示当前返回值仍是 `设备已关联打卡记录，不能删除`，而不是测试代码语法错误或上下文启动失败。

- [ ] **Step 3: 在删除校验里写最小实现**

只修改 `DeviceValidationSupport.requireDeletableDevice` 的异常文案，不改查询逻辑、错误码、返回结构：

```java
public Device requireDeletableDevice(String deviceId) {
    Device device = requireExistingDevice(deviceId);
    if (deviceMapper.countAttendanceRecordByDeviceId(device.getId()) > 0) {
        throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "设备已关联打卡记录，不能删除，请先停用设备");
    }
    return device;
}
```

- [ ] **Step 4: 重跑单场景测试确认 GREEN**

Run: `mvn "-Dtest=DeviceManagementIntegrationTest#shouldFailDeleteDeviceWhenReferencedByAttendanceRecord" test`

Expected: PASS，说明删除规则仍然阻断，但返回提示已升级为新文案。

### Task 2: 同步 API 与测试文档

**Files:**
- Modify: `docs/api/API接口设计文档.md:241-246`
- Modify: `docs/test/测试用例文档.md:138-140`

- [ ] **Step 1: 更新 API 接口设计文档中的删除失败说明**

将 `docs/api/API接口设计文档.md` 的删除设备小节改为以下内容：

```md
### 3.13 删除设备
- 路径：`DELETE /api/device/{deviceId}`
- 业务约束：
  - 设备不存在时返回 `code=400`，消息为 `设备不存在`
  - 设备已被 `attendanceRecord.deviceId` 引用时，返回 `code=400`，消息为 `设备已关联打卡记录，不能删除，请先停用设备`
```

- [ ] **Step 2: 更新测试用例文档中的 API030 预期结果**

将 `docs/test/测试用例文档.md` 中 `API030` 对应行改为：

```md
| API030 | `DELETE /api/device/{deviceId}` | 删除已引用设备 | 已被打卡记录引用的设备编号 | 返回 `设备已关联打卡记录，不能删除，请先停用设备` |
```

- [ ] **Step 3: 人工复查文档边界，避免误改历史设计/计划文档**

本次只同步面向接口使用者和测试执行者的文档：

```text
保留 docs/superpowers/specs/2026-04-02-be-02-device-management-design.md
保留 docs/superpowers/plans/2026-04-02-be-02-device-management.md
仅新增本次 follow-up 计划并修改 API/测试文档
```

预期结果：历史设计与历史实施计划保留原始交付记录，本次变更由新的 follow-up 设计与计划承接。

### Task 3: 最小回归验证

**Files:**
- Verify: `src/test/java/com/quyong/attendance/DeviceManagementIntegrationTest.java`
- Verify: `docs/api/API接口设计文档.md`
- Verify: `docs/test/测试用例文档.md`

- [ ] **Step 1: 运行设备集成测试类做最小回归**

Run: `mvn "-Dtest=DeviceManagementIntegrationTest" test`

Expected: PASS，至少确认以下行为未变：

```text
删除未引用设备仍成功
删除不存在设备仍返回 设备不存在
删除已引用设备仍返回 code=400
删除已引用设备的新 message 为 设备已关联打卡记录，不能删除，请先停用设备
```

- [ ] **Step 2: 仅在最小回归出现共享影响时再跑全量测试**

Run: `mvn test`

Expected: 只有在出现公共断言、统一异常处理或公共测试夹具被波及时才执行；若执行，结果应为 `BUILD SUCCESS`。

- [ ] **Step 3: 输出交付说明**

交付说明应明确写出以下结论：

```text
本次只优化设备删除失败提示文案，未修改删除规则本身；当设备已关联打卡记录时，接口仍返回失败和 code=400，但 message 已更新为“设备已关联打卡记录，不能删除，请先停用设备”；相关集成测试和 API/测试文档已同步。
```

## Plan Self-Review

### 1. Spec coverage

- 规格第 4.1 节“保持现有删除规则”：Task 1 只改 `BusinessException` 文案，不改删除流程、响应结构和错误码。
- 规格第 4.2 节“仅优化业务错误文案”：Task 1 明确把旧消息替换为 `设备已关联打卡记录，不能删除，请先停用设备`。
- 规格第 5 节“影响范围”：Task 1 覆盖业务代码与集成测试，Task 2 覆盖 API 文档和测试文档。
- 规格第 6 节“验证”：Task 3 覆盖最小 `DeviceManagementIntegrationTest` 回归，并保留必要时执行 `mvn test` 的兜底步骤。

### 2. Placeholder scan

- 无 `TODO`、`TBD`、`implement later` 等占位词。
- 每个代码修改步骤都给出了可直接落地的代码或文档片段。
- 每个验证步骤都给出了明确命令和期望结果。

### 3. Type consistency

- 删除入口统一仍为 `DELETE /api/device/{deviceId}`。
- 业务校验方法统一仍为 `requireDeletableDevice(String deviceId)`。
- 新旧差异仅限消息文本从 `设备已关联打卡记录，不能删除` 升级为 `设备已关联打卡记录，不能删除，请先停用设备`。
