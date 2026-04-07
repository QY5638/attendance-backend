# BE-05 异常检测与智能分析模块设计文档

## 1. 目标

在现有后端骨架和 `BE-01~04` 已完成能力之上，补齐 `BE-05` 的最小必要实现，形成“基础规则异常判定 + 复杂异常智能分析 + 决策链路留痕 + 异常结果查询”闭环，为 `BE-06/07/08` 提供稳定、可复用的异常主数据和分析结果。

本次设计只覆盖 `BE-05` 模块边界内的固定接口，不扩展到预警生成、人工复核、统计报表，也不顺手改造 `BE-01~04` 既有实现。

## 2. 约束与边界

### 2.1 已确认约束

- 以 `docs/module-guides/BE-05-异常检测与智能分析模块开发指南.md` 和 `docs/api/API接口设计文档.md` 为第一优先级契约。
- 只实现 `BE-05` 指南中的固定接口，不纳入 `/api/system/prompt/*`、`/api/system/model-log/list` 等支撑管理接口。
- 沿用当前项目的统一返回、分页对象、全局异常处理和 Spring Security 链路。
- 异常主结果直接复用现有表：`rule`、`attendanceException`、`exceptionAnalysis`、`promptTemplate`、`modelCallLog`、`decisionTrace`。
- `rule` 视为 `exceptiondetect` 子域，不新增独立顶层模块。
- 公开业务接口放在 `exceptiondetect`，提示词、模型调用日志、决策追踪仅作为内部支撑能力存在。
- 本轮 `BE-05` 默认不调整 `BE-01~04` 的数据库结构、接口契约和主业务流程，除非实现中出现明确阻塞并再次确认。
- 本轮进入实现前需新建独立英文分支和 worktree，但该动作放到编码前执行，不在本设计阶段执行。

### 2.2 业务规则

- 固定接口范围：
  - `POST /api/exception/rule-check`
  - `POST /api/exception/complex-check`
  - `GET /api/exception/list`
  - `GET /api/exception/{id}`
  - `GET /api/exception/{id}/decision-trace`
  - `GET /api/exception/{id}/analysis-brief`
  - `GET /api/rule/list`
  - `POST /api/rule/add`
  - `PUT /api/rule/update`
  - `PUT /api/rule/status`
- `rule-check` 的可信输入以 `recordId` 对应的 `attendanceRecord` 为准，不依赖前端传入异常结论。
- `complex-check` 允许接收 API 文档中的 `recordId + userId + riskFeatures`，但后端以 `recordId` 落库记录和关联数据重新组织分析输入，`riskFeatures` 仅作为补充，不直接信任前端结论。
- 基础规则异常本轮只覆盖最小可落地场景：`LATE`、`EARLY_LEAVE`、`REPEAT_CHECK`、`ILLEGAL_TIME`。
- 本轮 `BE-05` 的 MVP 明确不做 `ABSENT` 自动判定，因为当前系统缺少排班、出勤日历和日终扫描触发闭环，强行加入会越界修改上游模块。
- 同一条打卡记录在同一来源类型下重复触发时，应优先复用已存在异常记录，避免重复插入。
- 复杂异常分析需产出可复用字段：`type`、`riskLevel`、`sourceType`、`processStatus`、`modelConclusion`、`reasonSummary`、`actionSuggestion`、`confidenceScore`。
- 模型调用失败时，不直接把外部失败暴露为接口失败，而是以保守降级结果完成异常落库与决策留痕。

### 2.3 非目标

- 不在本模块生成预警记录。
- 不在本模块提交或处理人工复核。
- 不在本模块输出统计报表或趋势摘要。
- 不开放提示词模板管理和模型调用日志查询接口。
- 不引入真实排班、考勤日历、定时缺勤扫描等扩展机制。
- 不重做人脸识别算法，也不修改 `BE-03` 的 `faceScore` 生成逻辑。

## 3. 现状基线与复用点

### 3.1 公共层复用

- 统一响应继续使用 `Result<T>`，结构固定为 `code/message/data`。
- 分页响应继续使用 `PageResult<T>`，结构固定为 `total/records`。
- 业务异常继续使用 `BusinessException + GlobalExceptionHandler`。
- 当前登录用户获取方式沿用现有 `SecurityContextHolder -> AuthUser` 模式，不新建鉴权体系。

### 3.2 上游输入契约复用

- `BE-04` 的 `attendanceRecord` 是 `BE-05` 的核心事实源，直接复用字段：
  - `id` 作为 `recordId`
  - `userId`
  - `checkTime`
  - `checkType`
  - `deviceId`
  - `ipAddr`
  - `location`
  - `faceScore`
  - `status`
