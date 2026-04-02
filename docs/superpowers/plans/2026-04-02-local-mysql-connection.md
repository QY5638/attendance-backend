# 本机 MySQL 接入与初始化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让后端项目在本机 `dev` 环境下连接 `localhost:3306/system` MySQL，并完成删库重建、建表、种子数据导入和应用启动验证。

**Architecture:** 保持项目现有 `application-dev.yml` 的环境变量接入方式，不把密码写入仓库。先在当前 Windows 用户环境和当前 PowerShell 会话中设置 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`，再通过本机已存在的 `mysql.exe` 执行现有 schema 与 seed 脚本，最后启动 Spring Boot 并通过 `/api/health` 验证应用确实已连上本机 MySQL。

**Tech Stack:** Spring Boot 2.7、MySQL 8.4 CLI、PowerShell 5.1、Maven

> 当前用户要求：本计划执行阶段不包含 `git commit`、`git push`、创建 PR 或其他 Git 集成动作，除非后续再次明确要求。

---

## 文件结构与职责

- Read: `src/main/resources/application.yml`
  - 确认默认激活 `dev` profile。
- Read: `src/main/resources/application-dev.yml`
  - 确认数据源来自 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`，默认目标是 `localhost:3306/system`。
- Read: `sql/schema/数据库建表SQL.sql`
  - 用于删库重建 `system` 并创建全部表结构与基础数据。
- Read: `sql/seed/测试数据插入SQL.sql`
  - 用于导入演示和验证所需种子数据。
- Read: `src/main/java/com/quyong/attendance/module/health/controller/HealthController.java`
  - 确认启动后可通过 `/api/health` 做无鉴权联通验证。
- Modify: 无预期仓库文件改动
  - 本计划优先复用现有配置和 SQL，执行内容主要发生在本机环境变量和本机 MySQL 中。
- External: `HKCU\Environment`
  - 持久化 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 三个用户级环境变量。
- External: `D:\MySQL\mysql-8.4.3-winx64\bin\mysql.exe`
  - 本机已存在的 MySQL CLI，用于连库和执行 SQL。

### Task 1: 配置并验证本机 MySQL 连接环境

**Files:**
- Read: `src/main/resources/application-dev.yml`
- External: `HKCU\Environment`
- External: `D:\MySQL\mysql-8.4.3-winx64\bin\mysql.exe`

- [ ] **Step 1: 在当前 PowerShell 会话和当前用户环境中设置数据库变量**

Run:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/system?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
$env:DB_USERNAME = 'root'
$securePassword = Read-Host '请输入 MySQL root 密码' -AsSecureString
$ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
try {
    $plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
}
$env:DB_PASSWORD = $plainPassword
[Environment]::SetEnvironmentVariable('DB_URL', $env:DB_URL, 'User')
[Environment]::SetEnvironmentVariable('DB_USERNAME', $env:DB_USERNAME, 'User')
[Environment]::SetEnvironmentVariable('DB_PASSWORD', $env:DB_PASSWORD, 'User')
Remove-Variable securePassword, ptr, plainPassword
```

Expected:
- 当前 PowerShell 会话存在 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`
- 当前 Windows 用户环境已持久化同名变量
- 未修改任何仓库配置文件

- [ ] **Step 2: 用 MySQL CLI 验证 root 账号可连接本机服务**

Run:

```powershell
& 'D:\MySQL\mysql-8.4.3-winx64\bin\mysql.exe' --host=localhost --port=3306 --protocol=TCP --user=$env:DB_USERNAME --password=$env:DB_PASSWORD --default-character-set=utf8mb4 --execute="SELECT VERSION() AS mysql_version;"
```

Expected:
- 命令退出码为 0
- 输出至少一行 `mysql_version`
- 不能出现 `Access denied`、`Can't connect to MySQL server` 或 `Unknown database`

- [ ] **Step 3: 记录当前会话实际使用的连接目标，确保后续初始化不会跑错库**

Run:

```powershell
"DB_URL=$env:DB_URL"
"DB_USERNAME=$env:DB_USERNAME"
```

Expected:
- 输出目标为 `localhost:3306/system`
- 用户名为 `root`

### Task 2: 重建 `system` 库并导入现有 SQL

**Files:**
- Read: `sql/schema/数据库建表SQL.sql`
- Read: `sql/seed/测试数据插入SQL.sql`
- External: `D:\MySQL\mysql-8.4.3-winx64\bin\mysql.exe`

