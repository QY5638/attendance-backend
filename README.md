# attendance-backend

考勤异常检测与预警系统后端单仓库，基于 Spring Boot 提供认证、安全、设备管理、数据访问与异常分析相关能力。本文档面向本地启动和最小化校验场景，内容与当前 `pom.xml`、`application.yml`、`application-local.example.yml` 以及 `.github/workflows/backend-ci.yml` 保持一致。

## 项目简介

该仓库是后端独立工程，默认应用名为 `attendance-backend`。启动时默认激活 `dev` profile，因此会参与加载 `src/main/resources/application-dev.yml`；同时 `application-dev.yml` 还会通过 `spring.config.import` 额外读取仓库根目录下的 `application-local.yml` 作为本地环境配置，让本地值覆盖 `dev` 默认值。

## 技术栈

- Java 8
- Spring Boot 2.7.18
- Spring Web
- Spring Security
- Spring Validation
- Spring Data Redis
- MyBatis-Plus 3.5.7
- MySQL 驱动（运行时）
- Maven
- H2（仅测试依赖）
- Lombok

## 环境要求

本地启动前请准备以下运行环境：

- JDK 8
- Maven 3.x，并确保 `mvn -v` 使用的是 Java 8
- 可访问的 MySQL 实例
- 可访问的 Redis 实例

如果本地还要验证依赖外部能力的接口，还需要准备：

- LLM 服务提供方的地址、模型名和 API Key
- 地图服务提供方的地址和 API Key

## 本地配置

### 配置文件叠加关系

- `src/main/resources/application.yml` 提供基础配置，并声明默认激活 `dev` profile。
- 因为默认 profile 是 `dev`，所以 `src/main/resources/application-dev.yml` 会参与配置叠加；当前工程中的数据源、Redis 和 `app.env` 默认值都在这个文件里。
- `src/main/resources/application-dev.yml` 还通过 `spring.config.import=optional:file:${user.dir}/application-local.yml` 从当前启动目录加载本地配置，因此仓库根目录 `application-local.yml` 会在 `dev` 默认值之后生效。
- 排障时不要只看 `application-local.yml`；至少要同时检查 `application.yml`、`application-dev.yml`、仓库根目录 `application-local.yml` 这三处配置来源。

### `application-local.yml` 与示例文件的关系

- 因为导入路径是 `${user.dir}/application-local.yml`，文件应放在当前启动目录；本仓库的推荐方式仍然是放在仓库根目录，与 `pom.xml` 同级，而不是放进 `src/main/resources`。
- `application-local.example.yml` 只是模板，不会被应用自动读取；实际启动前请复制一份并命名为 `application-local.yml`。
- 如果 `application-local.yml` 中没有覆盖对应键，应用仍会继续使用 `application-dev.yml` 中的默认值或环境变量占位表达式，因此本地问题排查必须结合两边一起看。

可直接在仓库根目录执行：

```bash
cp application-local.example.yml application-local.yml
```

Windows PowerShell 可执行：

```powershell
Copy-Item application-local.example.yml application-local.yml
```

### 必填项