- `BE-02` 设备主数据继续通过 `device.id` 与 `attendanceRecord.deviceId` 对接，对外语义仍视为 `deviceId`。
- `BE-03` 的人脸验证结果本轮只复用已落入打卡记录中的 `faceScore`，不重新定义 `registered/matched/threshold` 契约。
- `BE-01` 提供的 `AuthUser.roleCode` 继续作为管理员访问边界判断基础。

### 3.3 现实限制

- 当前默认人脸比对分数近似二值化，成功打卡中的 `faceScore` 可作为风险特征，但不应作为精细评分主依据。
- 主工作树尚无 `exceptiondetect` 和 `model` Java 实现，测试基线也尚未包含 `BE-05` 相关表。
- 仓库内旧 worktree 存在未合入的 `phase-b-llm-foundation` 骨架；本次只复用其合理的结构思路，不直接把其额外支撑接口纳入范围。

## 4. 模块边界与结构设计

### 4.1 公开业务模块

新增 `src/main/java/com/quyong/attendance/module/exceptiondetect/`，内部按现有项目分层组织：

- `controller`
- `service`
- `service/impl`
- `mapper`
- `entity`
- `dto`
- `vo`
- `support`

该模块负责：

- 规则配置管理
- 基础规则判定
- 复杂异常分析编排
- 异常主数据查询
- 分析摘要查询

### 4.2 内部支撑模块

新增轻量 `src/main/java/com/quyong/attendance/module/model/` 作为内部支撑层，仅提供：

- `prompt`：读取启用中的提示词模板
- `gateway`：统一模型调用入口与 mock/外部实现切换点
- `log`：写入 `modelCallLog`
- `trace`：写入并查询 `decisionTrace`

该层本次不暴露独立管理接口，仅服务 `BE-05` 内部编排。

### 4.3 控制器职责

- `ExceptionController`：承接异常判定与查询接口
- `RuleController`：承接规则管理接口

控制器只负责 DTO 绑定、调用 service 和返回统一 `Result`，不直接承载判定逻辑、模型调用逻辑或 SQL 组装。

### 4.4 Service 职责

- `RuleService`：规则查询、新增、修改、状态切换、获取启用规则
- `ExceptionRuleCheckService` 或等价编排服务：基于 `attendanceRecord` 执行规则判定、异常落库和决策追踪写入
- `ExceptionComplexAnalysisService` 或等价编排服务：提取风险特征、选择模板、调用模型、落库分析结果与调用日志
- `ExceptionQueryService`：异常列表、异常详情、分析摘要查询
- `DecisionTraceService`：统一保存和读取决策链

### 4.5 Support 职责

新增轻量支撑组件，负责：

- 规则入参与状态校验
- 异常查询参数归一化与分页参数校验
- 复杂分析特征组装与输入摘要构造
- 当前用户管理员身份辅助判断

Support 只承载校验、归一化和只读辅助逻辑，不直接落库。

## 5. 数据与契约设计

### 5.1 表结构复用

#### 5.1.1 `rule`

继续作为基础规则配置表，复用字段：

- `id`
- `name`
- `startTime`
- `endTime`
- `lateThreshold`
- `earlyThreshold`
- `repeatLimit`
- `status`

#### 5.1.2 `attendanceException`

作为 `BE-05` 的异常主记录表，对下游模块提供稳定主键和状态口径，复用字段：

- `id`
- `recordId`
- `userId`
- `type`
- `riskLevel`
- `sourceType`
- `description`
- `processStatus`
- `createTime`

字段语义约定：

- `sourceType`：本轮至少包含 `RULE`、`MODEL`、`MODEL_FALLBACK`
- `processStatus`：本轮创建时统一写 `PENDING`，后续由 `BE-06/07` 消费更新

#### 5.1.3 `exceptionAnalysis`

仅用于复杂异常分析结果，复用字段：

- `exceptionId`
- `promptTemplateId`
- `inputSummary`
- `modelResult`
- `modelConclusion`
- `confidenceScore`
- `decisionReason`
- `suggestion`
- `reasonSummary`
- `actionSuggestion`
- `similarCaseSummary`
- `promptVersion`

其中：

- `suggestion` 保持与表结构兼容
- `actionSuggestion` 作为对下游更稳定的建议字段

#### 5.1.4 `promptTemplate`

本轮只读使用，按 `sceneType=EXCEPTION_ANALYSIS` 和启用状态读取生效模板。

#### 5.1.5 `modelCallLog`

记录复杂分析模型调用过程：

- `businessType` 固定使用 `EXCEPTION_ANALYSIS`
- `businessId` 指向对应 `attendanceException.id`
- `status` 至少包含 `SUCCESS`、`FAILED`

