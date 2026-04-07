# Phase 5 最终验收清单

## 1. 用途与适用范围

- 本清单用于 Phase 5 最终验收记录，聚焦前后端本地联调后的最终人工验收收口，不替代后端 README、前端 README 与本地联调手册。
- 当前后端工作目录：`D:\Graduation project\backend\.worktrees\phase5-docs-runbook`
- 当前前端工作目录：`D:\Graduation project\frontend\.worktrees\phase5-docs-runbook`
- 本清单默认以前置文档已完成且已校对为前提：`README.md`（后端）、`README.md`（前端）、`docs/integration/phase5-local-runbook.md`。
- 首个完成门槛只看最小主链：`登录 -> 异常查询 -> 预警查询 -> 复核提交 -> 统计查询`。
- 增强链路仅作为可选增强验收记录，不纳入首个完成门槛。

## 2. 验收前准备

### 2.1 文档与工作目录确认

- [x] 当前后端工作目录确认为 `D:\Graduation project\backend\.worktrees\phase5-docs-runbook`
- [x] 当前前端工作目录确认为 `D:\Graduation project\frontend\.worktrees\phase5-docs-runbook`
- [x] 已完成并可回查后端首包文档：`D:\Graduation project\backend\.worktrees\phase5-docs-runbook\README.md`
- [x] 已完成并可回查前端首包文档：`D:\Graduation project\frontend\.worktrees\phase5-docs-runbook\README.md`
- [x] 已完成并可回查联调首包文档：`D:\Graduation project\backend\.worktrees\phase5-docs-runbook\docs\integration\phase5-local-runbook.md`

### 2.2 环境、端口与数据基线确认

- [x] 后端从仓库根目录启动，使用 `application-local.yml`，默认端口为 `8080`
- [x] 前端开发代理仍使用 `VITE_API_BASE_URL=/api`，`VITE_API_PROXY_TARGET=http://127.0.0.1:8080`
- [x] 前端开发服务器按默认口径使用 `npm run dev` 启动，未额外指定端口时默认端口为 `5173`
- [x] 后端数据库已按 `sql/schema/数据库建表SQL.sql` -> `sql/seed/测试数据插入SQL.sql` 顺序初始化，并确认应用连接到 `system` 库
- [x] 最小主链验收优先复用仓库 seed 数据，不把新增增强链路作为前置条件

> 当前说明：截至 `2026-04-07`，`phase5-docs-runbook` 两个 worktree 的私有配置文件已补齐，真实本地启动与界面级留证已完成；同时为适配当前真实库，还对 `exceptionAnalysis`、`warningRecord`、`riskLevel`、`decisionTrace`、`modelCallLog` 做了最小 schema 同步。

### 2.3 最小命令校验

- [x] 已完成后端最小测试命令校验
- [x] 已完成后端编译命令校验
- [x] 已完成前端测试命令校验
- [x] 已完成前端构建命令校验

#### 2.3.1 后端最小测试

- 操作：在 `D:\Graduation project\backend\.worktrees\phase5-docs-runbook` 执行 `mvn "-Dtest=DeviceManagementIntegrationTest,AuthSecurityIntegrationTest,ModuleSkeletonBeansTest" test`。
- 预期：测试命令执行成功，最小测试集全部通过，无新增阻塞失败。
- 证据：`后端最小测试 | 2026-04-07 17:43 / backend\.worktrees\phase5-docs-runbook | Maven surefire 输出（Tests run: 41, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS）`
- 失败时记录：记录失败测试类名、报错摘要、相关日志位置、是否为环境问题或代码问题。

#### 2.3.2 后端编译

- 操作：在 `D:\Graduation project\backend\.worktrees\phase5-docs-runbook` 执行 `mvn -DskipTests compile`。
- 预期：编译成功结束，无编译错误、无资源加载阻塞问题。
- 证据：`后端编译 | 2026-04-07 17:43 / backend\.worktrees\phase5-docs-runbook | Maven compile 输出（BUILD SUCCESS）`
- 失败时记录：记录失败模块、报错摘要、相关日志位置、是否可稳定复现。

