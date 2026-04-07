# Phase 5 启动文档与联调手册首包 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 交付后端 README、前端 README 与独立联调手册，使新机器或新会话可以按文档完成前后端启动与最小主链联调准备。

**Architecture:** 保持代码与工程配置不变，只补齐文档层交付。后端 README 负责单仓库启动说明，前端 README 负责单仓库启动说明，联调手册负责跨仓库串联顺序；`继续执行文档-完整系统.md` 只负责状态回写。

**Tech Stack:** Markdown, Spring Boot, Maven, Vue 3, Vite, Vitest

---

### Task 1: 建立前端隔离 worktree 并确认文档改动工作区

**Files:**
- Create: `D:/Graduation project/frontend/.worktrees/phase5-docs-runbook/`

- [ ] **Step 1: 确认前端 `.worktrees` 已忽略**

```powershell
cd "D:\Graduation project\frontend"
git check-ignore -q .worktrees
```

Expected: 退出码为 `0`

- [ ] **Step 2: 创建前端隔离 worktree**

```powershell
cd "D:\Graduation project\frontend"
git worktree add ".worktrees/phase5-docs-runbook" -b "phase5-docs-runbook"
```

Expected: 新 worktree 创建成功，基于 `frontend/main`

- [ ] **Step 3: 安装前端依赖**

```powershell
cd "D:\Graduation project\frontend\.worktrees\phase5-docs-runbook"
npm install
```

Expected: 依赖安装成功

- [ ] **Step 4: 运行前端基线命令确认当前工程可用**

```powershell
cd "D:\Graduation project\frontend\.worktrees\phase5-docs-runbook"
npm test
```

Expected: 当前前端测试集通过；若失败，先记录为基线风险，再停止执行后续文档宣称

### Task 2: 重写后端 README

**Files:**
- Modify: `D:/Graduation project/backend/.worktrees/phase5-docs-runbook/README.md`
- Test: `D:/Graduation project/backend/.github/workflows/backend-ci.yml`
- Test: `D:/Graduation project/backend/pom.xml`
- Test: `D:/Graduation project/backend/application-local.example.yml`

- [ ] **Step 1: 写入后端 README 的最小完整结构**

````markdown
# attendance-backend

考勤异常检测与预警系统后端。

## 1. 技术栈

- Spring Boot 2.7.18
- Java 8
- Maven
- MyBatis-Plus
- MySQL
- Redis

## 2. 环境要求

- JDK 8
- Maven 3.8+
- MySQL 8.x
- Redis 6.x 或兼容版本

## 3. 本地配置

1. 复制 `application-local.example.yml` 为 `application-local.yml`
2. 填写以下必填项：
   - `spring.datasource.*`
   - `spring.redis.*`
   - `app.llm.*`
   - `app.map.*`

## 4. 启动步骤

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

默认端口：`8080`

## 5. 测试与编译

```powershell
mvn "-Dtest=DeviceManagementIntegrationTest,AuthSecurityIntegrationTest,ModuleSkeletonBeansTest" test
mvn -DskipTests compile
```

## 6. 常见问题

- MySQL 无法连接
- Redis 无法连接
- LLM/地图配置缺失导致相关能力不可用
````

- [ ] **Step 2: 用仓库真实信息补齐 README 内容**

```markdown
需要把以下具体内容写实，而不是保留占位：
- 后端模块简介
- 配置文件路径与复制方式
- `backend-ci.yml` 里的最小测试命令
- `pom.xml` 对应的 Java/Maven 版本约束
- `application-local.example.yml` 中 LLM 与地图配置项说明
```

- [ ] **Step 3: 运行 README 中声明的后端验证命令**

```powershell
cd "D:\Graduation project\backend\.worktrees\phase5-docs-runbook"
mvn "-Dtest=DeviceManagementIntegrationTest,AuthSecurityIntegrationTest,ModuleSkeletonBeansTest" test
mvn -DskipTests compile
```

Expected: 两条命令都成功

### Task 3: 重写前端 README

**Files:**
- Modify: `D:/Graduation project/frontend/.worktrees/phase5-docs-runbook/README.md`
- Test: `D:/Graduation project/frontend/.env.example`
- Test: `D:/Graduation project/frontend/package.json`

- [ ] **Step 1: 写入前端 README 的最小完整结构**

````markdown
# attendance-frontend

考勤异常检测与预警系统前端。

## 1. 技术栈

- Vue 3
- Vite
- Element Plus
- Pinia
- Vue Router

## 2. 环境要求

- Node.js 18+
- npm 9+

## 3. 本地配置