#### 5.1.6 `decisionTrace`

记录规则结果、模型结果、最终决策，`businessType` 统一使用 `ATTENDANCE_EXCEPTION`，`businessId` 指向 `attendanceException.id`。

### 5.2 接口请求与响应模型

#### 5.2.1 `POST /api/exception/rule-check`

请求模型最小化：

- `recordId`

响应模型建议：

- `exceptionId`
- `type`
- `riskLevel`
- `sourceType`
- `processStatus`

若未命中基础异常：

- 返回 `code=200`
- `data=null`

#### 5.2.2 `POST /api/exception/complex-check`

请求模型：

- `recordId`
- `userId`
- `riskFeatures`

其中 `riskFeatures` 兼容 API 文档，但服务端重新计算并补齐以下分析输入：

- 当前打卡时间与类型
- 当前设备与地点
- 当前 `faceScore`
- 历史异常次数
- 近期设备切换情况
- 近期地点变化情况
- 近期重复/边界时间行为概况

响应模型在基础异常响应字段上扩展：

- `modelConclusion`
- `reasonSummary`
- `actionSuggestion`
- `confidenceScore`

#### 5.2.3 `GET /api/exception/list`

查询参数：

- `pageNum`
- `pageSize`
- `type`
- `riskLevel`
- `processStatus`
- `userId`

返回继续使用 `Result<PageResult<AttendanceExceptionVO>>`，不另起 `Map(total, records)` 套路。

#### 5.2.4 `GET /api/exception/{id}`

返回异常主记录详情：

- `id`
- `recordId`
- `userId`
- `type`
- `riskLevel`
- `sourceType`
- `description`
- `processStatus`
- `createTime`

#### 5.2.5 `GET /api/exception/{id}/decision-trace`

返回决策链明细：

- `businessType`
- `businessId`
- `ruleResult`
- `modelResult`
- `finalDecision`
- `confidenceScore`
- `decisionReason`
- `createTime`

#### 5.2.6 `GET /api/exception/{id}/analysis-brief`

返回复杂分析摘要：

- `modelConclusion`
- `reasonSummary`
- `actionSuggestion`
- `similarCaseSummary`
- `promptVersion`
- `confidenceScore`

#### 5.2.7 `GET /api/rule/list`

查询参数：

- `pageNum`
- `pageSize`
- `keyword`
- `status`

返回继续使用 `Result<PageResult<RuleVO>>`。

#### 5.2.8 `POST /api/rule/add`
#### 5.2.9 `PUT /api/rule/update`
#### 5.2.10 `PUT /api/rule/status`

沿用当前动作式接口风格，新增/修改入参字段与 API 文档保持一致，状态接口只允许更新 `status`。

### 5.3 返回语义

- 成功：HTTP `200`，响应体 `code=200`、`message=success`
- 参数或业务校验失败：HTTP `200`，响应体 `code=400`
- 未登录、token 无效、token 过期：HTTP `401`
- 已登录但权限不足：HTTP `403`

建议业务错误消息：

- `考勤记录不存在`
- `考勤记录用户与请求用户不一致`
- `未找到启用中的考勤规则`
- `未找到启用中的异常分析模板`
- `异常记录不存在`
- `异常分析摘要不存在`
- `规则不存在`
- `规则名称不能为空`
- `规则状态不合法`

## 6. 核心数据流设计

### 6.1 基础规则判定流程

1. 接收 `recordId`
2. 查询 `attendanceRecord`
3. 查询当前启用中的 `rule`
4. 基于记录执行最小规则集判定：
   - 上班超过阈值判 `LATE`
   - 下班早于阈值判 `EARLY_LEAVE`
   - 短时间内重复上班打卡判 `REPEAT_CHECK`
   - 非法时间段打卡判 `ILLEGAL_TIME`
5. 若未命中，返回 `data=null`
6. 若已存在同 `recordId + sourceType=RULE` 的异常记录，直接返回已有结果
7. 若首次命中，则插入 `attendanceException`
8. 写入 `decisionTrace`，记录规则结果与最终决策
9. 将对应 `attendanceRecord.status` 更新为 `ABNORMAL`
10. 返回最小异常决策结果

### 6.2 复杂异常分析流程

1. 接收 `recordId + userId + riskFeatures`
2. 查询 `attendanceRecord`
3. 校验 `recordId` 对应 `userId` 与请求一致
4. 读取设备信息、历史异常次数、近期设备切换和地点变化情况
5. 选择启用中的 `promptTemplate(sceneType=EXCEPTION_ANALYSIS)`
6. 生成脱敏后的 `inputSummary`
7. 通过 `ModelGateway` 执行结构化分析
8. 成功时：
   - 写 `attendanceException(sourceType=MODEL)`
   - 写 `exceptionAnalysis`
   - 写 `modelCallLog(status=SUCCESS)`
   - 写 `decisionTrace`
   - 更新 `attendanceRecord.status=ABNORMAL`
