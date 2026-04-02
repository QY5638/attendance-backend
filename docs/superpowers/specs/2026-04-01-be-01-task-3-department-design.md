# BE-01 Task 3 部门管理设计文档

## 1. 目标

在现有 `BE-01` 用户与权限基础模块上，补齐 `department` 的最小必要 CRUD 能力，并完成“部门被用户引用时禁止删除”的业务校验。

本次设计只覆盖部门管理，不扩展到用户、角色之外的其他业务模块，不引入分页、详情接口、批量操作或数据库结构调整。

## 2. 约束与边界

### 2.1 已确认约束

- 继续在 worktree `D:\Graduation project\backend\.worktrees\be-01-user-auth-foundation` 中开发。
- 沿用当前项目结构、命名和统一返回格式。
- 沿用 `Task 2` 已确认的安全边界：`/api/department/**` 仅允许 `ADMIN` 访问。
- 业务异常继续走现有 `BusinessException + GlobalExceptionHandler`。
- 本次不修改数据库表结构，不新增唯一索引，不修改 `sql/schema` 和 `sql/seed`。

### 2.2 业务规则

- 部门名称必须非空。
- 部门名称在新增和修改时必须唯一。
- 名称唯一性校验前，对输入名称执行首尾空格去除。
- 删除部门前，若 `user.deptId` 仍引用该部门，则阻止删除。
- 删除被引用部门时，不做“转默认部门”或“级联删除用户”。

### 2.3 非目标

- 不新增分页查询。
- 不新增详情接口。
- 不新增数据库约束、触发器或额外表。
- 不扩展到 `Task 4/Task 5` 的角色、用户完整 CRUD。

## 3. 接口设计

### 3.1 接口范围

保留当前模块边界，仅实现以下接口：

1. `GET /api/department/list`
2. `POST /api/department/add`
3. `PUT /api/department/update`
4. `DELETE /api/department/{id}`

### 3.2 请求与响应模型

- 查询入参继续使用 `DepartmentQueryDTO`
  - `keyword`：按部门名称模糊查询
- 新增/修改入参继续使用 `DepartmentSaveDTO`
  - `id`
  - `name`
  - `description`
- 返回对象继续使用 `DepartmentVO`
  - `id`
  - `name`
  - `description`

不新增分页对象、详情对象或额外包装层。

### 3.3 返回语义

- 成功：HTTP `200`，响应体 `code=200`、`message=success`
- 参数或业务校验失败：HTTP `200`，响应体 `code=400`
- 未登录、token 无效、token 过期：HTTP `401`
- 已登录但角色不足：HTTP `403`

建议的业务错误消息：

- `部门名称不能为空`
- `部门名称已存在`
- `部门不存在`
- `部门下存在关联用户，不能删除`

## 4. 模块职责设计

### 4.1 Controller

`DepartmentController` 负责：

- 接收 HTTP 请求
- 绑定 DTO
- 调用 `DepartmentService`
- 使用 `Result.success(...)` 返回统一响应

控制器不承载名称唯一、引用校验等业务逻辑。

### 4.2 Service

`DepartmentService` / `DepartmentServiceImpl` 负责：

- 部门查询
- 新增前名称去空格与唯一性校验
- 修改前存在性与唯一性校验
- 删除前存在性与引用校验
- `Department` 与 `DepartmentVO` 的映射

本次不新增独立转换器、校验器或 facade，保持最小实现。

### 4.3 Mapper

- `DepartmentMapper` 继续基于 `BaseMapper<Department>`
- `UserMapper` 复用于删除前引用计数

优先使用 MyBatis-Plus 基础查询能力完成：

- 按名称精确查重
- 按名称模糊查询
- 按 `deptId` 统计用户数量

本次不新增 XML。

## 5. 业务流程

### 5.1 查询部门列表

1. 控制器接收 `keyword`
2. Service 判断 `keyword` 是否为空
3. 为空时查询全部部门；非空时按 `name` 模糊匹配
4. 结果按 `id` 升序返回
5. 映射为 `DepartmentVO` 列表后返回

### 5.2 新增部门

1. 读取 `name`
2. 对 `name` 执行 `trim`
3. 若为空，返回 `部门名称不能为空`
4. 查询是否存在同名部门
5. 若存在，返回 `部门名称已存在`
6. 插入新部门
7. 返回插入后的 `DepartmentVO`

