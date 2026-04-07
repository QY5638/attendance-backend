# 本机 MySQL seed 登录修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 只通过修正 `sql/seed/测试数据插入SQL.sql` 中的 3 个 seed 用户密码，让重新初始化后的本机 `system` 库支持 `admin/123456` 正常登录。

**Architecture:** 保持认证代码不变，继续沿用 `BCryptPasswordEncoder` 约束，只把 seed SQL 中 `admin`、`zhangsan`、`lisi` 的密码值替换成预生成的 `BCrypt` 哈希。验证不依赖 `mvn test`，而是基于真实本机 MySQL 做 `schema + seed` 重建，再通过 `/api/health` 和 `/api/auth/login` 做运行态烟测。

**Tech Stack:** Spring Security BCrypt、MySQL 8.4 CLI、PowerShell 5.1、Spring Boot 2.7

> 当前用户要求：本计划执行阶段不包含 `git commit`、`git push`、创建 PR；只修 seed 登录，不顺带修改认证代码、测试代码、文档或测试文档。

---

## 文件结构与职责

- Modify: `sql/seed/测试数据插入SQL.sql:10-13`
  - 仅替换 `admin`、`zhangsan`、`lisi` 三个 seed 用户的 `password` 字段。
- Read: `src/main/java/com/quyong/attendance/config/SecurityConfig.java:20-23`
  - 确认当前密码编码器是 `BCryptPasswordEncoder`。
- Read: `src/main/java/com/quyong/attendance/module/auth/service/impl/AuthServiceImpl.java:42-45`
  - 确认登录使用 `passwordEncoder.matches(...)` 校验密码。
- Read: `src/test/java/com/quyong/attendance/AuthSecurityIntegrationTest.java:86-95`
  - 作为登录成功返回结构的现有约束参考，确认返回包含 `token`、`roleCode`、`realName`。
- Read: `sql/schema/数据库建表SQL.sql`
  - 用于删库重建本机 `system` 库。
- External: `HKCU\Environment`
  - 复用已存在的 `DB_USERNAME`、`DB_PASSWORD` 用户环境变量。
- External: `D:\MySQL\mysql-8.4.3-winx64\bin\mysql.exe`
  - 用于重建数据库并校验数据。
- External: `http://localhost:8080`
  - 用于健康检查和登录烟测。

### Task 1: 仅替换 seed 用户密码为 BCrypt 哈希

**Files:**
- Modify: `sql/seed/测试数据插入SQL.sql:10-13`
- Read: `src/main/java/com/quyong/attendance/config/SecurityConfig.java:20-23`
- Read: `src/main/java/com/quyong/attendance/module/auth/service/impl/AuthServiceImpl.java:42-45`

- [ ] **Step 1: 把 3 个 seed 用户密码替换成预生成的 BCrypt 哈希**

将 `sql/seed/测试数据插入SQL.sql` 的用户插入片段改成：

```sql
INSERT IGNORE INTO `user` (`id`, `username`, `password`, `realName`, `gender`, `phone`, `deptId`, `roleId`, `status`, `createTime`) VALUES
(9001, 'admin', '$2a$10$DII2rUub7WSmcTFOa/4AtumHq9r3yDGwQ4gHW1pvyx51.dE.Abliu', '系统管理员', '男', '13800000001', 1, 1, 1, '2026-03-01 08:00:00'),
(1001, 'zhangsan', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '张三', '男', '13800000002', 2, 2, 1, '2026-03-01 08:10:00'),
(1002, 'lisi', '$2a$10$Cw71Sz28BSmh1fcOBJIAXOagYeMZjJRl6UEU4n8kQMGESv3RgL0SC', '李四', '女', '13800000003', 3, 2, 1, '2026-03-01 08:20:00');
```

Expected:
- 只改这 3 个 `password` 字段
- 明文口令语义仍是 `123456`
- 认证代码和其他 seed 数据不变

- [ ] **Step 2: 检查 diff，确认没有超范围修改**

Run:

```powershell
git diff -- sql/seed/测试数据插入SQL.sql
```

Expected:
- diff 仅出现在 `sql/seed/测试数据插入SQL.sql`
- 仅看到 3 个密码值由 `123456` 变为上述 `BCrypt` 哈希
- 不能出现其他表、其他字段或文档改动