9. 失败时：
   - 记录 `modelCallLog(status=FAILED, errorMessage=...)`
   - 生成保守降级异常记录 `sourceType=MODEL_FALLBACK`
   - 写最小 `decisionTrace`
   - 仍返回结构化结果，等待下游人工处理

### 6.3 查询流程

- `exception/list`：按条件查询 `attendanceException`，再映射为分页 VO
- `exception/{id}`：直接读取异常主记录
- `analysis-brief`：按 `exceptionId` 读取最新 `exceptionAnalysis`
- `decision-trace`：按 `businessType + businessId` 顺序读取全部决策链

## 7. 与 BE-01/02/03/04 的衔接设计

### 7.1 与 BE-01 的衔接

- 继续复用现有 Bearer Token 认证
- 继续通过 `AuthUser.roleCode` 判断是否为管理员
- 默认不改变全局鉴权架构，仅为 `BE-05` 新接口补充必要的安全路径配置

### 7.2 与 BE-02 的衔接

- 复用 `device.id == deviceId` 契约
- 复杂分析读取设备位置和状态信息时，直接使用现有 `device` 表，不重新建立设备快照表

### 7.3 与 BE-03 的衔接

- 复用 `attendanceRecord.faceScore`
- 不重新调用 `POST /api/face/verify`
- 不重新设计 `threshold` 或失败验证记录契约

### 7.4 与 BE-04 的衔接

- `attendanceRecord.id` 直接映射为 `recordId`
- `attendanceRecord.status` 由 `NORMAL` 向 `ABNORMAL` 联动更新
- 打卡记录查询与补卡申请不属于 `BE-05` 处理职责，但可作为复杂分析的辅助历史特征来源

## 8. 错误处理与安全设计

### 8.1 错误处理

- 参数校验失败：抛 `BusinessException(400, message)`
- 畸形 JSON：沿用全局异常处理返回 `请求参数错误`
- 记录、规则、模板或异常不存在：返回 `code=400`
- 模型调用异常：不直接向前端抛失败，改为记录失败日志并返回降级结果

### 8.2 安全与脱敏

- `BE-05` 所有公开接口默认仅允许管理员访问
- 模型输入以 `inputSummary` 摘要化保存，不直接落原始图片、人脸特征原文或不必要敏感信息
- `modelCallLog.outputSummary` 只保存结构化摘要，不保存完整敏感上下文

## 9. 最小测试策略

### 9.1 测试方式

优先使用接口级集成测试覆盖 `BE-05` 主链路：

- `@SpringBootTest`
- `@AutoConfigureMockMvc`
- `@ActiveProfiles("test")`
- `JdbcTemplate` 准备测试数据

### 9.2 需要补齐的测试基线

- 在 `src/test/resources/schema.sql` 中补充：
  - `rule`
  - `attendanceException`
  - `exceptionAnalysis`
  - `promptTemplate`
  - `modelCallLog`
  - `decisionTrace`
- 补充与 `BE-05` 直接相关的最小种子数据

### 9.3 核心通过用例

- 规则列表查询成功
- 规则新增、修改、状态切换成功
- `rule-check` 命中迟到并落库成功
- `rule-check` 未命中时返回空结果
- `complex-check` 成功生成复杂异常、分析结果、模型日志和决策链
- `exception/list`、`detail`、`analysis-brief`、`decision-trace` 查询成功

### 9.4 核心失败与降级用例

- `recordId` 不存在
- `recordId` 与 `userId` 不匹配
- 无启用规则
- 无启用模板
- 模型调用失败时走 `MODEL_FALLBACK`
- 未登录访问 `BE-05` 接口返回 `401`
- 员工访问 `BE-05` 接口返回 `403`

### 9.5 最小验证命令

实现完成后，仅执行与 `BE-05` 直接相关的最小验证：

```bash
mvn -Dtest=ExceptionControllerTest,RuleControllerTest test
```

## 10. 风险与取舍

- `ABSENT` 缺勤自动判定不进入本轮 MVP，避免越界引入排班或定时扫描机制。
- 当前 `faceScore` 粒度有限，复杂分析要以设备、地点、历史异常和组合行为为主特征，不能过度依赖低分值语义。
- 模型调用本轮只要求形成可插拔网关和可验证的 mock/fallback 闭环，不以真实第三方联通为完成条件。
- 旧 worktree 中更大范围的 `prompt/log` 管理接口和额外支撑能力不属于本轮交付范围，避免边界漂移。
