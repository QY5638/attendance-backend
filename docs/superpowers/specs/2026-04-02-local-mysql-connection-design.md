# 本机 MySQL 接入与初始化设计

## 1. 背景

当前后端项目已经具备 MySQL 运行时依赖，并且 `dev` / `prod` 环境均通过环境变量读取数据源配置：

- `src/main/resources/application-dev.yml`
- `src/main/resources/application-prod.yml`

默认连接目标为：

- 数据库：`system`
- 主机：`localhost`
- 端口：`3306`
- 用户名：`root`

当前用户目标不是修改业务功能，而是让项目能够直接连接本机 MySQL，并完成本机数据库初始化，确保后端可基于本机数据库正常启动。

## 2. 目标

本次工作完成后，应满足以下结果：

1. 项目在本机 `dev` 环境下通过 MySQL 数据源启动。
2. 数据库连接信息通过环境变量提供，不把密码写入仓库配置文件。
3. 本机 `system` 库按现有 SQL 脚本完成重建和初始化。
4. 初始化后可验证基础表结构和种子数据存在。

## 3. 非目标

本次不包含以下内容：

1. 不修改业务代码逻辑。
2. 不重构现有 profile 结构。
3. 不把数据库密码写入 `application-dev.yml`。
4. 不顺带处理与 MySQL 无关的问题，例如新的业务模块开发。
5. 不默认处理 Redis 等其他外部依赖；若启动时 Redis 成为阻塞项，再单独分析。

## 4. 当前项目现状

### 4.1 数据源配置

项目当前已采用环境变量兜底：

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `DB_DRIVER`

其中 `application-dev.yml` 的默认值已经指向：

`jdbc:mysql://localhost:3306/system?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true`

这意味着本次接入不需要重做配置结构，优先沿用现有方式。

### 4.2 SQL 初始化脚本

项目已经提供本机初始化脚本：

- `sql/schema/数据库建表SQL.sql`
- `sql/seed/测试数据插入SQL.sql`

其中建表脚本开头包含：

```sql
DROP DATABASE IF EXISTS `system`;
CREATE DATABASE `system` ...;
```

因此本次初始化策略是：直接接受删库重建，得到一个与当前项目脚本一致的全新本机库。

## 5. 方案选择

### 方案 A：环境变量接入 + 手动执行现有 SQL（选用）

做法：

1. 保持 `application-dev.yml` 现状。
2. 在本机设置 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`。
3. 连接本机 MySQL。
4. 执行现有 schema 和 seed 脚本。
5. 启动后端验证连通性。

优点：

1. 与项目当前配置完全一致。
2. 不把密码写入仓库。
3. 后续切换数据库时只需调整本机环境变量。

缺点：

1. 首次使用时需要多一步环境变量设置。

### 方案 B：把本机连接信息直接写入 `application-dev.yml`

优点：启动简单。

缺点：

1. 明文密码风险高。
2. 容易误提交到仓库。
3. 与当前项目已有环境变量模式不一致。

### 方案 C：只接通数据库，不初始化 SQL

优点：改动最小。

缺点：不满足“连通并初始化”的目标，项目即使能连库，也可能因缺表或缺数据无法正常验证。

结论：选择方案 A。

## 6. 执行设计

### 6.1 配置层

保持现有配置文件结构不变，优先使用本机环境变量覆盖默认值：

- `DB_URL=jdbc:mysql://localhost:3306/system?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true`
- `DB_USERNAME=root`
- `DB_PASSWORD=<本机密码>`

如果用户本机未显式设置 `DB_URL` 和 `DB_USERNAME`，理论上可依赖默认值；但为了降低环境差异，执行时仍应显式设置。

### 6.2 初始化层

连接本机 MySQL 后，按顺序执行：

1. `sql/schema/数据库建表SQL.sql`
2. `sql/seed/测试数据插入SQL.sql`

预期结果：

1. `system` 库被重新创建。
2. 基础表结构存在。
3. 种子数据存在，例如管理员、部门、角色和演示数据。

### 6.3 启动验证层

初始化完成后，启动项目并验证：

1. 数据源初始化成功。
2. 后端应用成功启动。
3. 不出现 MySQL 认证失败、数据库不存在、表不存在等错误。

如果启动时出现 Redis 连接错误，则单独作为环境依赖问题继续处理，不把 MySQL 接入结果与 Redis 问题混为一谈。

## 7. 验证标准

本次工作的完成证据应至少包括：

1. 本机 MySQL 可使用目标账号连接。
2. `system` 数据库存在。
3. 关键表存在，例如：
   - `department`
   - `role`
   - `user`
   - `device`
4. 基础种子数据存在，例如管理员账号和基础部门角色数据。
5. Spring Boot 在 `dev` 环境下成功启动并连上 MySQL。

## 8. 风险与处理

### 8.1 删库风险

建表脚本会删除并重建 `system` 库。本次已明确接受该风险，因此执行时无需额外保留旧库内容。

### 8.2 凭据风险

密码不落盘到仓库配置文件，避免本地明文密码被误提交。

### 8.3 外部依赖混淆风险

即使 MySQL 接入成功，应用启动仍可能受到 Redis 等其他依赖影响。出现此类情况时，应明确区分：

- MySQL 是否已接通并初始化成功
- 其他依赖是否仍需补充配置

## 9. 最小改动原则

本次优先使用现有配置和现有 SQL，不新增新的 profile、脚本或自定义初始化流程。只有在现有路径被实测证明不可用时，才考虑补充最小必要改动。
