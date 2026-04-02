# BE-01 Task 4 用户管理设计文档

## 1. 目标

在现有 `BE-01` 用户与权限基础模块上，补齐 `user` 的最小必要 CRUD 能力，形成“登录认证 + 部门管理 + 用户管理”的闭环。

本次设计只覆盖用户管理，不扩展到角色管理、人脸、考勤、异常分析等其他业务模块，不引入分页、详情接口、批量操作、数据库结构调整或新的后台子系统。

## 2. 约束与边界

### 2.1 已确认约束

- 继续在 worktree `D:\Graduation project\backend\.worktrees\be-01-user-auth-foundation` 中开发。
- 沿用当前项目结构、命名、统一返回格式和现有安全链路。
- 用户管理接口继续受当前安全边界保护，仅允许 `ADMIN` 访问。
- 业务异常继续走现有 `BusinessException + GlobalExceptionHandler`。
- 密码统一使用 `BCrypt` 编码。
- 本次不修改数据库表结构，不新增表、不新增字段、不调整 SQL 文档。

### 2.2 业务规则

- 接口范围仅包含用户列表、新增、修改、删除。
- 查询条件继续使用现有 `UserQueryDTO`：`keyword`、`deptId`、`status`。
- `keyword` 按 `username`、`realName` 模糊查询。
- 新增用户时，`username`、`password`、`realName`、`deptId`、`roleId` 必填。
- 新增用户时，`status` 为空则默认写入 `1`。
- 修改用户时，`id`、`username`、`realName`、`deptId`、`roleId`、`status` 必填。
- 修改用户时，`password` 为可选；未提供或为空白时保留原密码，提供有效值时重新编码覆盖。
- `status` 只允许 `0` 或 `1`。
- `username` 在新增和修改时必须唯一。
- `deptId` 必须指向存在的部门。
- `roleId` 必须指向存在的角色。
- 删除用户前仅校验用户是否存在；本次不扩展软删除、审计记录或 token 失效联动。

### 2.3 非目标

- 不新增用户详情接口。
- 不新增分页查询。
- 不新增批量新增、批量删除、批量导入。
- 不新增单独的启停接口、重置密码接口。
- 不扩展角色管理接口。
- 不返回 `deptName`、`roleName` 等联表展示字段。
- 不调整当前 token 快照语义。

## 3. 接口设计

### 3.1 接口范围

保留当前模块边界，仅实现以下接口：

1. `GET /api/user/list`
2. `POST /api/user/add`
3. `PUT /api/user/update`
4. `DELETE /api/user/{id}`

### 3.2 请求与响应模型

- 查询入参继续使用 `UserQueryDTO`
  - `keyword`
  - `deptId`
  - `status`
- 新增/修改入参继续使用 `UserSaveDTO`
  - `id`
  - `username`
  - `password`
  - `realName`
  - `gender`
  - `phone`
  - `deptId`
  - `roleId`
  - `status`
- 返回对象继续使用 `UserVO`
  - `id`
  - `username`
  - `realName`
  - `gender`
  - `phone`
  - `deptId`
  - `roleId`
  - `status`
  - `createTime`

密码只参与写入，不出现在任何返回对象中。

### 3.3 返回语义

- 成功：HTTP `200`，响应体 `code=200`、`message=success`
- 参数或业务校验失败：HTTP `200`，响应体 `code=400`
- 未登录、token 无效、token 过期：HTTP `401`
- 已登录但角色不足：HTTP `403`

建议的业务错误消息：

- `用户名不能为空`
- `用户名已存在`
- `密码不能为空`
- `姓名不能为空`
- `部门不存在`
- `角色不存在`
- `用户不存在`
- `用户状态不合法`

## 4. 模块职责设计

### 4.1 Controller

`UserController` 负责：

- 接收 HTTP 请求
- 绑定 DTO
- 调用 `UserService`
- 使用 `Result.success(...)` 返回统一响应

控制器不承载字段校验、密码编码、引用检查等业务逻辑。

### 4.2 Service

`UserService` / `UserServiceImpl` 负责：

- 用户列表查询
- 用户新增、修改、删除编排
- 调用用户校验与密码处理支撑组件
- `User` 与 `UserVO` 的映射

`UserServiceImpl` 仍作为唯一业务门面，不再继续拆出额外 facade 或 converter。

### 4.3 校验支撑组件

为避免所有规则堆叠在 `UserServiceImpl` 中，本次新增一个轻量校验支撑组件：`UserValidationSupport`。

它负责：

- 统一执行字符串归一化后的必填校验
- 校验用户名唯一
- 校验目标用户是否存在
- 校验部门是否存在
- 校验角色是否存在
- 校验 `status` 是否为 `0/1`

该组件只承载校验和读取，不直接保存用户。

### 4.4 密码支撑组件

本次新增一个轻量密码支撑组件：`UserPasswordSupport`。

它负责：

- 新增场景下校验密码是否为空
- 新增场景下执行 `BCrypt` 编码
- 修改场景下决定“保留原密码”还是“重新编码覆盖”

该组件不参与查询和其他业务判断，只聚焦密码规则。

### 4.5 Mapper

- `UserMapper` 继续基于 `BaseMapper<User>`
- `DepartmentMapper` 复用于部门存在校验
- `RoleMapper` 复用于角色存在校验

查询优先使用 MyBatis-Plus 基础能力完成：

- 按 `username` 精确查重
- 按 `username`、`realName` 模糊查询
- 按 `deptId`、`status` 条件过滤

本次不新增 XML。

## 5. 业务流程

### 5.1 查询用户列表