1. 复制 `.env.example` 为 `.env.local`
2. 重点检查：
   - `VITE_API_BASE_URL`
   - `VITE_API_PROXY_TARGET`
   - `VITE_AMAP_KEY`

## 4. 启动、构建与测试

```powershell
npm install
npm run dev
npm run build
npm test
```

## 5. 常见问题

- 代理目标未指向本地后端
- 登录请求失败
- 地图 key 缺失导致地图不可用
````

- [ ] **Step 2: 用仓库真实信息补齐前端 README 内容**

```markdown
需要把以下具体内容写实，而不是保留占位：
- 当前开发端口与代理模式
- `.env.example` 中各环境变量的含义
- `package.json` 里的实际脚本名
- 前端与后端联调时的最小依赖关系
```

- [ ] **Step 3: 运行 README 中声明的前端验证命令**

```powershell
cd "D:\Graduation project\frontend\.worktrees\phase5-docs-runbook"
npm test
npm run build
```

Expected: 两条命令都成功

### Task 4: 新增独立联调手册

**Files:**
- Create: `D:/Graduation project/backend/.worktrees/phase5-docs-runbook/docs/integration/phase5-local-runbook.md`
- Test: `D:/Graduation project/backend/.worktrees/phase5-docs-runbook/README.md`
- Test: `D:/Graduation project/frontend/.worktrees/phase5-docs-runbook/README.md`

- [ ] **Step 1: 写入联调手册的骨架内容**

````markdown
# Phase 5 本地启动与联调手册

## 1. 适用范围

适用于本地从零准备前后端环境，并完成最小主链联调。

## 2. 前置依赖

- MySQL
- Redis
- LLM 配置
- 地图 Key

## 3. 推荐启动顺序

1. 配置并启动后端
2. 配置并启动前端
3. 登录系统并验证主链入口

## 4. 最小联调路径

1. 登录
2. 打卡/异常页检查
3. 预警页检查
4. 复核页检查
5. 统计页检查

## 5. 最小验证命令

- 后端：`mvn "-Dtest=DeviceManagementIntegrationTest,AuthSecurityIntegrationTest,ModuleSkeletonBeansTest" test`
- 前端：`npm test`

## 6. 常见阻塞排查

- 后端未启动
- 前端代理未指向 `8080`
- 地图或 LLM 配置缺失
````

- [ ] **Step 2: 把最小联调路径写实为可执行步骤**

```markdown
每一步必须包含：
- 进入哪个页面
- 依赖哪个服务先启动
- 预期看到什么结果
- 如果失败先查哪里
```

- [ ] **Step 3: 交叉校对联调手册与两个 README**

```powershell
cd "D:\Graduation project\backend\.worktrees\phase5-docs-runbook"
git diff -- README.md docs/integration/phase5-local-runbook.md

cd "D:\Graduation project\frontend\.worktrees\phase5-docs-runbook"
git diff -- README.md
```

Expected: 三份文档中的命令、端口、配置路径描述一致，无互相矛盾项

### Task 5: 回写状态文档并做本轮最小验证记录

**Files:**
- Modify: `D:/Graduation project/继续执行文档-完整系统.md`
- Test: `D:/Graduation project/继续执行文档-完整系统.md`

- [ ] **Step 1: 更新阶段状态与下一步动作**

```markdown
- 当前阶段改为 `Phase 5 - 文档、部署、联调、验收闭环`
- 本轮完成：启动文档 + 独立联调手册首包
- 本轮未完成：最终验收步骤、证据清单、截图要求
- 下一步第一动作：锁定 Phase 5 第二包（验收清单与证据要求）
```

- [ ] **Step 2: 回写本轮实际验证**

```markdown
记录以下验证命令与结果：
- 后端：`mvn "-Dtest=DeviceManagementIntegrationTest,AuthSecurityIntegrationTest,ModuleSkeletonBeansTest" test`
- 后端：`mvn -DskipTests compile`
- 前端：`npm test`
- 前端：`npm run build`
```

- [ ] **Step 3: 最终人工检查四份文档都已落地**

```powershell
Get-Item "D:\Graduation project\backend\.worktrees\phase5-docs-runbook\README.md"
Get-Item "D:\Graduation project\frontend\.worktrees\phase5-docs-runbook\README.md"
Get-Item "D:\Graduation project\backend\.worktrees\phase5-docs-runbook\docs\integration\phase5-local-runbook.md"
Get-Item "D:\Graduation project\继续执行文档-完整系统.md"
```

Expected: 四个路径全部存在，且已被本轮修改
