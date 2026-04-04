# 考勤异常检测与预警系统 API 接口设计文档

## 1. 接口设计规范
- 接口风格：以 RESTful 为主，结合毕设场景采用 `/list`、`/add`、`/update`、`/submit` 等工程化路径
- 数据格式：JSON
- 编码格式：UTF-8
- 鉴权方式：Token 或 JWT
- 持久化数据库：`system`
- 请求与响应字段采用 camelCase 风格，与后端对象保持一致，如 `realName`、`deptId`、`createTime`

核心落库表：
- `user`、`department`、`role`
- `device`、`faceFeature`、`rule`
- `attendanceRecord`、`attendanceException`、`exceptionAnalysis`
- `warningRecord`、`reviewRecord`、`operationLog`

统一响应格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

列表查询统一约定：
- 列表类接口默认支持 `pageNum`、`pageSize` 两个分页参数。
- 查询类接口可按业务补充 `keyword`、`status`、`userId`、`deptId`、`startDate`、`endDate` 等过滤参数。
- 列表类接口返回结构默认采用：`data.total` + `data.records`。

分页响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 2,
    "records": [
      {
        "id": 1
      }
    ]
  }
}
```

## 2. 认证接口

### 2.1 用户登录
- 路径：`POST /api/auth/login`

请求示例：

```json
{
  "username": "admin",
  "password": "123456"
}
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "xxxx",
    "roleCode": "ADMIN"
  }
}
```

## 3. 用户与部门管理接口

### 3.1 查询员工列表
- 路径：`GET /api/user/list`

查询参数建议：
- `pageNum`：页码
- `pageSize`：每页条数
- `keyword`：按用户名或姓名模糊查询
- `deptId`：按部门筛选
- `status`：按账号状态筛选

返回字段要点：
- `id`
- `username`
- `realName`
- `phone`
- `deptId`
- `roleId`
- `status`
- `createTime`

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1001,
      "username": "zhangsan",
      "realName": "张三",
      "gender": "男",
      "phone": "13800000000",
      "deptId": 2,
      "roleId": 2,
      "status": 1,
      "createTime": "2026-04-01T09:00:00"
    }
  ]
}
```

### 3.2 新增员工
- 路径：`POST /api/user/add`

```json
{
  "username": "wangwu",
  "password": "123456",
  "realName": "王五",
  "gender": "男",
  "phone": "13800000001",
  "deptId": 1,
  "roleId": 2,
  "status": 1
}
```

### 3.3 修改员工
- 路径：`PUT /api/user/update`

- 说明：`password` 为可选字段；未传或为空白时保留原密码，传入有效值时按新密码更新。

```json
{
  "id": 1001,
  "username": "zhangsan",
  "password": "654321",
  "realName": "张三",
  "gender": "男",
  "phone": "13800000000",
  "deptId": 2,
  "roleId": 2,
  "status": 1
}
```

### 3.4 删除员工
- 路径：`DELETE /api/user/{id}`

### 3.5 查询部门列表
- 路径：`GET /api/department/list`
- 查询参数：`keyword`，可选，用于按部门名称模糊查询

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "研发部",
      "description": "负责系统研发"
    }
  ]
}
```

### 3.6 新增部门
- 路径：`POST /api/department/add`

```json
{
  "name": "财务部",
  "description": "负责财务管理"
}
```

### 3.7 修改部门
- 路径：`PUT /api/department/update`

```json
{
  "id": 2,
  "name": "人力资源部",
  "description": "负责人力资源与培训"
}
```

### 3.8 删除部门
- 路径：`DELETE /api/department/{id}`
- 业务约束：当部门仍被 `user.deptId` 引用时，返回 `code=400`，消息为 `部门下存在关联用户，不能删除`

### 3.9 查询设备列表
- 路径：`GET /api/device/list`
- 查询参数：
  - `keyword`：可选，按设备编号、设备名称、地点模糊查询
  - `status`：可选，按状态筛选，`1` 为启用，`0` 为停用

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "deviceId": "DEV-001",
      "name": "前台考勤机1",
      "location": "办公区A",
      "status": 1,
      "description": "默认正常设备"
    }
  ]
}
```

