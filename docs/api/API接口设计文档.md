# 考勤异常检测与预警系统 API 接口设计文档

## 1. 接口设计规范
- 接口风格：RESTful
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

## 3. 用户管理接口

### 3.1 查询员工列表
- 路径：`GET /api/user/list`

### 3.2 新增员工
- 路径：`POST /api/user/add`

```json
{
  "username": "zhangsan",
  "realName": "张三",
  "phone": "13800000000",
  "deptId": 1,
  "roleId": 2
}
```

### 3.3 修改员工
- 路径：`PUT /api/user/update`

### 3.4 删除员工
- 路径：`DELETE /api/user/{id}`

## 4. 人脸与打卡接口

### 4.1 人脸录入
- 路径：`POST /api/face/register`

```json
{
  "userId": 1001,
  "imageData": "base64..."
}
```

### 4.2 人脸验证
- 路径：`POST /api/face/verify`

### 4.3 提交打卡
- 路径：`POST /api/attendance/checkin`

```json
{
  "userId": 1001,
  "checkType": "IN",
  "deviceId": "DEV-001",
  "ipAddr": "192.168.1.10",
  "location": "办公区A"
}
```

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

### 5.4 查询异常详情
- 路径：`GET /api/exception/{id}`

## 6. 预警与复核接口

### 6.1 查询预警列表
- 路径：`GET /api/warning/list`

### 6.2 提交复核结果
- 路径：`POST /api/review/submit`

```json
{
  "exceptionId": 9001,
  "reviewResult": "CONFIRMED",
  "reviewComment": "确认存在代打卡风险"
}
```

### 6.3 查询复核记录
- 路径：`GET /api/review/{exceptionId}`

## 7. 统计分析接口

### 7.1 个人统计
- 路径：`GET /api/statistics/personal?userId=1001`

### 7.2 部门统计
- 路径：`GET /api/statistics/department?deptId=1`

### 7.3 异常趋势统计
- 路径：`GET /api/statistics/exception-trend`

### 7.4 报表导出
- 路径：`GET /api/statistics/export`

## 8. 组织与权限管理接口

### 8.1 查询部门列表
- 路径：`GET /api/department/list`

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

### 9.1 查询设备列表
- 路径：`GET /api/device/list`

### 9.2 新增设备
- 路径：`POST /api/device/add`

```json
{
  "id": "DEV-002",
  "name": "前台考勤机",
  "location": "一楼前台"
}
```

### 9.3 修改设备
- 路径：`PUT /api/device/update`

```json
{
  "id": "DEV-002",
  "name": "前台考勤机",
  "location": "一楼大厅",
  "description": "访客区考勤设备"
}
```

### 9.4 更新设备状态
- 路径：`PUT /api/device/status`

```json
{
  "id": "DEV-002",
  "status": 1
}
```

### 9.5 删除设备
- 路径：`DELETE /api/device/{id}`

### 9.6 查询个人考勤记录
- 路径：`GET /api/attendance/record/{userId}`

### 9.7 查询考勤记录列表
- 路径：`GET /api/attendance/list`

### 9.8 提交补卡申请
- 路径：`POST /api/attendance/repair`

```json
{
  "userId": 1001,
  "checkType": "IN",
  "checkTime": "2026-03-31 09:05:00",
  "repairReason": "设备故障未成功打卡"
}
```

## 10. 系统配置接口

### 10.1 查询考勤规则列表
- 路径：`GET /api/rule/list`

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

## 11. 错误码设计
- `200`：请求成功
- `400`：请求参数错误
- `401`：未授权
- `403`：无权限
- `404`：资源不存在
- `500`：系统内部错误
