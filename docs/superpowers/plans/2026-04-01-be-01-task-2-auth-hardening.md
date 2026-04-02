# BE-01 Task 2 鉴权收口 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 收口 `Task 2`，补齐登录鉴权链路的角色状态校验、服务端会话字段、安全默认策略与 `dev` 启动链路。

**Architecture:** 保持现有 `Spring Security + BearerTokenAuthenticationFilter + TokenStore` 方案不变，只做最小必要修复。测试环境继续使用内存 TokenStore 与 H2，运行环境恢复 datasource 自动配置，并通过配置降低数据库瞬时不可达时的启动失败风险。

**Tech Stack:** Spring Boot 2.7、Spring Security、MyBatis-Plus、Redis、H2、JUnit 5、MockMvc

---

### Task 1: 先补会失败的鉴权与启动测试

**Files:**
- Modify: `src/test/java/com/quyong/attendance/AuthSecurityIntegrationTest.java`
- Create: `src/test/java/com/quyong/attendance/DevProfileContextStartupTest.java`

- [ ] 为“禁用角色登录失败”补集成测试，并先运行该单测确认当前失败。
- [ ] 为“未知 `/api/**` 路径未登录返回 401”补集成测试，并先运行该单测确认当前失败。
- [ ] 为“登录后 TokenStore 中保存 `status` 和 `expireAt`”补集成测试，并先运行该单测确认当前失败。
- [ ] 为 `dev` profile 上下文启动补测试，使用测试内覆盖的 H2 datasource 属性验证当前失败。

### Task 2: 最小修改认证实现与安全配置

**Files:**
- Modify: `src/main/java/com/quyong/attendance/module/auth/model/AuthUser.java`
- Modify: `src/main/java/com/quyong/attendance/module/auth/service/impl/AuthServiceImpl.java`
- Modify: `src/main/java/com/quyong/attendance/module/auth/store/InMemoryTokenStore.java`
- Modify: `src/main/java/com/quyong/attendance/config/SecurityConfig.java`
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/resources/application-dev.yml`
- Modify: `src/main/resources/application-prod.yml`

- [ ] 给 `AuthUser` 增加 `status`、`expireAt` 字段及访问器，保持现有字段不变。
- [ ] 在登录逻辑中校验 `role.status == 1`，未启用时拒绝签发 token。
- [ ] 在签发 token 时把 `userId`、`username`、`realName`、`roleCode`、`status`、`expireAt` 一次性写入会话对象。
- [ ] 让 `InMemoryTokenStore` 继续按过期时间剔除 token，并复用会话对象里的 `expireAt`。
- [ ] 将 `SecurityConfig` 的默认策略从 `permitAll()` 收紧到 `authenticated()`，仅保留 `/api/health` 与 `/api/auth/login` 放行。
- [ ] 移除全局 datasource 自动配置排除项，并在 `dev/prod` 补齐 datasource 与 Hikari 启动容错配置。

### Task 3: 补回归测试并验证

**Files:**
- Modify: `src/test/java/com/quyong/attendance/AuthSecurityIntegrationTest.java`
- Modify: `docs/test/测试用例文档.md`

- [ ] 补“无效 token -> 401”“过期 token -> 401”“空用户名 -> 400”“空密码 -> 400”回归测试。
- [ ] 运行 `mvn -Dtest=AuthSecurityIntegrationTest,DevProfileContextStartupTest test` 并确认全部通过。
- [ ] 运行 `mvn spring-boot:run "-Dspring-boot.run.profiles=dev"`，确认应用可启动到 Web 容器初始化完成。
- [ ] 同步 `docs/test/测试用例文档.md` 中 Task 2 的新增覆盖点。