### 3.10 新增设备
- 路径：`POST /api/device/add`
- 业务约束：
  - `deviceId` 必填且唯一
  - `name` 必填
  - `status` 为空时默认写入 `1`
  - `status` 仅允许 `0` 或 `1`

```json
{
  "deviceId": "DEV-010",
  "name": "后门考勤机",
  "location": "办公区C",
  "description": "新增设备"
}
```

### 3.11 修改设备
- 路径：`PUT /api/device/update`
- 业务约束：
  - `deviceId` 必填，接口通过该字段定位已有设备
  - 本接口不提供设备编号变更能力
  - `name` 必填
  - `status` 如传入，仅允许 `0` 或 `1`

```json
{
  "deviceId": "DEV-002",
  "name": "二号考勤机",
  "location": "办公区B-北侧",
  "status": 0,
  "description": "调整位置"
}
```

### 3.12 修改设备状态
- 路径：`PUT /api/device/status`
- 业务约束：
  - `deviceId` 必填
  - `status` 必填，且仅允许 `0` 或 `1`

```json
{
  "deviceId": "DEV-009",
  "status": 1
}
```

### 3.13 删除设备
- 路径：`DELETE /api/device/{deviceId}`
- 业务约束：
  - 设备不存在时返回 `code=400`，消息为 `设备不存在`
  - 设备已被 `attendanceRecord.deviceId` 引用时，返回 `code=400`，消息为 `设备已关联打卡记录，不能删除，请先停用设备`

## 4. 人脸与打卡接口

### 4.1 人脸录入
- 路径：`POST /api/face/register`
- 权限：`ADMIN`、`EMPLOYEE`
- 业务约束：
  - 当前登录用户自助录入，目标用户由当前会话解析
  - 请求体中的 `userId` 非必填；若传入，后端忽略并覆盖为当前会话用户编号
  - `imageData` 必填
  - 同一员工重复录入时新增一条 `faceFeature` 记录，验证始终只取最新一条模板
  - 不直接返回原始特征内容

```json
{
  "imageData": "base64..."
}
```

响应字段要点：
- `userId`：当前会话解析出的用户编号
- `registered`
- `message`
- `createTime`

### 4.2 人脸验证
- 路径：`POST /api/face/verify`
- 权限：`ADMIN`、`EMPLOYEE`
- 业务约束：
  - 当前登录用户自助验证，目标用户由当前会话解析
  - 请求体中的 `userId` 非必填；若传入，后端忽略并覆盖为当前会话用户编号
  - `imageData` 必填
  - 未录入人脸时返回 `registered=false`
  - 验证失败时返回 `matched=false`

```json
{
  "imageData": "base64..."
}
```

响应字段要点：
- `userId`：当前会话解析出的用户编号
- `registered`
- `matched`
- `faceScore`
- `threshold`
- `message`

### 4.3 提交打卡
- 路径：`POST /api/attendance/checkin`
- 业务约束：
  - 当前登录用户自助打卡，目标用户由当前会话解析
  - 请求体中的 `userId` 非必填；若传入，后端忽略并覆盖为当前会话用户编号
  - `checkType`、`deviceId`、`imageData` 必填
  - `ipAddr` 由后端按 request 地址解析，不由前端传入
  - `location` 由后端按 `deviceId` 回填为设备位置快照
  - `checkType` 仅允许 `IN` 或 `OUT`
  - 打卡时直接复用 `POST /api/face/verify` 的自助验证结果
  - 设备停用时返回 `code=400`，消息为 `设备已停用，不能打卡`
  - 未录入人脸或验证失败时返回人脸模块已有提示，不生成打卡记录

```json
{
  "checkType": "IN",
  "deviceId": "DEV-001",
  "imageData": "base64..."
}
```

响应字段要点：
- `recordId`
- `userId`
- `checkTime`
- `checkType`
- `deviceId`
- `location`
- `faceScore`
- `threshold`
- `status`
- `message`

## 5. 异常检测接口

### 5.1 基础规则判定
- 路径：`POST /api/exception/rule-check`

### 5.2 复杂异常分析
- 路径：`POST /api/exception/complex-check`

