# Phase 5 真实登录阻塞修复设计

## 背景

在 `Phase 5` 真实启动与界面级验收过程中，后端已能按当前 `phase5-docs-runbook` worktree 私有配置成功启动，`/api/health` 返回 `200`，但真实登录请求：

`POST /api/auth/login`

稳定返回：

```json
{"code":500,"message":"server error"}
```

当前已经确认：

- `AuthController` 与 `AuthServiceImpl` 的用户查询、密码校验、角色查询都能走通。
- `operationLogService.save(...)` 自身会吞掉运行时异常，不是这次 `500` 的直接根因。
- 失败路径只出现在非 `test` 环境，因为测试环境使用 `InMemoryTokenStore`，真实环境使用 `RedisTokenStore`。
- 真实环境中，token 写入 Redis 前后没有可见成功证据，问题高度集中在 `RedisTokenStore.store(...)` 这条链路。

因此，这不是 Phase 5 文档问题，而是一个阻塞真实验收的后端真实环境 bug。

## 目标

修复真实环境登录阻塞，使以下条件同时成立：

1. `POST /api/auth/login` 在真实环境不再返回 `500`。
2. 登录成功后 token 可稳定写入与读取 Redis。
3. 修复方式不依赖本地 profile 绕过，不引入“只在本地能用”的特殊分支。
4. 保持测试环境 `InMemoryTokenStore` 不受影响。

## 根因假设

当前最可信的根因是：

- `RedisTokenStore` 直接使用通用 `ObjectMapper` 把 `AuthUser` 写成 JSON。
- `AuthUser` 含有 `Instant expireAt` 字段。
- 真实环境 token 存储路径可能因为时间类型序列化/反序列化或 Redis 写入格式不稳定而抛出运行时异常，最终被全局异常处理吞成 `server error`。

虽然还没有从运行日志拿到完整异常栈，但已有证据表明问题位于 `RedisTokenStore.store(...)` 这条真实环境专属链路，且测试环境不会覆盖它。

## 方案

### 推荐方案：为 Redis token 存储定义稳定的显式序列化格式

不再把整个 `AuthUser` 直接交给通用 `ObjectMapper` 序列化，而是在 `RedisTokenStore` 内把 token session 明确映射为稳定的字符串 JSON 结构，只保留：

- `userId`
- `username`
- `realName`
- `roleCode`
- `status`
- `expireAtEpochMilli` 或等价的稳定时间字符串

读取时再从这个稳定结构重建 `AuthUser`。

### 为什么不选“给 ObjectMapper 补 JavaTimeModule”

这是可行备选，但仍然把 Redis token 存储格式绑定在通用 Jackson 装配上，真实环境和测试环境更容易因模块、默认配置或时间格式变化产生漂移。显式稳定结构更适合 token session 这种跨环境基础设施数据。

### 为什么不选“本地绕过 Redis token 存储”

这只能绕过 `Phase 5` 当前阻塞，不能修掉真实环境路径本身的问题，会把真实环境 bug 留在系统里。

## 变更边界

本轮只允许最小修改以下后端文件：

- `src/main/java/com/quyong/attendance/module/auth/store/RedisTokenStore.java`
- `src/test/java/...` 中新增或补充覆盖 Redis token 存储真实格式的最小测试文件

必要时可增一个极小的内部 DTO/静态内部类用于稳定序列化，但不扩散到其他模块。

本轮不做：

- 修改 `AuthServiceImpl` 登录业务逻辑
- 修改 `InMemoryTokenStore`
- 修改前端登录逻辑
- 修改 Redis 配置策略
- 顺手重构鉴权模块

## 测试策略

严格按 TDD：

1. 先补一个最小失败测试，锁定 `RedisTokenStore.store(...)` 在带 `expireAt` 的 `AuthUser` 上必须成功完成写入。
2. 运行失败测试，确认当前实现会在这条路径失败。
3. 做最小实现，改成稳定序列化格式。
4. 再补或复用读取侧断言，确认 `store -> get` 能还原出正确的 `AuthUser`。
5. 通过后，重新执行真实登录请求，确认 `500` 消失，再继续 Phase 5 真实验收。

## 验证要求

修复完成后，至少要有两层验证：

1. 单元/集成测试层：覆盖 `RedisTokenStore` 的真实序列化路径。
2. 真实运行层：
   - 后端启动成功
   - `POST /api/auth/login` 返回 `200`
   - 返回 `token`、`roleCode=ADMIN`、`realName=系统管理员`

只有这两层都通过，才算这条阻塞真正解除。

## 风险与后续

- 如果失败根因最终不是时间类型序列化，而是 Redis 认证或 RedisTemplate 运行时行为，TDD 失败测试会把真实问题进一步缩小，不会浪费修复范围。
- 这条 bugfix 完成后，Phase 5 应回到“真实启动与界面级验收”主线，不再继续扩展新的文档包。