#### 2.3.3 前端测试

- 操作：在 `D:\Graduation project\frontend\.worktrees\phase5-docs-runbook` 执行 `npm test`。
- 预期：前端测试命令执行成功，现有测试全部通过，无阻塞性报错。
- 证据：`前端测试 | 2026-04-07 17:43 / frontend\.worktrees\phase5-docs-runbook | Vitest 输出（36 文件、169 测试通过）`
- 失败时记录：记录失败用例名、报错摘要、相关日志位置、是否为环境依赖问题。

#### 2.3.4 前端构建

- 操作：在 `D:\Graduation project\frontend\.worktrees\phase5-docs-runbook` 执行 `npm run build`。
- 预期：构建成功结束，产物生成正常，无阻塞性构建错误。
- 证据：`前端构建 | 2026-04-07 17:43 / frontend\.worktrees\phase5-docs-runbook | Vite build 输出（成功，存在 chunk 大小告警）`
- 失败时记录：记录失败阶段、报错摘要、相关日志位置、是否与环境变量或依赖缺失有关。

### 2.4 可选增强链路边界确认

- [x] 已明确最小主链只覆盖 `登录 -> 异常查询 -> 预警查询 -> 复核提交 -> 统计查询`
- [x] 已明确增强链路只在单独的可选增强验收区记录，不与必验项混排
- [x] 已明确本轮先完成 seed 优先主链，再决定是否补充可选增强验收记录

## 3. 启动验收

### 3.1 后端启动验收


- [x] 在 `D:\Graduation project\backend\.worktrees\phase5-docs-runbook` 执行 `mvn spring-boot:run`
- [x] 启动日志未出现数据源、Redis、端口占用类阻塞错误
- [x] 服务实际监听在 `8080`
- [x] 本次启动读取的是仓库根目录下的 `application-local.yml`

> 证据：`后端真实启动 | 2026-04-07 / backend\.worktrees\phase5-docs-runbook | /api/health => {"code":200,"message":"success","data":"ok"}`

### 3.2 前端启动验收


- [x] 如为首次启动，已在 `D:\Graduation project\frontend\.worktrees\phase5-docs-runbook` 执行 `npm install`
- [x] 在前端仓库根目录执行 `npm run dev`
- [x] 页面可通过本地开发地址访问，未额外指定端口时按默认 `5173` 验证
- [x] 前端请求仍通过 `/api` 代理到 `http://127.0.0.1:8080`

> 证据：`前端真实启动 | 2026-04-07 / frontend\.worktrees\phase5-docs-runbook | http://localhost:5173 => 200`

### 3.3 启动口径复核

- [x] 后端启动方式、端口、配置文件位置与后端 README 一致
- [x] 前端环境变量、代理目标、开发端口口径与前端 README 一致
- [x] 本地启动顺序、seed 优先主链口径与 `docs/integration/phase5-local-runbook.md` 一致

## 4. 联调验收

### 4.1 最小主链必验项

#### 4.1.1 登录

- 操作：使用 seed 优先口径验证登录，优先使用 `admin/123456` 登录并进入业务页面。
- 预期：登录成功，页面进入业务首页或默认业务页，后续接口请求不再因未认证失败。
- 证据：`登录真实验收 | 2026-04-07 / backend\.worktrees\phase5-docs-runbook | POST /api/auth/login => code=200, roleCode=ADMIN, realName=系统管理员；页面截图：docs/integration/evidence/phase5-ui/01-login.png, 02-dashboard.png`
- 失败时记录：若后续做真实 UI 验收失败，记录使用账号、页面提示、接口状态码、响应摘要、浏览器控制台或网络面板关键信息。

#### 4.1.2 异常查询

- 操作：进入异常查询页，优先查询 seed 中的规则异常 `3002` 或 `3003`，或按页面默认排序直接定位对应记录。
- 预期：异常列表可正常加载，至少能定位到 `3002` 或 `3003` 中的一条规则异常记录。
- 证据：`异常查询真实验收 | 2026-04-07 / backend\.worktrees\phase5-docs-runbook | GET /api/exception/list => total=3；页面截图：docs/integration/evidence/phase5-ui/03-exception.png`
- 失败时记录：若后续做真实 UI 验收失败，记录查询条件、实际返回结果、页面报错或空列表表现、相关接口返回摘要。