```json
{
  "recordId": 5001,
  "userId": 1001,
  "riskFeatures": {
    "faceScore": 83.5,
    "deviceChanged": true,
    "locationChanged": false,
    "historyAbnormalCount": 3
  }
}
```

### 5.3 查询异常列表
- 路径：`GET /api/exception/list`

查询参数建议：
- `pageNum`
- `pageSize`
- `type`
- `riskLevel`
- `processStatus`
- `userId`

返回字段要点：
- `id`
- `recordId`
- `userId`
- `type`
- `riskLevel`
- `sourceType`
- `processStatus`
- `createTime`

### 5.4 查询异常详情
- 路径：`GET /api/exception/{id}`

### 5.5 查询异常决策链
- 路径：`GET /api/exception/{id}/decision-trace`

返回字段要点：
- `businessType`
- `businessId`
- `ruleResult`
- `modelResult`
- `finalDecision`
- `confidenceScore`
- `decisionReason`

### 5.6 查询异常分析摘要
- 路径：`GET /api/exception/{id}/analysis-brief`

返回字段要点：
- `modelConclusion`
- `reasonSummary`
- `actionSuggestion`
- `similarCaseSummary`
- `promptVersion`

## 6. 预警与复核接口

### 6.1 查询预警列表
- 路径：`GET /api/warning/list`

查询参数建议：
- `pageNum`
- `pageSize`
- `level`
- `status`
- `type`

返回字段要点：
- `id`
- `exceptionId`
- `type`
- `level`
- `status`
- `priorityScore`
- `aiSummary`
- `disposeSuggestion`
- `sendTime`

### 6.2 查询预警建议
- 路径：`GET /api/warning/{id}/advice`

返回字段要点：
- `id`
- `exceptionId`
- `priorityScore`
- `aiSummary`
- `disposeSuggestion`
- `decisionSource`

### 6.3 重新评估预警优先级
- 路径：`POST /api/warning/re-evaluate`

```json
{
  "warningId": 5001,
  "reason": "规则已更新，需要按最新策略重新评估"
}
```

### 6.4 提交复核结果
- 路径：`POST /api/review/submit`

```json
{
  "exceptionId": 9001,
  "reviewResult": "CONFIRMED",
  "reviewComment": "确认存在代打卡风险"
}
```

返回字段要点：
- `id`
- `exceptionId`
- `reviewUserId`
- `reviewResult`
- `reviewComment`
- `aiReviewSuggestion`
- `similarCaseSummary`
- `reviewTime`

### 6.5 查询复核记录
- 路径：`GET /api/review/{exceptionId}`

说明：返回该异常的最新一条复核记录；若该异常尚无复核记录，则返回体中的 `data` 字段为空或省略。

返回字段要点：
- `id`
- `exceptionId`
- `reviewUserId`
- `reviewResult`
- `reviewComment`
- `aiReviewSuggestion`
- `similarCaseSummary`
- `feedbackTag`
- `strategyFeedback`
- `reviewTime`

### 6.6 查询 AI 复核辅助信息
- 路径：`GET /api/review/{exceptionId}/assistant`

返回字段要点：
- `aiReviewSuggestion`
- `similarCaseSummary`
- `decisionReason`
- `confidenceScore`

### 6.7 提交复核反馈标签
- 路径：`POST /api/review/feedback`

```json
{
  "reviewId": 6001,
  "feedbackTag": "CONFIRMED_EFFECTIVE",
  "strategyFeedback": "建议保留当前提示词模板并提高设备异常权重"
}
```

说明：该接口仅更新既有复核记录上的 `feedbackTag` 与 `strategyFeedback`，不回写预警表。

## 7. 统计分析接口

### 7.1 个人统计
- 路径：`GET /api/statistics/personal`

查询参数建议：
- `startDate`
- `endDate`

业务约束：
- 按当前登录会话推导统计用户，前端不传 `userId`
- 管理员返回本人个人统计
- 员工返回本人个人统计
- 统计时间锚点统一使用 `attendanceRecord.checkTime`

返回字段要点：
- `userId`
- `realName`
- `deptId`
- `recordCount`
- `exceptionCount`
- `analysisCount`
- `warningCount`
- `reviewCount`
- `closedLoopCount`
- `exceptionTypeDistribution`
- `riskLevelDistribution`
- `warningStatusDistribution`
- `reviewResultDistribution`

