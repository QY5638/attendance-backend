# BE-02 设备基础资料模块设计文档

## 1. 目标

在现有后端骨架上补齐 `device` 模块的最小必要管理能力，形成设备列表、设备新增、设备修改、设备状态切换、设备删除的闭环，为后续 `BE-04` 打卡记录模块提供稳定的设备主数据。

本次设计只覆盖 `BE-02` 设备基础资料模块，不扩展到打卡、人脸、异常分析、预警、统计等其他业务域，不引入分页、详情接口、批量操作、逻辑删除或数据库结构调整。

## 2. 约束与边界

### 2.1 已确认约束

- 在 worktree `D:\Graduation project\backend\.worktrees\be-02-device-module` 中开发。
- 沿用当前项目结构、统一返回、异常处理和安全链路。
- 设备管理接口继续受现有安全边界保护，仅允许 `ADMIN` 访问。
- 业务异常继续走 `BusinessException + GlobalExceptionHandler`。
- 本次不修改数据库表结构，不新增字段、不调整 SQL。
- 接口层统一对外使用 `deviceId`；数据库表 `device` 继续使用主键字段 `id`。

### 2.2 业务规则

- 接口范围固定为：`GET /api/device/list`、`POST /api/device/add`、`PUT /api/device/update`、`PUT /api/device/status`、`DELETE /api/device/{deviceId}`。
- `deviceId` 是固定主键，新增后不允许修改；更新接口中的 `deviceId` 只用于定位设备。
- 列表接口本次不分页，仅支持 `keyword`、`status` 查询。
- `keyword` 按 `deviceId`、`name`、`location` 模糊查询。
- 新增设备时，`deviceId`、`name` 必填；`location`、`description` 可选。
- 新增设备时，`status` 为空则默认写入 `1`。
- 修改设备时，`deviceId`、`name` 必填；`status` 仍可随主更新接口一并更新。
- 单独状态接口只允许修改 `status`，且 `status` 仅允许 `0` 或 `1`。
- 设备编号在新增时必须唯一。
- 删除设备前必须校验设备是否存在。
- 删除设备前必须校验是否已被 `attendanceRecord` 引用；若已被引用，禁止删除，只允许停用。

### 2.3 非目标

- 不新增设备详情接口。
- 不新增分页查询。
- 不新增批量导入、批量删除、批量状态切换。
- 不新增设备逻辑删除字段。
- 不联动生成或清理打卡记录。
- 不调整现有 API 主文档中的固定路径。

## 3. 接口设计

### 3.1 请求与响应模型

- 查询入参继续使用 `DeviceQueryDTO`
  - `keyword`
  - `status`
- 新增/修改入参继续使用 `DeviceSaveDTO`
  - `deviceId`
  - `name`
  - `location`
  - `status`
  - `description`
- 新增状态切换入参新增 `DeviceStatusDTO`
  - `deviceId`
  - `status`
- 返回对象继续使用 `DeviceVO`
  - `deviceId`
  - `name`
  - `location`
  - `status`
  - `description`

### 3.2 返回语义

- 成功：HTTP `200`，响应体 `code=200`、`message=success`
- 参数或业务校验失败：HTTP `200`，响应体 `code=400`
- 未登录、token 无效、token 过期：HTTP `401`
- 已登录但角色不足：HTTP `403`

建议的业务错误消息：

- `设备编号不能为空`
- `设备名称不能为空`
- `设备编号已存在`
- `设备不存在`
- `设备状态不合法`
- `设备已关联打卡记录，不能删除`

## 4. 模块职责设计

### 4.1 Controller

`DeviceController` 负责：

- 接收 HTTP 请求
- 绑定 DTO
- 调用 `DeviceService`
- 使用 `Result.success(...)` 返回统一响应

控制器不承载字段归一化、唯一性校验、引用校验等业务逻辑。

### 4.2 Service

`DeviceService` / `DeviceServiceImpl` 负责：

- 设备列表查询
- 设备新增、修改、状态切换、删除编排
- `Device` 与 `DeviceVO` 的映射
- `id -> deviceId` 的对外字段转换

### 4.3 校验支撑组件

新增轻量支撑组件 `DeviceValidationSupport`，负责：