启动应用前，至少需要补齐以下配置：

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`
- `spring.redis.host`
- `spring.redis.port`
- `spring.redis.password`（Redis 开了密码时填写真实值；如果本地 Redis 无密码，必须在 `application-local.yml` 中显式将 `spring.redis.password` 设为 `~`（YAML null）或其他等价空值，不能通过删除该键回落到 `application-dev.yml` 里的默认 `${REDIS_PASSWORD:change_me}`）

如果要验证智能分析或地图相关功能，还需要补齐：

- `app.llm.provider`
- `app.llm.base-url`
- `app.llm.model`
- `app.llm.api-key`
- `app.map.provider`
- `app.map.base-url`
- `app.map.api-key`

示例模板中的值全部是占位符，不能直接用于启动。请只在本地 `application-local.yml` 中填写真实连接信息和密钥，不要提交真实密钥。

## 启动步骤

### 1. 准备数据库

- 仓库已提供数据库初始化脚本入口：
  - `sql/schema/数据库建表SQL.sql`
  - `sql/seed/测试数据插入SQL.sql`
- 建表脚本会先执行 `DROP DATABASE IF EXISTS system`，然后创建并切换到 `system` 数据库；如果你按仓库脚本原样导入，请把 `application-local.yml` 中的 `spring.datasource.url` 或环境变量 `DB_URL` 同步改成指向 `system`，不要继续使用 `application-dev.yml` 中默认回退的 `attendance_demo` 库名。
- 使用 MySQL 客户端按顺序执行以下两步：

```bash
mysql -h <db-host> -P <db-port> -u <db-username> -p < "sql/schema/数据库建表SQL.sql"
mysql -h <db-host> -P <db-port> -u <db-username> -p < "sql/seed/测试数据插入SQL.sql"
```

- 执行顺序不能反：`sql/seed/测试数据插入SQL.sql` 依赖 `sql/schema/数据库建表SQL.sql` 已经成功执行。
- 确保 `spring.datasource.username` 对应账号具备建库、建表、插入测试数据以及后续读写权限。

### 2. 准备 Redis

- 启动 Redis 服务。
- 将 `application-local.yml` 中的主机、端口、密码、库号改成你的本地环境值。

### 3. 创建本地配置文件

- 复制 `application-local.example.yml` 为 `application-local.yml`。
- 填写数据库、Redis，以及你要实际使用的 LLM/地图配置。
- 如果你直接导入了仓库里的 SQL 脚本，请确认 `spring.datasource.url` 最终连接的是 `system` 数据库，而不是 `application-dev.yml` 默认回退值中的 `attendance_demo`。

### 4. 启动应用

在仓库根目录执行：

```bash
mvn spring-boot:run
```

说明：

- 默认激活的 profile 是 `dev`，所以 `src/main/resources/application-dev.yml` 会参与配置叠加。
- 仓库根目录 `application-local.yml` 也会在启动时被导入；定位配置问题时请同时核对 `application.yml`、`application-dev.yml`、`application-local.yml`。
- 默认端口是 `8080`。
- 请从仓库根目录启动，这样 `user.dir` 会指向仓库根目录，`application-local.yml` 才能被正确读取。

## 测试与编译

当前 GitHub Actions 的后端 CI 只执行两步最小校验，README 建议本地也按同样方式执行：

### 运行最小测试集

```bash
mvn "-Dtest=DeviceManagementIntegrationTest,AuthSecurityIntegrationTest,ModuleSkeletonBeansTest" test
```

### 运行编译校验

```bash
mvn -DskipTests compile
```

这两条命令分别对应 `.github/workflows/backend-ci.yml` 中的“运行后端最小测试集”和“运行编译校验”。

## 常见问题

### 1. MySQL 配置缺失或错误

典型现象：

- 应用启动阶段直接失败。
- 日志中常见 `Failed to configure a DataSource`、`Communications link failure`、`Access denied for user`、`Unknown database` 一类报错。

排查方向：

- 检查 `spring.datasource.url` 是否指向正确主机、端口和库名。
- 检查账号密码是否正确。
- 检查 MySQL 服务是否已启动，且本机网络可达。

### 2. Redis 配置缺失或错误

典型现象：

- 启动阶段或首次访问依赖 Redis 的接口时失败。
- 日志中常见 `Unable to connect to Redis`、`RedisConnectionFailureException` 一类报错。

排查方向：

- 检查 `spring.redis.host`、`spring.redis.port`、`spring.redis.password`。
- 检查 Redis 服务是否已启动，密码和数据库编号是否匹配。

### 3. LLM 配置缺失或错误

典型现象：

- 应用能启动，但调用依赖大模型的功能时返回 4xx/5xx。
- 日志中常见上游鉴权失败、模型不存在、请求超时或连接失败等报错。

排查方向：

- 检查 `app.llm.provider`、`app.llm.base-url`、`app.llm.model`、`app.llm.api-key` 是否完整且与服务商要求一致。
- 检查本机是否可以访问对应 LLM 服务。

### 4. 地图配置缺失或错误

典型现象：

- 应用能启动，但调用位置解析、轨迹相关或地理围栏类功能时失败。
- 日志中常见第三方地图服务鉴权失败、请求超时或返回空结果。

排查方向：

- 检查 `app.map.provider`、`app.map.base-url`、`app.map.api-key`。
- 检查地图服务账号额度、白名单或来源限制。

## 目录内快速操作

```bash
# 启动应用
mvn spring-boot:run

# 运行与 CI 对齐的最小测试集
mvn "-Dtest=DeviceManagementIntegrationTest,AuthSecurityIntegrationTest,ModuleSkeletonBeansTest" test

# 编译校验
mvn -DskipTests compile
```