### 7.2 部门统计
- 路径：`GET /api/statistics/department`

查询参数建议：
- `startDate`
- `endDate`

业务约束：
- 仅 `ADMIN` 可访问
- 默认统计范围为全局所有部门汇总，前端不传 `deptId`
- 返回中的 `deptName` 固定为 `全部部门`

返回字段要点：
- `deptId`
- `deptName`
- `recordCount`
- `exceptionCount`
- `analysisCount`
- `warningCount`
- `reviewCount`
- `closedLoopCount`
- `exceptionTypeDistribution`
- `riskLevelDistribution`
- `warningStatusDistribution`
- `reviewResultDistribution`

### 7.3 异常趋势统计
- 路径：`GET /api/statistics/exception-trend`

查询参数建议：
- `deptId`
- `startDate`
- `endDate`
- `periodType`：`DAY`、`WEEK`、`MONTH`

返回字段要点：
- `periodType`
- `points[].date`
- `points[].recordCount`
- `points[].exceptionCount`
- `points[].analysisCount`
- `points[].warningCount`
- `points[].reviewCount`
- `points[].closedLoopCount`

### 7.4 查询统计摘要
- 路径：`GET /api/statistics/summary`

查询参数建议：
- `startDate`
- `endDate`
- `periodType`

返回字段要点：
- `periodType`
- `summary`
- `highlightRisks`
- `manageSuggestion`

口径说明：
- `ADMIN` 查看全局摘要
- `EMPLOYEE` 查看本人个人摘要
- `summary` 基于打卡量、异常量、预警量、复核量和闭环量生成
- `highlightRisks` 需体现高风险异常、未处理预警、未闭环异常和审计缺口
- 审计缺口评估需纳入 `modelCallLog`、`decisionTrace`

### 7.5 查询部门风险画像摘要
- 路径：`GET /api/statistics/department-risk-brief`

查询参数建议：
- `deptId`
- `startDate`
- `endDate`

返回字段要点：
- `deptId`
- `deptName`
- `riskScore`
- `riskSummary`
- `manageSuggestion`

口径说明：
- `riskScore` 基于异常率、高风险占比、未处理预警占比和闭环缺口综合计算

### 7.6 报表导出
- 路径：`GET /api/statistics/export`

查询参数建议：
- `exportType`：`PERSONAL`、`DEPARTMENT`、`TREND`、`AUDIT`
- `type`
- `pageNum`
- `pageSize`
- `startDate`
- `endDate`
- `periodType`

返回说明：
- 返回 `text/csv;charset=UTF-8` 文件下载响应
- 响应头包含 `Content-Disposition: attachment`
- 本轮不默认放开员工导出权限
- `PERSONAL` 导出个人闭环统计
- `DEPARTMENT` 导出全局部门汇总统计
- `TREND` 导出趋势点位统计
- `AUDIT` 导出操作日志查询结果
- `AUDIT` 导出保留“导出全量筛选结果”语义，但单次导出上限为 `5000` 条，超限时返回 `code=400`

## 8. 组织与权限管理接口

### 8.1 查询部门列表
- 路径：`GET /api/department/list`

查询参数建议：
- `pageNum`
- `pageSize`
- `keyword`

返回字段要点：
- `id`
- `name`
- `description`

### 8.2 新增部门
- 路径：`POST /api/department/add`

```json
{
  "name": "研发部",
  "description": "负责系统开发与维护"
}
```

### 8.3 修改部门
- 路径：`PUT /api/department/update`

```json
{
  "id": 1,
  "name": "研发一部",
  "description": "负责后端服务开发"
}
```

### 8.4 删除部门
- 路径：`DELETE /api/department/{id}`

### 8.5 查询角色列表
- 路径：`GET /api/role/list`

查询参数建议：
- `pageNum`
- `pageSize`
- `keyword`
- `status`

返回字段要点：
- `id`
- `code`
- `name`
- `description`
- `status`

### 8.6 新增角色
- 路径：`POST /api/role/add`