### Task 2: 基于修正后的 seed 重新初始化本机 `system` 库

**Files:**
- Read: `sql/schema/数据库建表SQL.sql`
- Read: `sql/seed/测试数据插入SQL.sql`
- External: `HKCU\Environment`
- External: `D:\MySQL\mysql-8.4.3-winx64\bin\mysql.exe`

- [ ] **Step 1: 读取用户环境变量，确认执行重建所需凭据存在**

Run:

```powershell
$dbUser = [Environment]::GetEnvironmentVariable('DB_USERNAME', 'User')
$dbPassword = [Environment]::GetEnvironmentVariable('DB_PASSWORD', 'User')
if ([string]::IsNullOrWhiteSpace($dbUser) -or [string]::IsNullOrWhiteSpace($dbPassword)) {
    throw '缺少 DB_USERNAME 或 DB_PASSWORD 用户环境变量'
}
"DB_USERNAME=$dbUser"
'DB_PASSWORD_SET=True'
```

Expected:
- 输出 `DB_USERNAME=root`
- 输出 `DB_PASSWORD_SET=True`
- 不回显明文密码

- [ ] **Step 2: 用原始文件重定向方式执行 schema，删库并重建 `system`**

Run:

```powershell
$dbUser = [Environment]::GetEnvironmentVariable('DB_USERNAME', 'User')
$dbPassword = [Environment]::GetEnvironmentVariable('DB_PASSWORD', 'User')
cmd /c "\"D:\MySQL\mysql-8.4.3-winx64\bin\mysql.exe\" --host=localhost --port=3306 --protocol=TCP --user=$dbUser --password=$dbPassword --default-character-set=utf8mb4 < \"D:\Graduation project\backend\.worktrees\local-mysql-connection\sql\schema\数据库建表SQL.sql\""
```

Expected:
- 命令退出码为 0
- 不出现语法错误、权限错误或字符集错误
- `system` 库被删除后重建

- [ ] **Step 3: 用同样方式执行修正后的 seed SQL**

Run:

```powershell
$dbUser = [Environment]::GetEnvironmentVariable('DB_USERNAME', 'User')
$dbPassword = [Environment]::GetEnvironmentVariable('DB_PASSWORD', 'User')
cmd /c "\"D:\MySQL\mysql-8.4.3-winx64\bin\mysql.exe\" --host=localhost --port=3306 --protocol=TCP --user=$dbUser --password=$dbPassword --default-character-set=utf8mb4 < \"D:\Graduation project\backend\.worktrees\local-mysql-connection\sql\seed\测试数据插入SQL.sql\""
```

Expected:
- 命令退出码为 0
- 不出现外键错误或中文乱码问题
- `admin`、`zhangsan`、`lisi` 三个用户重新导入成功

- [ ] **Step 4: 查询 `user` 表，确认 3 个 seed 账号不再存明文密码**

Run:

```powershell
$dbUser = [Environment]::GetEnvironmentVariable('DB_USERNAME', 'User')
$dbPassword = [Environment]::GetEnvironmentVariable('DB_PASSWORD', 'User')
& 'D:\MySQL\mysql-8.4.3-winx64\bin\mysql.exe' --host=localhost --port=3306 --protocol=TCP --user=$dbUser --password=$dbPassword --database=system --default-character-set=utf8mb4 --execute='SELECT COUNT(*) AS plain_password_count FROM `user` WHERE username IN (''admin'',''zhangsan'',''lisi'') AND password = ''123456''; SELECT username, password FROM `user` WHERE username IN (''admin'',''zhangsan'',''lisi'') ORDER BY id;'
```

Expected:
- `plain_password_count = 0`
- 返回 3 行用户密码数据
- `admin`、`zhangsan`、`lisi` 的密码值分别等于：

```text
admin    $2a$10$DII2rUub7WSmcTFOa/4AtumHq9r3yDGwQ4gHW1pvyx51.dE.Abliu
zhangsan $2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm
lisi     $2a$10$Cw71Sz28BSmh1fcOBJIAXOagYeMZjJRl6UEU4n8kQMGESv3RgL0SC
```

### Task 3: 启动应用并验证 seed 账号登录成功

