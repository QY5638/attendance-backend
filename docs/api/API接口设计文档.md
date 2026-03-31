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

## 8. 错误码设计
- `200`：请求成功
- `400`：请求参数错误
- `401`：未授权
- `403`：无权限
- `404`：资源不存在
- `500`：系统内部错误