```json
{
  "code": "HR",
  "name": "人事专员",
  "description": "负责人事与考勤管理",
  "status": 1
}
```

### 8.7 修改角色
- 路径：`PUT /api/role/update`

```json
{
  "id": 2,
  "code": "HR",
  "name": "人事主管",
  "description": "负责人事、考勤与复核管理",
  "status": 1
}
```

### 8.8 删除角色
- 路径：`DELETE /api/role/{id}`

## 9. 设备与考勤记录接口

说明：本节补充设备管理、考勤记录查询与补卡接口，与 `4.3 提交打卡` 共同构成完整的考勤接口组。

设备编号字段约定：接口层统一使用 `deviceId` 表示设备编号；数据库 `device` 表主键字段当前命名为 `id`，实现层需完成 `id -> deviceId` 映射，对前后端契约保持 `deviceId` 不变。

### 9.1 查询设备列表
- 路径：`GET /api/device/list`

查询参数建议：
- `pageNum`
- `pageSize`
- `keyword`
- `status`

返回字段要点：
- `deviceId`
- `name`
- `location`
- `status`
- `description`

### 9.2 新增设备
- 路径：`POST /api/device/add`

```json
{
  "deviceId": "DEV-002",
  "name": "前台考勤机",
  "location": "一楼前台"
}
```

### 9.3 修改设备
- 路径：`PUT /api/device/update`

```json
{
  "deviceId": "DEV-002",
  "name": "前台考勤机",
  "location": "一楼大厅",
  "description": "访客区考勤设备"
}
```

### 9.4 更新设备状态
- 路径：`PUT /api/device/status`

```json
{
  "deviceId": "DEV-002",
  "status": 1
}
```

### 9.5 删除设备
- 路径：`DELETE /api/device/{deviceId}`

### 9.6 查询自助打卡设备列表
- 路径：`GET /api/attendance/device-options`
- 业务约束：
  - `FE-05` 通过该接口获取可选打卡设备来源
  - 仅返回 `status=1` 的启用设备
  - 该接口不复用 `GET /api/device/list` 的管理语义

返回字段要点：
- `deviceId`
- `name`
- `location`

### 9.7 查询个人考勤记录
- 路径：`GET /api/attendance/record/me`
- 业务约束：
  - `FE-05` 通过该接口查询当前登录用户个人记录
  - 当前登录用户由当前会话解析，不依赖登录响应额外提供 `userId`
  - 保留 `GET /api/attendance/record/{userId}` 作为兼容/管理接口，不作为 `FE-05` 自助入口

查询参数建议：
- `pageNum`
- `pageSize`
- `startDate`
- `endDate`

返回字段要点：
- `id`
- `userId`
- `checkTime`
- `checkType`
- `deviceId`
- `location`
- `faceScore`
- `status`

### 9.8 查询考勤记录列表
- 路径：`GET /api/attendance/list`
- 业务约束：仅管理员可访问

查询参数建议：
- `pageNum`
- `pageSize`
- `userId`
- `deptId`
- `checkType`
- `status`
- `startDate`
- `endDate`

返回字段要点：
- `id`
- `userId`
- `realName`
- `checkTime`
- `checkType`
- `deviceId`
- `location`
- `faceScore`
- `status`

### 9.9 提交补卡申请
- 路径：`POST /api/attendance/repair`
- 业务约束：
  - 当前登录用户自助提交补卡申请，目标用户由当前会话解析
  - 请求体中的 `userId` 非必填；若传入，后端忽略并覆盖为当前会话用户编号
  - 成功提交后生成一条 `status=PENDING` 的补卡申请记录
  - 若同一用户、同一补卡类型、同一补卡时间已存在待处理申请，则返回 `code=400`，消息为 `补卡申请已存在，请勿重复提交`
  - 若同一时间点已存在正式打卡记录，则返回 `code=400`，消息为 `该时间点已存在打卡记录，无需补卡`

```json
{
  "checkType": "IN",
  "checkTime": "2026-03-31 09:05:00",
  "repairReason": "设备故障未成功打卡"
}
```

## 10. 系统配置与审计接口

说明：本节统一补充规则管理、风险等级配置、异常类型配置与操作日志查询；其中 `GET /api/log/operation/list` 为系统审计查询接口，前端可挂在系统配置页的日志子页中使用。

