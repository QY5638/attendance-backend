# Phase 5 本地配置加载优先级修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 `application-local.yml` 未能覆盖 `application-dev.yml` 默认占位值的问题，使当前 worktree 的本地私有配置真正参与真实启动，并解除 `change_me:6379` 导致的登录阻塞。

**Architecture:** 不改登录业务逻辑，先用一个专门的 dev 配置加载测试锁定“根目录 `application-local.yml` 必须覆盖 dev 默认值”，然后将本地私有配置的导入点从 `application.yml` 调整到 `application-dev.yml`，使其在 dev 默认值之后以更高优先级生效。若 README 口径因此变化，再做最小同步。

**Tech Stack:** Spring Boot, JUnit 5, Maven, YAML

---

### Task 1: 写失败测试锁定本地配置必须覆盖 dev 默认值

**Files:**
- Create: `src/test/java/com/quyong/attendance/DevProfileLocalConfigOverrideTest.java`
- Test: `src/test/java/com/quyong/attendance/DevProfileLocalConfigOverrideTest.java`

- [ ] **Step 1: 新增专门的 dev 配置加载测试**

````java
package com.quyong.attendance;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DevProfileLocalConfigOverrideTest {

    private static final Path LOCAL_CONFIG = Paths.get("application-local.yml");

    @AfterEach
    void cleanUp() throws Exception {
        Files.deleteIfExists(LOCAL_CONFIG);
    }

    @Test
    void shouldPreferRootApplicationLocalOverridesOverDevDefaults() throws Exception {
        Files.write(
                LOCAL_CONFIG,
                (
                        "spring:\n" +
                        "  datasource:\n" +
                        "    url: jdbc:h2:mem:local-config;MODE=MySQL;NON_KEYWORDS=USER,ROLE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false\n" +
                        "    username: sa\n" +
                        "    password: \n" +
                        "    driver-class-name: org.h2.Driver\n" +
                        "  redis:\n" +
                        "    host: 127.0.0.1\n" +
                        "    port: 6379\n" +
                        "    password: \n"
                ).getBytes(StandardCharsets.UTF_8)
        );

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(AttendanceApplication.class)
                .profiles("dev")
                .properties(
                        "spring.main.web-application-type=none",
                        "app.llm.provider=",
                        "app.map.provider=",
                        "spring.datasource.hikari.initialization-fail-timeout=0"
                )
                .web(WebApplicationType.NONE)
                .run()) {
            assertEquals("127.0.0.1", context.getEnvironment().getProperty("spring.redis.host"));
            assertEquals("sa", context.getEnvironment().getProperty("spring.datasource.username"));
            assertNotEquals("change_me", context.getEnvironment().getProperty("spring.redis.host"));
            assertNotEquals("change_me", context.getEnvironment().getProperty("spring.datasource.username"));
        }
    }
}
````

- [ ] **Step 2: 运行测试确认它先失败**

Run: `mvn -Dtest=DevProfileLocalConfigOverrideTest#shouldPreferRootApplicationLocalOverridesOverDevDefaults test`

Expected: FAIL。当前实现下，测试会因为应用仍然读取到 `application-dev.yml` 的 `change_me` 默认值而无法满足断言，或在启动阶段因未正确覆盖配置而失败。

### Task 2: 用最小配置调整修复覆盖顺序

**Files:**
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/resources/application-dev.yml`
- Modify: `README.md`
- Test: `src/test/java/com/quyong/attendance/DevProfileLocalConfigOverrideTest.java`

- [ ] **Step 1: 从 `application.yml` 移除当前的根目录本地配置 import**

````yaml
spring:
  application:
    name: attendance-backend
  profiles:
    active: dev
  jackson:
    default-property-inclusion: non_null
````

- [ ] **Step 2: 把根目录 `application-local.yml` 的导入移到 `application-dev.yml`**

````yaml
spring:
  config:
    import: optional:file:./application-local.yml
  datasource:
    url: ${DB_URL:jdbc:mysql://127.0.0.1:3306/attendance_demo?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true}
    username: ${DB_USERNAME:change_me}
    password: ${DB_PASSWORD:change_me}
    driver-class-name: ${DB_DRIVER:com.mysql.cj.jdbc.Driver}
    hikari:
      initialization-fail-timeout: 0
      connection-timeout: 3000
      validation-timeout: 1000
  redis:
    host: ${REDIS_HOST:change_me}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:change_me}
    database: ${REDIS_DATABASE:0}
    timeout: ${REDIS_TIMEOUT:3000ms}

app:
  env: dev
````

- [ ] **Step 3: 把 README 的配置来源说明同步到新事实**

````markdown
- `application.yml` 负责默认激活 `dev` profile。
- `application-dev.yml` 负责 dev 默认配置，并通过 `spring.config.import=optional:file:./application-local.yml` 继续读取仓库根目录私有配置。
- 排障时至少同时检查：`application.yml`、`application-dev.yml`、仓库根目录 `application-local.yml`。
````

- [ ] **Step 4: 运行配置加载测试确认转绿**

Run: `mvn -Dtest=DevProfileLocalConfigOverrideTest test`

Expected: PASS。测试能证明根目录 `application-local.yml` 已成功覆盖 dev 默认值，不再落回 `change_me`。

### Task 3: 用真实后端重启与真实登录验证配置阻塞解除

**Files:**
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/resources/application-dev.yml`
- Modify: `README.md`
- Test: `src/test/java/com/quyong/attendance/DevProfileLocalConfigOverrideTest.java`

- [ ] **Step 1: 重启当前 worktree 的后端服务**

```powershell
$procId = (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty OwningProcess)
if ($procId) { Stop-Process -Id $procId -Force }

$process = Start-Process -FilePath "cmd.exe" -ArgumentList '/c','mvn spring-boot:run > backend-run.log 2>&1' -WorkingDirectory "D:\Graduation project\backend\.worktrees\phase5-docs-runbook" -WindowStyle Hidden -PassThru
$process.Id
```

- [ ] **Step 2: 轮询健康检查，确认新配置下服务可启动**

```powershell
$ok = $false
for ($i = 0; $i -lt 30; $i++) {
  try {
    $resp = Invoke-WebRequest -UseBasicParsing 'http://127.0.0.1:8080/api/health' -TimeoutSec 2
    if ($resp.StatusCode -eq 200) {
      $ok = $true
      $resp.Content
      break
    }
  } catch {}
  Start-Sleep -Seconds 2
}
if (-not $ok) { throw 'BACKEND_HEALTH_TIMEOUT' }
```

Expected: 返回 `{"code":200,"message":"success","data":"ok"}`。

- [ ] **Step 3: 运行真实登录请求，确认不再落到 `change_me:6379`**

```powershell
$body = @{ username = 'admin'; password = '123456' } | ConvertTo-Json
$resp = Invoke-RestMethod -UseBasicParsing -Method Post -Uri 'http://127.0.0.1:8080/api/auth/login' -ContentType 'application/json' -Body $body
$resp | ConvertTo-Json -Compress
```

Expected: 不再返回因为 `change_me:6379` 导致的 `500`。理想结果是 `code=200`，并带 `data.token`、`data.roleCode=ADMIN`、`data.realName=系统管理员`。

- [ ] **Step 4: 若真实登录仍失败，只记录新根因，不扩散修复范围**

Expected: 至少要证明“配置加载优先级阻塞已解除”。若还有新失败，记录新的真实异常栈并停止在此，不顺手继续追别的根因。
