# Phase 5 真实登录阻塞修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复真实环境 `POST /api/auth/login` 返回 `500` 的阻塞，使 Redis token 存储不再因为 `AuthUser` 的时间字段序列化而打断登录。

**Architecture:** 保持 `AuthServiceImpl`、控制器和测试环境 `InMemoryTokenStore` 不变，只在 `RedisTokenStore` 内把 token session 映射成稳定的简单 JSON 结构，再从该结构重建 `AuthUser`。先用现有 `RedisTokenStoreTest` 做 TDD 红绿，再用真实后端启动和真实登录请求做运行层验证。

**Tech Stack:** Spring Boot, Jackson, Redis, JUnit 5, Mockito, Maven

---

### Task 1: 用失败测试锁定 Redis token 存储的真实阻塞

**Files:**
- Modify: `src/test/java/com/quyong/attendance/module/auth/store/RedisTokenStoreTest.java`
- Test: `src/test/java/com/quyong/attendance/module/auth/store/RedisTokenStoreTest.java`

- [ ] **Step 1: 在现有测试文件中补一条最小失败测试**

````java
@Test
void shouldStoreAndRestoreTokenSessionWhenAuthUserContainsInstant() {
    Map<String, String> redisStorage = new HashMap<String, String>();
    when(valueOperations.get(anyString()))
            .thenAnswer(invocation -> redisStorage.get(invocation.getArgument(0, String.class)));
    doAnswer(invocation -> {
        redisStorage.put(invocation.getArgument(0, String.class), invocation.getArgument(1, String.class));
        return null;
    }).when(valueOperations).set(anyString(), anyString(), any(Duration.class));

    Instant expireAt = Instant.parse("2026-04-07T10:00:00Z");
    AuthUser authUser = new AuthUser(9001L, "admin", "系统管理员", "ADMIN", 1, expireAt);

    assertDoesNotThrow(() -> redisTokenStore.store("token-1", authUser, Duration.ofHours(12)));

    AuthUser restored = redisTokenStore.get("token-1");
    assertNotNull(restored);
    assertEquals(Long.valueOf(9001L), restored.getUserId());
    assertEquals("admin", restored.getUsername());
    assertEquals("系统管理员", restored.getRealName());
    assertEquals("ADMIN", restored.getRoleCode());
    assertEquals(Integer.valueOf(1), restored.getStatus());
    assertEquals(expireAt, restored.getExpireAt());
    assertTrue(redisStorage.get("auth:token:token-1").contains("expireAtEpochMilli"));
}
````

- [ ] **Step 2: 运行单条测试确认它先失败**

Run: `mvn -Dtest=RedisTokenStoreTest#shouldStoreAndRestoreTokenSessionWhenAuthUserContainsInstant test`

Expected: FAIL，当前实现会在 `redisTokenStore.store(...)` 抛出 `IllegalStateException: token 序列化失败` 或等价异常，而不是成功写入 Redis 字符串。

### Task 2: 用稳定 session 结构做最小实现

**Files:**
- Modify: `src/main/java/com/quyong/attendance/module/auth/store/RedisTokenStore.java`
- Modify: `src/test/java/com/quyong/attendance/module/auth/store/RedisTokenStoreTest.java`
- Test: `src/test/java/com/quyong/attendance/module/auth/store/RedisTokenStoreTest.java`

- [ ] **Step 1: 在 `RedisTokenStore` 内增加稳定 session 结构，不直接序列化 `AuthUser`**

````java
private static final class RedisAuthSession {
    private Long userId;
    private String username;
    private String realName;
    private String roleCode;
    private Integer status;
    private Long expireAtEpochMilli;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Long getExpireAtEpochMilli() { return expireAtEpochMilli; }
    public void setExpireAtEpochMilli(Long expireAtEpochMilli) { this.expireAtEpochMilli = expireAtEpochMilli; }
}
````

- [ ] **Step 2: 让 `store()` 把 `AuthUser` 转成 `RedisAuthSession` 再写入 Redis**

