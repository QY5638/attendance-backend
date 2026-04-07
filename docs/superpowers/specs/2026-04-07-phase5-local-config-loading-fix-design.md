# Phase 5 本地配置加载优先级修复设计

## 背景

在继续推进 `Phase 5` 真实启动与界面级验收时，已经拿到新的根因证据：

- 后端真实启动成功，`/api/health` 返回 `200`
- 真实登录 `POST /api/auth/login` 仍返回 `500`
- 加入异常栈日志后，后端日志明确显示：

`RedisConnectionFailureException: Unable to connect to change_me:6379`

这说明当前后端运行时并没有实际使用 `phase5-docs-runbook/application-local.yml` 中提供的 Redis 连接配置，而是退回到了 `application-dev.yml` 中的默认占位值：

- `REDIS_HOST: change_me`
- `DB_USERNAME: change_me`
- `DB_PASSWORD: change_me`

因此，当前真正的根因不是登录业务逻辑，也不是 token 序列化本身，而是：

**本地配置加载优先级/覆盖方式与 README 现有口径不一致，导致 `application-local.yml` 没有实际覆盖 `application-dev.yml` 的占位默认值。**

## 目标

修复本地配置加载链路，使以下条件同时成立：

1. 当前 worktree 根目录下的 `application-local.yml` 能稳定覆盖 `application-dev.yml` 的数据库和 Redis 默认占位值。
2. 后端真实运行时不再连接 `change_me:6379` 或使用 `change_me` 账号密码。
3. README 中“仓库根目录 `application-local.yml` 可直接用于本地启动”的口径重新变成真实可执行事实。
4. 修复后，后端真实登录 `POST /api/auth/login` 至少能越过配置阻塞，不再因 Redis/数据库默认占位值导致 `500`。

## 根因假设

当前最可信的根因是：

- `application.yml` 使用了：

`spring.config.import=optional:file:./application-local.yml`

- 但在当前 Spring Boot 2.7 配置链路下，这种 import 方式没有达到“覆盖 `application-dev.yml` 默认占位值”的效果。
- 真实运行仍然沿用了 `application-dev.yml` 中的 `${REDIS_HOST:change_me}`、`${DB_USERNAME:change_me}` 等默认表达式。

从当前实测表现看，这个 import 语句要么没有在期望优先级生效，要么其加载时机不足以覆盖 `application-dev.yml` 的默认值。

## 方案

### 推荐方案：在 `application.yml` 中改用 `spring.profiles.include: local`

把本地私有配置从“额外 import 文件”切换成显式 profile 叠加：

- 保留 `spring.profiles.active: dev`
- 增加 `spring.profiles.include: local`
- 将当前 worktree 根目录的 `application-local.yml` 改为 `src/main/resources/application-local.yml` 对等的 profile 文件风格并参与 Spring profile 合并

但考虑到当前私有配置文件已明确要求不能进仓库，因此更稳的最小落地方式是：

### 更小实现：继续使用 worktree 根目录私有文件，但通过 `spring.config.additional-location` 明确追加为高优先级来源

即在启动默认配置中显式声明：

- `spring.config.additional-location=optional:file:./`

并让 `application-local.yml` 保持仓库根目录私有文件名不变。

这样可以继续满足：

- 私有文件不进入仓库
- 当前 README 不需要改成“必须传环境变量”
- 配置来源仍与当前 worktree 同级

### 推荐落地选择

本轮推荐使用：

**`spring.config.additional-location` + 根目录 `application-local.yml`**

原因：

- 改动比 profile 重构更小
- 保持私有配置文件命名与 README 现有习惯一致
- 更接近“修复配置优先级”，而不是引入新的启动姿势

## 不采用的方案

### 1. 只在启动命令里塞环境变量

这能临时绕过问题，但 README 和状态文档当前的启动口径仍然是假象，后续新会话仍会踩坑。

### 2. 只在当前机器上手工改 Redis/数据库环境变量

这不是仓库级修复，也不具备可复用性。

### 3. 继续追登录代码

当前异常栈已经明确表明登录代码之前的基础配置就错了，继续追业务层只会偏离根因。

## 变更边界

本轮只允许最小修改：

- `src/main/resources/application.yml`
- 与此直接对应的后端文档说明（若修复后 README 启动口径需要同步）
- 必要的最小测试文件或现有测试补充

本轮不做：

- 修改 `AuthServiceImpl`
- 修改 `RedisTokenStore`
- 改用环境变量作为新的主方案
- 大改 Phase 5 文档结构

## 测试策略

严格按 TDD：

1. 先补一个最小失败测试或最小可验证断言，锁定“本地配置来源必须覆盖默认占位值”。
2. 确认当前实现失败：运行环境仍落到 `change_me`。
3. 做最小实现，修正配置加载优先级。
4. 验证层分两步：
   - 配置加载测试/断言通过
   - 真实后端重启后，登录不再因 `change_me:6379` 失败

## 验证要求

修复完成后至少要满足：

1. 真实后端启动后，日志或行为不再指向 `change_me:6379`
2. `POST /api/auth/login` 不再因为 Redis/数据库默认占位值导致 `500`
3. 如果 README 口径受影响，需要同步更新 README 的“配置来源与启动方式”说明

## 风险与后续

- 若修复后登录仍失败，则说明“配置加载优先级”只是第一个根因，后续再回到登录/Token 链路继续排查。
- 只有当后端真实配置链路恢复正常，`Phase 5` 才能继续推进真实 UI 验收与最终收口。