**Files:**
- Read: `src/main/java/com/quyong/attendance/module/auth/controller/AuthController.java:24-27`
- Read: `src/main/java/com/quyong/attendance/module/auth/vo/LoginVO.java:5-30`
- Read: `src/test/java/com/quyong/attendance/AuthSecurityIntegrationTest.java:86-95`

- [ ] **Step 1: 如 8080 已被当前 worktree 旧进程占用，先安全停止旧进程**

Run:

```powershell
$listener = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if ($listener) {
    $proc = Get-CimInstance Win32_Process -Filter "ProcessId = $($listener.OwningProcess)"
    if ($proc.CommandLine -like '*local-mysql-connection*') {
        Stop-Process -Id $listener.OwningProcess
    } else {
        throw '8080 已被非当前任务进程占用，请先人工确认'
    }
}
```

Expected:
- 若端口空闲，命令直接结束
- 若端口被当前 worktree 的旧 Spring Boot 进程占用，该进程被停止
- 若端口被无关进程占用，明确失败并停止继续执行

- [ ] **Step 2: 后台启动 Spring Boot，并把日志写到固定文件**

Run:

```powershell
$stdout = "$env:TEMP\local-mysql-seed-fix.out.log"
$stderr = "$env:TEMP\local-mysql-seed-fix.err.log"
Remove-Item $stdout, $stderr -ErrorAction SilentlyContinue
Start-Process -FilePath 'mvn' -ArgumentList '-DskipTests','spring-boot:run' -WorkingDirectory 'D:\Graduation project\backend\.worktrees\local-mysql-connection' -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru | Select-Object -ExpandProperty Id
```

Expected:
- 输出一个新的后台进程 PID
- `stdout` / `stderr` 日志文件被创建

- [ ] **Step 3: 轮询启动日志，确认应用已成功启动且未出现 MySQL 错误**

Run:

```powershell
$stdout = "$env:TEMP\local-mysql-seed-fix.out.log"
$started = $false
1..60 | ForEach-Object {
    if (Select-String -Path $stdout -Pattern 'Started AttendanceApplication' -Quiet) {
        $started = $true
        break
    }
    Start-Sleep -Seconds 2
}
if (-not $started) {
    throw 'Spring Boot 未在 120 秒内启动成功'
}
```

Expected:
- 120 秒内匹配到 `Started AttendanceApplication`
- 日志中不出现 MySQL 认证失败、数据库不存在、表不存在、驱动缺失等错误

- [ ] **Step 4: 验证健康检查接口仍正常**

Run:

```powershell
Invoke-RestMethod -Uri 'http://localhost:8080/api/health' -Method Get | ConvertTo-Json -Compress
```

Expected:
- 返回结果包含 `"code":200`
- 返回结果包含 `"message":"success"`
- 返回结果包含 `"data":"ok"`

- [ ] **Step 5: 使用 seed 账号执行登录烟测，确认 `admin/123456` 现在可成功登录**

Run:

```powershell
$body = '{"username":"admin","password":"123456"}'
Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -ContentType 'application/json; charset=utf-8' -Body $body | ConvertTo-Json -Compress
```

Expected:
- 返回结果包含 `"code":200`
- 返回结果包含 `"message":"success"`
- 返回结果中 `data.token` 为非空字符串
- 返回结果中 `data.roleCode = "ADMIN"`
- 返回结果中 `data.realName = "系统管理员"`

### Task 4: 清理运行进程并汇总结果

**Files:**
- Read: `sql/seed/测试数据插入SQL.sql:10-13`

- [ ] **Step 1: 停止当前任务启动的后台 Spring Boot 进程**

Run:

```powershell
$listener = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if ($listener) {
    $proc = Get-CimInstance Win32_Process -Filter "ProcessId = $($listener.OwningProcess)"
    if ($proc.CommandLine -like '*local-mysql-connection*') {
        Stop-Process -Id $listener.OwningProcess
    }
}
```

Expected:
- 若 8080 监听进程属于当前 worktree，则被正常停止
- 不影响无关进程

- [ ] **Step 2: 汇总本轮结果并明确剩余边界**

Expected:
- 明确说明：本次只修了 `sql/seed/测试数据插入SQL.sql`
- 明确说明：全新执行 `schema + seed` 后，`admin/123456` 已可登录
- 明确说明：历史上已经导入过旧明文 seed 的库，若不重建数据库，本次不会自动修复
