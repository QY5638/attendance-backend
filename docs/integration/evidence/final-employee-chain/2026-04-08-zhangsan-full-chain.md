# EMPLOYEE 全链最终证据（2026-04-08）

## 对象与口径

- EMPLOYEE 账号：`zhangsan/123456`
- EMPLOYEE 用户 ID：`1001`
- 人脸样本：同一份 `imageData`
- 关联 ADMIN 复核账号：`admin/123456`
- 本证据只记录本轮最终闭环关键结果；本地运行时与真实库修复见同目录 `2026-04-08-local-runtime-and-schema.md`

## 关键结果

### 1. EMPLOYEE 登录

- 结果：`POST /api/auth/login => code=200`
- 关键信息：`roleCode=EMPLOYEE`

### 2. 人脸采集

- 结果：`POST /api/face/register => code=200`
- 关键信息：`userId=1001`，`registered=true`，`createTime=2026-04-08T00:54:16`

### 3. 人脸验证

- 结果：`POST /api/face/verify => code=200`
- 关键信息：`matched=true`，`faceScore=99.99`，`threshold=85.00`

### 4. 打卡

- 结果：`POST /api/attendance/checkin => code=200`
- 关键信息：`recordId=2041564164937326593`，`deviceId=DEV-002`，`checkType=IN`，`location=办公区B`，`status=ABNORMAL`

### 5. 基础异常

- 回查方式：`ADMIN` 侧 `GET /api/exception/list?pageNum=1&pageSize=10`
- 关键信息：`exceptionId=2041564165004435457`，`recordId=2041564164937326593`，`type=ILLEGAL_TIME`，`riskLevel=HIGH`，`sourceType=RULE`，`processStatus=PENDING`

### 6. 复杂异常

- 回查方式：`ADMIN` 侧 `GET /api/exception/list?pageNum=1&pageSize=10`
- 关键信息：`exceptionId=2041564165067350018`，`recordId=2041564164937326593`，`type=PROXY_CHECKIN`，`riskLevel=HIGH`，`sourceType=MODEL`，`processStatus=PENDING`

### 7. 预警

- 回查方式：`ADMIN` 侧 `GET /api/warning/list?pageNum=1&pageSize=10`
- 基础异常预警：`warningId=2041564165004435459`，`exceptionId=2041564165004435457`，`status=UNPROCESSED`
- 复杂异常预警：`warningId=2041564165067350022`，`exceptionId=2041564165067350018`，`status=UNPROCESSED`

### 8. 复核

- 操作：`ADMIN` 侧 `POST /api/review/submit`
- 结果：`code=200`
- 关键信息：`reviewId=2041565106919284738`，`exceptionId=2041564165067350018`，`reviewResult=CONFIRMED`

### 9. 预警状态回写

- 回查方式：`ADMIN` 侧 `GET /api/warning/list?pageNum=1&pageSize=10`
- 关键信息：复杂异常预警 `warningId=2041564165067350022` 已由 `UNPROCESSED` 回写为 `PROCESSED`

### 10. 统计

- 回查方式：`ADMIN` 侧 `GET /api/statistics/summary`
- 结果：`code=200`
- 摘要：`统计周期内共发现6次考勤记录异常5次，完成智能分析2次，生成预警5次，人工复核4次，形成闭环4次。`
- 风险提示：`高风险异常3次，未完成复核异常3次，未闭环异常1次，未处理预警1次。`

## 结论

- 本轮已补齐 `EMPLOYEE` 侧“人脸采集 -> 打卡 -> 基础异常 -> 复杂异常 -> 预警 -> 复核 -> 统计”最终证据。
- 本轮复杂异常与复核回写均基于同一条新打卡记录 `recordId=2041564164937326593` 闭环。