### 5.3 修改部门

1. 校验 `id` 非空
2. 按 `id` 查询部门是否存在
3. 若不存在，返回 `部门不存在`
4. 对 `name` 执行 `trim`
5. 若为空，返回 `部门名称不能为空`
6. 查询“同名但 `id` 不同”的部门是否存在
7. 若存在，返回 `部门名称已存在`
8. 更新 `name`、`description`
9. 返回更新后的 `DepartmentVO`

### 5.4 删除部门

1. 按 `id` 查询部门是否存在
2. 若不存在，返回 `部门不存在`
3. 按 `deptId` 统计关联用户数量
4. 若数量大于 `0`，返回 `部门下存在关联用户，不能删除`
5. 若数量为 `0`，执行删除
6. 返回成功响应，`data` 为 `null`

## 6. 错误处理

本次设计沿用当前项目错误处理机制：

- 业务校验失败：抛出 `BusinessException`
- 参数绑定或校验失败：走全局异常处理器
- 鉴权失败：由现有安全链路返回 `401/403`

这能保持与 `Task 2` 一致的对外行为，避免部门模块出现新的异常风格。

## 7. 测试设计

### 7.1 测试方式

优先使用接口级集成测试覆盖完整链路：

- `@SpringBootTest`
- `@AutoConfigureMockMvc`
- `@ActiveProfiles("test")`
- `JdbcTemplate` 准备与清理测试数据

先写失败测试，再执行最小实现，遵循 TDD。

### 7.2 核心通过用例

- 管理员查询部门列表成功
- 管理员按关键字查询部门列表成功
- 管理员新增部门成功
- 管理员修改部门成功
- 管理员删除无引用部门成功

### 7.3 核心失败用例

- 未登录访问部门接口返回 `401`
- 员工访问部门接口返回 `403`
- 新增时部门名称为空返回 `400`
- 新增时部门名称重复返回 `400`
- 修改不存在部门返回 `400`
- 修改时与其他部门重名返回 `400`
- 删除不存在部门返回 `400`
- 删除被用户引用的部门返回 `400`

### 7.4 测试数据策略

- 继续复用 `src/test/resources/schema.sql` 和 `data.sql`
- 需要特定数据时，在测试用例中通过 `JdbcTemplate` 精确插入
- 引用删除校验的用户数据在测试内显式构造，避免依赖隐式种子数据

## 8. 文档同步范围

本次实现完成后，需要同步以下文档：

1. `docs/api/API接口设计文档.md`
   - 补充部门管理接口
   - 补充请求示例和返回语义
2. `docs/test/测试用例文档.md`
   - 补充部门 CRUD 测试点
   - 补充“部门存在引用时禁止删除”测试点

本次不需要同步数据库结构相关文档，因为没有表结构变更。

## 9. 验收标准

满足以下条件即可视为 `Task 3` 部门管理完成：

1. 部门列表、新增、修改、删除接口可用
2. 名称唯一规则生效
3. 被用户引用的部门无法删除
4. `ADMIN` 可访问，`EMPLOYEE` 不可访问
5. 相关测试先 RED 再 GREEN，并通过最小回归
6. `mvn test` 全量回归通过

## 10. 实现影响文件

预期会修改的核心文件：

- `src/main/java/com/quyong/attendance/module/department/controller/DepartmentController.java`
- `src/main/java/com/quyong/attendance/module/department/service/DepartmentService.java`
- `src/main/java/com/quyong/attendance/module/department/service/impl/DepartmentServiceImpl.java`
- `src/main/java/com/quyong/attendance/module/department/dto/DepartmentSaveDTO.java`
- `src/main/java/com/quyong/attendance/module/department/dto/DepartmentQueryDTO.java`
- `src/main/java/com/quyong/attendance/module/department/vo/DepartmentVO.java`
- `src/main/java/com/quyong/attendance/module/user/mapper/UserMapper.java`
- `src/test/java/com/quyong/attendance/...` 下新增或修改部门相关集成测试
- `docs/api/API接口设计文档.md`
- `docs/test/测试用例文档.md`

如果实现中发现现有 DTO 缺少最小必要注解或字段，可以在不扩大边界的前提下做小幅补充。
