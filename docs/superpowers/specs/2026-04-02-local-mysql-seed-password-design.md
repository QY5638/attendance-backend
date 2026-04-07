# 本机 MySQL seed 登录修复设计

## 1. 背景

当前项目认证链路已明确采用 `BCrypt` 密码校验：

- `src/main/java/com/quyong/attendance/config/SecurityConfig.java`
- `src/main/java/com/quyong/attendance/module/auth/service/impl/AuthServiceImpl.java`

其中：

- `SecurityConfig` 提供 `BCryptPasswordEncoder`
- `AuthServiceImpl` 在登录时通过 `passwordEncoder.matches(...)` 校验密码

但当前初始化脚本 `sql/seed/测试数据插入SQL.sql` 中，测试用户 `admin`、`zhangsan`、`lisi` 的密码仍然写成明文 `123456`。这导致：

1. 本机 MySQL 初始化本身虽然成功；
2. 应用也可以正常启动；
3. 但使用 seed 账号执行 `/api/auth/login` 时会返回“用户名或密码错误”。

本次目标不是修改认证逻辑，而是修复 seed 数据，使初始化后的 seed 账号能够按当前认证实现正常登录。

## 2. 目标

本次工作完成后，应满足以下结果：

1. `sql/seed/测试数据插入SQL.sql` 中的 seed 用户密码与当前 `BCrypt` 认证逻辑一致。
2. 重新执行 `schema + seed` 后，本机 MySQL 中的 seed 账号可使用明文口令 `123456` 登录。
3. `POST /api/auth/login` 使用 `admin/123456` 能返回登录成功结果。

## 3. 非目标

本次不包含以下内容：

1. 不修改 `SecurityConfig` 或 `AuthServiceImpl` 的认证逻辑。
2. 不为明文密码增加兼容分支。
3. 不修改测试代码、文档或测试文档。
4. 不处理“旧库只重复执行 seed 文件无法覆盖旧密码”的历史兼容问题。
5. 不调整 `INSERT IGNORE` 初始化策略。

## 4. 问题定位

### 4.1 认证约束

当前登录实现要求数据库中的 `user.password` 为 `BCrypt` 哈希，而不是明文。

### 4.2 seed 数据现状

`sql/seed/测试数据插入SQL.sql` 当前写法为：

```sql
(9001, 'admin', '123456', ...),
(1001, 'zhangsan', '123456', ...),
(1002, 'lisi', '123456', ...)
```

这与 `BCryptPasswordEncoder.matches(...)` 的使用方式不一致，因此 seed 账号登录失败是预期结果，而不是 MySQL 接入失败。

## 5. 方案选择

### 方案 A：把 seed 中的明文密码替换为预生成的 BCrypt 哈希（选用）

做法：

1. 只修改 `sql/seed/测试数据插入SQL.sql`
2. 将 `admin`、`zhangsan`、`lisi` 三个账号的密码值替换为对应 `123456` 的 `BCrypt` 哈希
3. 重新执行 `schema + seed`
4. 验证 `admin/123456` 可登录

优点：

1. 改动最小
2. 与现有认证实现完全一致
3. 不引入新的兼容逻辑

缺点：

1. SQL 中的密码值不再直观可读

### 方案 B：保留 seed 明文，修改认证逻辑兼容明文

优点：seed 文件更直观。

缺点：

1. 破坏当前已确定的密码安全约束
2. 把测试初始化问题扩散成生产认证逻辑问题
3. 容易形成不安全历史兼容代码

### 方案 C：导入 seed 后额外执行一次批量密码修正脚本

优点：可以保留 seed 可读性。

缺点：

1. 初始化流程变复杂
2. 容易漏执行
3. 与“只修 seed 登录”的最小边界不符

结论：选择方案 A。

## 6. 设计范围

### 6.1 修改范围

仅修改：

- `sql/seed/测试数据插入SQL.sql`

仅替换以下账号的 `password` 字段：

- `admin`
- `zhangsan`
- `lisi`

不改以下内容：

- 用户 ID
- 用户名
- 角色、部门、状态
- 其他表 seed 数据
- 认证代码

### 6.2 保持的初始化约束

当前 seed 继续保留 `INSERT IGNORE` 风格，不额外引入 `UPDATE` 或二次修复逻辑。

这意味着：

1. 只重复执行 seed 文件本身，不能覆盖已存在的旧明文密码
2. 本次验证必须基于“重新执行 `schema + seed` 的全新库”

## 7. 验证设计

本次验证不以 `mvn test` 作为主验收证据，原因是现有测试里的用户数据主要由测试代码自行准备，并不依赖 `sql/seed/测试数据插入SQL.sql`。

主验证步骤应为：

1. 重新执行 `sql/schema/数据库建表SQL.sql`
2. 重新执行修正后的 `sql/seed/测试数据插入SQL.sql`
3. 查询 `user` 表，确认 `admin`、`zhangsan`、`lisi` 的密码值不再是明文 `123456`
4. 启动应用
5. 验证 `GET /api/health` 仍成功
6. 验证 `POST /api/auth/login` 使用 `admin/123456` 返回成功，并包含 `token`、`roleCode`

## 8. 风险与边界

### 8.1 历史数据边界

本次只保证“使用修正后的 seed 初始化全新库后可登录”，不保证对已经存在旧明文密码数据的库自动修复。

### 8.2 哈希稳定性边界

`BCrypt` 同一明文每次生成的哈希都可能不同，因此本次只要求：

1. 写入值是合法 `BCrypt` 哈希
2. 能与明文 `123456` 匹配成功

不要求运行时重新生成哈希；只要 SQL 中写入的是可与 `123456` 匹配成功的合法 `BCrypt` 哈希即可。

### 8.3 SQL 执行方式边界

当前环境是 PowerShell 5.1，已验证文本管道在处理中文 SQL 时存在转码风险。本次执行初始化时，应继续优先采用不会破坏中文内容的原始文件重定向方式。

## 9. 最小改动原则

本次仅修复 seed 数据与认证约束的不一致，不把问题扩展为认证重构、初始化机制改造或文档批量修订。只要 `schema + seed` 后 `admin/123456` 能正常登录，就视为本次问题闭环。