### 10.1 查询考勤规则列表
- 路径：`GET /api/rule/list`

查询参数建议：
- `pageNum`
- `pageSize`
- `keyword`
- `status`

返回字段要点：
- `id`
- `name`
- `startTime`
- `endTime`
- `lateThreshold`
- `earlyThreshold`
- `repeatLimit`
- `status`

### 10.2 新增考勤规则
- 路径：`POST /api/rule/add`

```json
{
  "name": "标准工作日规则",
  "startTime": "09:00:00",
  "endTime": "18:00:00",
  "lateThreshold": 10,
  "earlyThreshold": 10,
  "repeatLimit": 3
}
```

### 10.3 修改考勤规则
- 路径：`PUT /api/rule/update`

```json
{
  "id": 1,
  "name": "标准工作日规则",
  "startTime": "09:00:00",
  "endTime": "18:00:00",
  "lateThreshold": 15,
  "earlyThreshold": 10,
  "repeatLimit": 3
}
```

### 10.4 更新考勤规则状态
- 路径：`PUT /api/rule/status`

```json
{
  "id": 1,
  "status": 1
}
```

### 10.5 查询风险等级配置
- 路径：`GET /api/system/risk-level/list`

查询参数建议：
- `pageNum`
- `pageSize`
- `status`

返回字段要点：
- `code`
- `name`
- `description`
- `status`

### 10.6 更新风险等级配置
- 路径：`PUT /api/system/risk-level/update`

```json
{
  "code": "HIGH",
  "name": "高风险",
  "description": "需要优先人工复核",
  "status": 1
}
```

### 10.7 查询异常类型配置
- 路径：`GET /api/system/exception-type/list`

查询参数建议：
- `pageNum`
- `pageSize`
- `status`

返回字段要点：
- `code`
- `name`
- `description`
- `status`

### 10.8 更新异常类型配置
- 路径：`PUT /api/system/exception-type/update`

```json
{
  "code": "MULTI_DEVICE",
  "name": "多设备异常打卡",
  "description": "同一员工短时间内跨设备打卡",
  "status": 1
}
```

### 10.9 查询操作日志
- 路径：`GET /api/log/operation/list`

查询参数建议：
- `pageNum`
- `pageSize`
- `userId`
- `type`
- `startDate`
- `endDate`

返回字段要点：
- `id`
- `userId`
- `type`
- `content`
- `operationTime`

口径说明：
- 本接口查询 `operationLog`，用于用户操作审计
- 当前最小纳入日志的关键成功路径包括：登录、打卡、补卡申请、预警重评估、复核提交、复核反馈

### 10.10 查询提示词模板列表
- 路径：`GET /api/system/prompt/list`

查询参数建议：
- `pageNum`
- `pageSize`
- `sceneType`
- `status`

返回字段要点：
- `id`
- `code`
- `name`
- `sceneType`
- `version`
- `status`
- `updateTime`

### 10.11 新增提示词模板
- 路径：`POST /api/system/prompt/add`

```json
{
  "code": "COMPLEX_EXCEPTION",
  "name": "复杂异常分析模板",
  "sceneType": "EXCEPTION_ANALYSIS",
  "version": "v1.1",
  "content": "请结合风险特征、历史行为和规则结果输出结构化结论、风险等级、理由摘要和处理建议。",
  "status": "ENABLED"
}
```

### 10.12 更新提示词模板
- 路径：`PUT /api/system/prompt/update`

### 10.13 更新提示词模板状态
- 路径：`PUT /api/system/prompt/status`

### 10.14 查询模型调用日志
- 路径：`GET /api/system/model-log/list`

查询参数建议：
- `pageNum`
- `pageSize`
- `businessType`
- `status`
- `startDate`
- `endDate`

返回字段要点：
- `id`
- `businessType`
- `businessId`
- `promptTemplateId`
- `status`
- `latencyMs`
- `createTime`

## 11. 错误码设计
- `200`：请求成功
- `400`：请求参数错误
- `401`：未授权
- `403`：无权限
- `404`：资源不存在
- `500`：系统内部错误