1. 控制器接收 `UserQueryDTO`
2. Service 对 `keyword` 执行归一化
3. `keyword` 非空时，按 `username` 或 `realName` 模糊查询
4. `deptId`、`status` 非空时追加过滤条件
5. 结果按 `id` 升序返回
6. 映射为 `UserVO` 列表后返回

### 5.2 新增用户

1. 读取 `UserSaveDTO`
2. 对 `username`、`realName`、`phone` 执行 `trim`
3. 校验 `username` 非空
4. 校验 `realName` 非空
5. 校验用户名唯一
6. 校验部门存在
7. 校验角色存在
8. 若 `status` 为空，默认设置为 `1`
9. 校验 `status` 合法
10. 校验密码非空并执行 `BCrypt` 编码
11. 插入新用户
12. 返回插入后的 `UserVO`

### 5.3 修改用户

1. 校验 `id` 非空
2. 按 `id` 查询用户是否存在
3. 若不存在，返回 `用户不存在`
4. 对 `username`、`realName`、`phone` 执行 `trim`
5. 校验 `username` 非空
6. 校验 `realName` 非空
7. 校验用户名唯一，排除当前用户自身
8. 校验部门存在
9. 校验角色存在
10. 校验 `status` 为 `0` 或 `1`
11. 若 `password` 提供了有效值，则重新编码后覆盖；否则保留原密码
12. 更新用户字段
13. 返回更新后的 `UserVO`

### 5.4 删除用户

1. 按 `id` 查询用户是否存在
2. 若不存在，返回 `用户不存在`
3. 执行删除
4. 返回成功响应，`data` 为 `null`

## 6. 错误处理与风险说明

本次设计沿用当前项目错误处理机制：

- 业务校验失败：抛出 `BusinessException`
- 鉴权失败：由现有安全链路返回 `401/403`
- 不依赖数据库异常作为主要业务分支

本次明确不处理以下扩展问题：

- 删除已登录用户后，服务端 token 是否即时失效
- 用户名并发写入竞争下的最终一致性增强
- 角色禁用后对用户管理列表展示的联动语义
- 软删除、操作日志、审计字段扩展

这些问题后续如进入更完整的后台管理阶段，再单独设计。

## 7. 测试设计

### 7.1 测试方式

优先使用接口级集成测试覆盖完整链路：

- `@SpringBootTest`
- `@AutoConfigureMockMvc`
- `@ActiveProfiles("test")`
- `JdbcTemplate` 准备与清理测试数据

先写失败测试，再执行最小实现，遵循 TDD。

### 7.2 核心通过用例

- 管理员查询用户列表成功
- 管理员按关键字查询用户列表成功
- 管理员按部门筛选用户成功
- 管理员按状态筛选用户成功
- 管理员新增用户成功
- 管理员修改用户成功
- 修改时不传密码可保留原密码
- 修改时传入新密码后可用新密码登录
- 管理员删除用户成功

### 7.3 核心失败用例

- 未登录访问用户接口返回 `401`
- 员工访问用户接口返回 `403`
- 新增时用户名为空返回 `400`
- 新增时密码为空返回 `400`
- 新增时姓名为空返回 `400`
- 新增时用户名重复返回 `400`
- 新增时部门不存在返回 `400`
- 新增时角色不存在返回 `400`
- 新增时状态非法返回 `400`
- 修改不存在用户返回 `400`
- 修改时用户名重复返回 `400`
- 修改时部门不存在返回 `400`
- 修改时角色不存在返回 `400`
- 修改时状态非法返回 `400`
- 删除不存在用户返回 `400`

### 7.4 测试数据策略

- 继续复用 `src/test/resources/schema.sql` 和 `data.sql`
- 在测试内通过 `JdbcTemplate` 显式插入 `role`、`department`、`user` 数据
- 需要验证密码更新时，直接复用现有登录接口完成端到端断言

## 8. 文档同步范围

本次实现完成后，需要同步以下文档：

1. `docs/api/API接口设计文档.md`
   - 补充用户管理接口的请求参数和响应示例
   - 明确用户修改时密码为可选更新字段
2. `docs/test/测试用例文档.md`
   - 补充用户 CRUD 测试点
   - 补充用户名唯一、状态筛选、密码更新验证等测试点

本次不需要同步数据库结构相关文档，因为没有表结构变更。

## 9. 验收标准

满足以下条件即可视为 `Task 4` 用户管理完成：

1. 用户列表、新增、修改、删除接口可用
2. 用户名唯一规则生效
3. 部门与角色存在校验生效
4. 密码新增与更新规则符合设计
5. `ADMIN` 可访问，`EMPLOYEE` 不可访问
6. 相关测试先 RED 再 GREEN，并通过最小回归
7. `mvn test` 全量回归通过

## 10. 实现影响文件

预期会修改或新增的核心文件：

- `src/main/java/com/quyong/attendance/module/user/controller/UserController.java`
- `src/main/java/com/quyong/attendance/module/user/service/UserService.java`
- `src/main/java/com/quyong/attendance/module/user/service/impl/UserServiceImpl.java`
- `src/main/java/com/quyong/attendance/module/user/mapper/UserMapper.java`
- `src/main/java/com/quyong/attendance/module/user/dto/UserSaveDTO.java`
- `src/main/java/com/quyong/attendance/module/user/dto/UserQueryDTO.java`
- `src/main/java/com/quyong/attendance/module/user/vo/UserVO.java`
- `src/main/java/com/quyong/attendance/module/user/...` 下新增轻量校验/密码支撑组件
- `src/test/java/com/quyong/attendance/...` 下新增或修改用户管理相关集成测试
- `docs/api/API接口设计文档.md`
- `docs/test/测试用例文档.md`

如果实现中发现现有 DTO 或 VO 缺少最小必要字段或方法，可以在不扩大边界的前提下做小幅补充。