- [ ] **Step 1: 执行建表脚本，删库并重建 `system`**

Run:

```powershell
Get-Content -Raw -Encoding UTF8 'D:\Graduation project\backend\sql\schema\数据库建表SQL.sql' | & 'D:\MySQL\mysql-8.4.3-winx64\bin\mysql.exe' --host=localhost --port=3306 --protocol=TCP --user=$env:DB_USERNAME --password=$env:DB_PASSWORD --default-character-set=utf8mb4
```

Expected:
- 命令退出码为 0
- 不出现语法错误、权限错误或字符集错误
- `system` 库被重建

- [ ] **Step 2: 执行种子脚本，导入基础演示数据**

Run:

```powershell
Get-Content -Raw -Encoding UTF8 'D:\Graduation project\backend\sql\seed\测试数据插入SQL.sql' | & 'D:\MySQL\mysql-8.4.3-winx64\bin\mysql.exe' --host=localhost --port=3306 --protocol=TCP --user=$env:DB_USERNAME --password=$env:DB_PASSWORD --default-character-set=utf8mb4
```

Expected:
- 命令退出码为 0
- 不出现外键依赖错误
- `admin`、`zhangsan`、`lisi` 等测试数据导入成功

- [ ] **Step 3: 验证关键表和关键数据数量**

Run:

```powershell
& 'D:\MySQL\mysql-8.4.3-winx64\bin\mysql.exe' --host=localhost --port=3306 --protocol=TCP --user=$env:DB_USERNAME --password=$env:DB_PASSWORD --database=system --default-character-set=utf8mb4 --execute='SHOW TABLES; SELECT COUNT(*) AS department_count FROM department; SELECT COUNT(*) AS role_count FROM `role`; SELECT COUNT(*) AS user_count FROM `user`; SELECT COUNT(*) AS device_count FROM device; SELECT COUNT(*) AS rule_count FROM rule;'
```

Expected:
- `SHOW TABLES` 至少包含 `department`、`role`、`user`、`device`、`rule`
- `department_count = 3`
- `role_count = 2`
- `user_count = 3`
- `device_count = 3`
- `rule_count = 1`

### Task 3: 启动后端并验证已连上本机 MySQL

**Files:**
- Read: `src/main/resources/application.yml`
- Read: `src/main/resources/application-dev.yml`
- Read: `src/main/java/com/quyong/attendance/module/health/controller/HealthController.java`

- [ ] **Step 1: 在已设置环境变量的 PowerShell 会话中启动后端**

Run:

```powershell
mvn "-DskipTests" spring-boot:run
```

Expected:
- Maven 开始启动 Spring Boot
- 日志中不出现 MySQL 认证失败、数据库不存在、表不存在、驱动缺失等错误
- 日志最终出现 `Started AttendanceApplication` 或等价的启动完成信息
- 当前终端保持运行，不要关闭，用于承载后端进程

- [ ] **Step 2: 在第二个 PowerShell 窗口中调用健康检查接口验证联通性**

Run:

```powershell
Invoke-RestMethod -Uri 'http://localhost:8080/api/health' -Method Get
```

Expected:
- 返回对象中 `code = 200`
- 返回对象中 `message = success`
- 返回对象中 `data = ok`

- [ ] **Step 3: 若启动失败，区分 MySQL 问题与其他外部依赖问题**

Expected:
- 如果失败日志指向 MySQL，记录具体错误并停在当前任务，不要盲改配置
- 如果 MySQL 已正常初始化，但失败日志指向 Redis 等其他依赖，记录为“本机 MySQL 接入已完成，仍存在其他环境依赖待处理”
- 不要把 Redis 问题误判成 MySQL 未接通

### Task 4: 汇总本机接入结果

**Files:**
- Read: `src/main/resources/application-dev.yml`
- Read: `sql/schema/数据库建表SQL.sql`
- Read: `sql/seed/测试数据插入SQL.sql`

- [ ] **Step 1: 汇总本次实际生效的连接方式与初始化结果**

Expected:
- 明确使用的是环境变量方案，而不是把密码写入仓库
- 明确 `system` 库已删库重建并导入数据
- 明确关键表与关键数据校验结果

- [ ] **Step 2: 给出后续环境建议**

Expected:
- 若一切通过，说明后续可以继续基于本机 MySQL 做联调或新模块开发
- 若只剩 Redis 等其他依赖问题，明确下一步应转向对应依赖排查，而不是重复处理 MySQL