- 统一执行字符串归一化
- 校验 `deviceId`、`name` 必填
- 校验设备是否存在
- 校验设备编号唯一
- 校验 `status` 是否为 `0/1`
- 校验设备是否已被打卡记录引用

该组件只承载校验和读取，不直接保存设备。

### 4.4 Mapper

- `DeviceMapper` 继续基于 `BaseMapper<Device>`
- 增加按设备编号统计打卡记录引用数的方法
- 查询优先使用 MyBatis-Plus 基础能力，不新增 XML

## 5. 业务流程

### 5.1 查询设备列表

1. 控制器接收 `DeviceQueryDTO`
2. Service 对 `keyword` 执行归一化
3. `keyword` 非空时，按 `id`、`name`、`location` 模糊查询
4. `status` 非空时追加过滤条件
5. 结果按 `id` 升序返回
6. 映射为 `DeviceVO`，对外字段统一输出 `deviceId`

### 5.2 新增设备

1. 对 `deviceId`、`name`、`location`、`description` 执行 `trim`
2. 校验 `deviceId` 非空
3. 校验 `name` 非空
4. 校验设备编号唯一
5. 若 `status` 为空，默认设置为 `1`
6. 校验 `status` 合法
7. 插入设备
8. 返回新增后的 `DeviceVO`

### 5.3 修改设备

1. 校验 `deviceId` 非空
2. 按 `deviceId` 查询设备是否存在
3. 若不存在，返回 `设备不存在`
4. 对 `name`、`location`、`description` 执行 `trim`
5. 校验 `name` 非空
6. 若请求中带 `status`，校验 `status` 为 `0` 或 `1`
7. 更新名称、位置、状态、描述
8. 返回更新后的 `DeviceVO`

### 5.4 更新设备状态

1. 校验 `deviceId` 非空
2. 校验 `status` 为 `0` 或 `1`
3. 按 `deviceId` 查询设备是否存在
4. 若不存在，返回 `设备不存在`
5. 仅更新状态字段
6. 返回更新后的 `DeviceVO`

### 5.5 删除设备

1. 按 `deviceId` 查询设备是否存在
2. 若不存在，返回 `设备不存在`
3. 查询设备是否已被 `attendanceRecord` 引用
4. 若引用数大于 `0`，返回 `设备已关联打卡记录，不能删除`
5. 执行删除
6. 返回成功响应，`data` 为 `null`

## 6. 错误处理与风险说明

- 业务校验失败：抛出 `BusinessException`
- 畸形 JSON：沿用 `GlobalExceptionHandler` 返回 `请求参数错误`
- 鉴权失败：由现有安全链路返回 `401/403`

本次明确不处理以下扩展问题：

- 设备被禁用后对下游打卡行为的联动限制
- 并发新增相同设备编号的最终一致性增强
- 删除设备后的审计日志与软删除追踪
- 设备状态枚举扩展到更多业务态

## 7. 测试设计

### 7.1 测试方式

优先使用接口级集成测试覆盖完整链路：

- `@SpringBootTest`
- `@AutoConfigureMockMvc`
- `@ActiveProfiles("test")`
- `JdbcTemplate` 准备与清理测试数据

实现阶段遵循 TDD：先写失败测试，再写最小实现。

### 7.2 核心通过用例

- 管理员查询设备列表成功
- 管理员按关键字查询设备成功
- 管理员按状态查询设备成功
- 管理员新增设备成功
- 管理员修改设备成功
- 管理员切换设备状态成功
- 管理员删除未引用设备成功

### 7.3 核心失败用例

- 新增时设备编号为空
- 新增时设备名称为空
- 新增时设备编号重复
- 新增或修改时设备状态不合法
- 修改或状态切换时设备不存在
- 删除已被打卡记录引用的设备失败
- 畸形 JSON 返回 `请求参数错误`
- 未登录访问设备接口返回 `401`
- 员工访问设备接口返回 `403`

## 8. 验证策略

最小验证命令：

```bash
mvn "-Dtest=DeviceManagementIntegrationTest,AuthSecurityIntegrationTest,ModuleSkeletonBeansTest" test
mvn -DskipTests compile
```

如设备测试落地后，再视结果决定是否补跑全量 `mvn test`。