#### 4.1.3 预警查询

- 操作：进入预警查询页，优先查询与上述异常对应的 `5002` 或 `5003`，确认待处理记录可见。
- 预期：预警列表可正常加载，能定位到与 seed 异常对应的预警记录，且初始状态为 `UNPROCESSED` 或等价待处理状态。
- 证据：`预警查询真实验收 | 2026-04-07 / backend\.worktrees\phase5-docs-runbook | GET /api/warning/list?status=UNPROCESSED => total=1，目标预警=5003；页面截图：docs/integration/evidence/phase5-ui/04-warning.png`
- 失败时记录：若后续做真实 UI 验收失败，记录查询条件、实际状态、页面报错或空列表表现、相关接口返回摘要。

#### 4.1.4 复核提交

- 操作：从异常页或预警页进入复核页，对 `3002` 或 `3003` 提交一次复核，并回查关联预警状态。
- 预期：复核页可基于目标 `exceptionId` 正常初始化，提交成功后，对应预警状态更新为 `PROCESSED` 或等价已处理状态。
- 证据：`复核提交流转真实验收 | 2026-04-07 / backend\.worktrees\phase5-docs-runbook | POST /api/review/submit => code=200, reviewResult=CONFIRMED，warning 5003: UNPROCESSED -> PROCESSED；页面截图：docs/integration/evidence/phase5-ui/05-review.png`
- 失败时记录：若后续做真实 UI 验收失败，记录入口页面、目标异常编号、提交参数摘要、失败提示、接口状态码、状态未更新的实际表现。

#### 4.1.5 统计查询

- 操作：复核提交后进入统计页，查询当前库内异常汇总结果。
- 预期：统计页可正常返回当前库内异常汇总数据，且结果与当前 seed 主链数据口径一致。
- 证据：`统计查询真实验收 | 2026-04-07 / backend\.worktrees\phase5-docs-runbook | GET /api/statistics/summary => code=200；页面截图：docs/integration/evidence/phase5-ui/06-statistics.png`
- 失败时记录：若后续做真实 UI 验收失败，记录统计条件、页面表现、接口状态码、返回空结果或异常结果的具体描述。

### 4.2 主链范围限制

- [x] 最小主链仅覆盖 `登录 -> 异常查询 -> 预警查询 -> 复核提交 -> 统计查询`
- [x] 本轮最终验收优先复用仓库 seed 数据完成主链，不以新增现场数据作为必验前提
- [x] 本轮最终验收先完成 seed 优先主链，再决定是否追加可选增强验收记录

### 4.3 必验项记录完成条件

- [x] 第 2.3 节四项命令类必验项均已填写“操作 / 预期 / 证据 / 失败时记录”
- [x] 第 4.1 节五项主链必验项均已填写“操作 / 预期 / 证据 / 失败时记录”
- [x] 必验项证据已能独立支撑首个完成门槛结论，不依赖增强链路补证

> 当前说明：命令类必验项、真实本地启动、真实 API 主链与页面级留证均已完成，首个完成门槛已达成；可选增强链路仍未验证，但不阻塞 Phase 5 完成。
>
> 测试侧合并结果：`主链测试侧验收 | 2026-04-07 18:47 / backend\.worktrees\phase5-docs-runbook | Maven surefire 输出（tests=5, failures=0, errors=0, skipped=0, BUILD SUCCESS）`
>
> 真实主链合并结果：`真实 API 主链 | 2026-04-07 / backend\.worktrees\phase5-docs-runbook | login=200, exception=200, warning=200, review=200, statistics=200`

## 5. 可选增强验收

- 本区仅记录可选增强，不纳入首个完成门槛，也不替代第 2.3 节和第 4.1 节必验项。
- 本区之外不再单独列出增强项名称，避免与必验项混排。

### 5.1 填写规则