````java
@Override
public void store(String token, AuthUser authUser, Duration ttl) {
    try {
        stringRedisTemplate.opsForValue().set(
                buildKey(token),
                objectMapper.writeValueAsString(toSession(authUser)),
                ttl
        );
    } catch (JsonProcessingException exception) {
        throw new IllegalStateException("token 序列化失败", exception);
    }
}

private RedisAuthSession toSession(AuthUser authUser) {
    RedisAuthSession session = new RedisAuthSession();
    session.setUserId(authUser.getUserId());
    session.setUsername(authUser.getUsername());
    session.setRealName(authUser.getRealName());
    session.setRoleCode(authUser.getRoleCode());
    session.setStatus(authUser.getStatus());
    session.setExpireAtEpochMilli(authUser.getExpireAt() == null ? null : authUser.getExpireAt().toEpochMilli());
    return session;
}
````

- [ ] **Step 3: 让 `get()` 从 `RedisAuthSession` 重建 `AuthUser`**

````java
@Override
public AuthUser get(String token) {
    String key = buildKey(token);
    String value = stringRedisTemplate.opsForValue().get(key);
    if (value == null) {
        return null;
    }
    try {
        RedisAuthSession session = objectMapper.readValue(value, RedisAuthSession.class);
        return new AuthUser(
                session.getUserId(),
                session.getUsername(),
                session.getRealName(),
                session.getRoleCode(),
                session.getStatus(),
                session.getExpireAtEpochMilli() == null ? null : Instant.ofEpochMilli(session.getExpireAtEpochMilli())
        );
    } catch (IOException exception) {
        stringRedisTemplate.delete(key);
        return null;
    }
}
````

- [ ] **Step 4: 运行 Redis token store 测试确认转绿**

Run: `mvn -Dtest=RedisTokenStoreTest test`

Expected: PASS，至少包含：
- `shouldStoreAndRestoreTokenSessionWhenAuthUserContainsInstant`
- `shouldReturnNullWhenStoredTokenSessionIsCorrupted`

### Task 3: 用真实后端重启与真实登录验证阻塞解除

**Files:**
- Modify: `src/main/java/com/quyong/attendance/module/auth/store/RedisTokenStore.java`
- Test: `src/test/java/com/quyong/attendance/module/auth/store/RedisTokenStoreTest.java`

- [ ] **Step 1: 停掉当前后端进程并重启当前 worktree 服务**

```powershell
Get-CimInstance Win32_Process | Where-Object {
  $_.CommandLine -like '*attendance-backend*spring-boot:run*phase5-docs-runbook*'
} | ForEach-Object {
  Stop-Process -Id $_.ProcessId -Force
}

$process = Start-Process -FilePath "cmd.exe" -ArgumentList '/c','mvn spring-boot:run > backend-run.log 2>&1' -WorkingDirectory "D:\Graduation project\backend\.worktrees\phase5-docs-runbook" -WindowStyle Hidden -PassThru
$process.Id
```

- [ ] **Step 2: 轮询健康检查，确认服务重启成功**

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

Expected: 返回包含 `{"code":200,..."data":"ok"}` 的健康检查结果。

- [ ] **Step 3: 运行真实登录请求，确认不再返回 500**

```powershell
$body = @{ username = 'admin'; password = '123456' } | ConvertTo-Json
$resp = Invoke-RestMethod -UseBasicParsing -Method Post -Uri 'http://127.0.0.1:8080/api/auth/login' -ContentType 'application/json' -Body $body
$resp | ConvertTo-Json -Compress
```

Expected: 返回 `code=200`，且 `data.token` 为字符串、`data.roleCode=ADMIN`、`data.realName=系统管理员`。

- [ ] **Step 4: 记录结果并停止，不顺手扩展到其他模块**

Expected: 只确认登录阻塞已解除；真实 UI 验收与 Phase 5 收口回到上层任务继续推进。