- 可选增强项不阻塞首个 Phase 5 完成，也不作为最终通过判定的前置条件。
- 如未验证，可只记录“未验证原因”；无需为了补齐本区记录而额外执行增强链路验证。
- 如已验证，建议记录“是否已验证 / 若已验证，证据放在哪里”，并可补充简短结果说明。

### 5.2 可选增强项记录模板

#### 5.2.1 实时新打卡链路

- 是否已验证：否
- 未验证原因：本轮优先完成 seed 优先的最小主链；实时新打卡仍需要员工账号、人脸采集结果与可用 `imageData` 配合验证。
- 若已验证，证据放在哪里（按第 6 节格式填写）：
- 补充说明：

#### 5.2.2 地图展示

- 是否已验证：否
- 未验证原因：本轮未把地图展示纳入最小必验范围；地图场景仍需单独页面交互与 key 生效验证。
- 若已验证，证据放在哪里（按第 6 节格式填写）：
- 补充说明：

#### 5.2.3 复杂异常分析

- 是否已验证：否
- 未验证原因：本轮未把复杂异常分析纳入最小必验范围；该链路仍建议在完整 LLM 配置和更针对性的样本下单独验证。
- 若已验证，证据放在哪里（按第 6 节格式填写）：
- 补充说明：

#### 5.2.4 多地点异常

- 是否已验证：否
- 未验证原因：本轮未把多地点异常纳入最小必验范围；该链路仍需结合地图/空间能力做专项验证。
- 若已验证，证据放在哪里（按第 6 节格式填写）：
- 补充说明：

#### 5.2.5 导出类页面演示

- 是否已验证：否
- 未验证原因：本轮优先完成最小主链与核心页面留证，未单独补导出演示截图。
- 若已验证，证据放在哪里（按第 6 节格式填写）：
- 补充说明：

## 6. 证据清单与截图要求

### 6.1 证据栏统一填写格式

- 第 2.3 节、第 4.1 节和第 5 节中所有“证据”或“证据放在哪里”栏位，统一按 `命令/页面名称 | 时间或位置 | 文件名、日志名或截图编号` 填写。
- 推荐示例：`后端最小测试 | 2026-04-07 14:20 / docs/evidence/backend | screenshot-backend-test-01.png`。
- 不建议使用“见截图”“已截图”“看录屏”等弱表述；至少要能从填写内容直接回查到具体证据文件或编号。

### 6.2 必验项证据清单

- 命令类必验项至少保留 4 份证据，分别对应：后端最小测试、后端编译、前端测试、前端构建。
- 页面类必验项至少保留 5 份证据，分别对应：登录、异常查询、预警查询、复核提交、统计查询。
- 如使用录屏代替多张截图，必须在对应“证据”栏写明录屏文件名、时间点和存放位置。

### 6.3 截图最低要求

- 命令类截图至少包含：执行命令、当前工作目录、成功或失败摘要。
- 页面类截图至少包含：当前页面、关键查询条件或目标编号、结果区域。
- 复核提交至少能证明两件事：提交动作成功、回查后的关联状态已更新；可用两张截图或一段连续录屏覆盖。
- 若截图无法完整展示关键信息，可在“证据”栏补充日志路径、接口响应摘要或录屏时间点。

### 6.4 失败时统一记录要求

- 统一记录发生时间、执行环境、复现步骤、实际结果、错误提示、日志位置和是否阻塞最终验收。
- 若失败由环境差异引起，应明确写出差异点，例如数据库数据不一致、端口占用、配置缺失或代理未生效。

## 7. 最终通过判定与遗留问题记录

- 本节用于登记最终通过判定、允许或不允许标记 Phase 5 完成的边界，以及已知问题。
- 在第 2.3 节和第 4.1 节必验项未核对完之前，不提前写“通过/不通过”结论。

### 7.1 最终通过判定口径

 - 最终通过判定只看第 2.3 节四项命令类必验项、第 3.1-3.3 节真实本地启动与口径复核，以及第 4.1 节五项主链必验项。
- 第 5 节可选增强验收仅作补充记录：已验证可附证据，未验证可写明原因，但不影响最终通过判定。
- 任一必验项缺少“操作 / 预期 / 证据 / 失败时记录”，或存在未关闭的阻塞性失败，都不能登记为“通过”。
- 截至 `2026-04-07`，第 2.3 节命令类验收、第 3 节真实本地启动、第 4.1 节最小主链验收以及第 6 节证据与截图要求均已落实，当前结论为“通过”，允许把 `Phase 5` 标记为完成。

### 7.2 允许标记 Phase 5 完成的条件

- 第 2.3 节四项命令类必验项已完成记录，并具备可回查证据。
- 第 3.1 节、`3.2` 节、`3.3` 节真实本地启动、代理、配置文件位置与口径复核已完成，并具备可回查证据。
- 第 4.1 节五项主链必验项已完成记录，并具备可回查证据。
- 最终通过判定已明确登记，且结论仅基于必验项得出，不依赖可选增强补证。
- 后端 README、前端 README、`docs/integration/phase5-local-runbook.md`、本清单与状态文档已完成一致性复核。
- 已知问题已完成阻塞/非阻塞归类；若仍有非阻塞问题，已明确写明“不阻塞最终通过判定”。

### 7.3 不允许标记 Phase 5 完成的情况

- 第 2.3 节或第 4.1 节仍有未填写、未验证、缺证据或缺失败记录的必验项。
- 第 3.1 节、`3.2` 节或 `3.3` 节真实本地启动与口径复核尚未完成，或无法证明工作区私有配置、代理、端口、启动结果与文档一致。
- 仅完成 README、联调手册或可选增强记录，但未完成必验项核对与最终通过判定。
- 仅凭第 5 节可选增强验收结果补足结论，或把可选增强与必验项混合作为“通过”依据。
- 后端 README、前端 README、联调手册、本清单与状态文档之间仍存在路径、命令、口径或完成边界不一致。
- 第二包验收清单与状态文档尚未收口确认一致时，提前在任一文档中写“Phase 5 已完成”。

### 7.4 已知问题记录模板

#### 已知问题 1：真实库 schema 历史漂移（已临时同步）

- 问题级别：非阻塞
- 关联必验项：`3.1`、`4.1`、统计摘要与预警/复核链路
- 发现时间：2026-04-07
- 现象描述：真实库 `system` 中的 `exceptionAnalysis`、`warningRecord`、`riskLevel`、`decisionTrace`、`modelCallLog` 与当前代码实体存在历史 schema 漂移，导致真实 API 主链在不同步骤出现 `Unknown column` 或缺表异常。
- 复现步骤：真实启动后依次请求 `/api/warning/list`、`/api/review/submit`、`/api/statistics/summary`，可在 `backend-run.log` 中看到对应 SQL 异常栈。
- 当前结论：当前已按主链所需做最小 schema 同步，真实主链已恢复。
- 是否阻塞最终通过判定：否
- 临时处理或绕过方式：无；当前 worktree 真实验收已按同步后的 schema 完成。
- 证据位置：`真实库 schema 同步 | 2026-04-07 / system.exceptionAnalysis, warningRecord, riskLevel, decisionTrace, modelCallLog | 最小 ALTER/CREATE TABLE 执行记录`
- 后续跟进说明：如后续继续在真实库联调，建议补一份正式 migration，避免再次出现历史表结构漂移。

#### 已知问题 2：前端构建存在 chunk 大小告警

- 问题级别：非阻塞
- 关联必验项：`2.3.4` 前端构建
- 发现时间：2026-04-07
- 现象描述：`npm run build` 成功，但 Vite 输出 chunk 大小告警。
- 复现步骤：在 `frontend/.worktrees/phase5-docs-runbook` 运行 `npm run build`。
- 当前结论：当前不阻塞 Phase 5 首个完成判定，但需要保留记录。
- 是否阻塞最终通过判定：否
- 临时处理或绕过方式：保持现状并登记为非阻塞已知项。
- 证据位置：`前端构建 | 2026-04-07 17:43 / frontend\.worktrees\phase5-docs-runbook | Vite build 输出（成功，存在 chunk 大小告警）`
- 后续跟进说明：如后续收尾前仍存在，继续在最终结果中保留该说明。
