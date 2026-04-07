# 本地运行时与真实库修复记录（2026-04-08）

## 1. 本地运行时口径

- 后端实际启动命令口径：`javaw -Dlogging.file.name=backend-live.log -Dspring.redis.host=127.0.0.1 -Dspring.redis.password= -jar target/attendance-backend-0.0.1-SNAPSHOT.jar`
- 原因：当前环境只提供 `DB_*` 变量；`application-dev.yml` 中 Redis 默认仍回落到 `host=change_me`、`password=change_me`，导致登录成功后的 token 持久化首次触 Redis 即返回 `500`
- 验证：补齐上述运行时覆盖后，`POST /api/auth/login` 恢复为 `code=200`

## 2. 真实库修复前诊断证据

- `DEVICE_COLUMNS=[id:varchar(64), name:varchar(100), location:varchar(255), status:tinyint, description:varchar(255)]`
- `ATTENDANCE_RECORD_COLUMNS=[id:bigint, userId:bigint, checkTime:datetime, checkType:varchar(20), deviceId:varchar(64), ipAddr:varchar(64), location:varchar(255), faceScore:decimal(5,2), status:varchar(20), createTime:datetime]`
- `promptTemplate` 缺表：`Table 'system.prompttemplate' doesn't exist`
- `REVIEW_RECORD_COLUMNS=[id:bigint, exceptionId:bigint, reviewUserId:bigint, result:varchar(20), comment:varchar(255), reviewTime:datetime]`

## 3. 本轮确认并执行的最小真实库修复

- 为 `device` 补 `longitude`、`latitude`
- 为 `attendanceRecord` 补 `longitude`、`latitude`
- 新建 `promptTemplate` 表，并补 `8001/8002/8003` 三条默认模板
- 为 `reviewRecord` 补 `aiReviewSuggestion`、`similarCaseSummary`、`feedbackTag`、`strategyFeedback`

## 4. 修复后复验证据

- `DEVICE_COLUMNS=[id:varchar(64), name:varchar(100), location:varchar(255), longitude:decimal(10,6), latitude:decimal(10,6), status:tinyint, description:varchar(255)]`
- `ATTENDANCE_RECORD_COLUMNS=[id:bigint, userId:bigint, checkTime:datetime, checkType:varchar(20), deviceId:varchar(64), ipAddr:varchar(64), location:varchar(255), longitude:decimal(10,6), latitude:decimal(10,6), faceScore:decimal(5,2), status:varchar(20), createTime:datetime]`
- `PROMPT_TEMPLATE_COLUMNS=[id:bigint, code:varchar(50), name:varchar(100), sceneType:varchar(50), version:varchar(50), content:text, status:varchar(20), remark:varchar(255), createTime:datetime, updateTime:datetime]`
- `REVIEW_RECORD_COLUMNS=[id:bigint, exceptionId:bigint, reviewUserId:bigint, result:varchar(20), comment:varchar(255), aiReviewSuggestion:text, similarCaseSummary:text, feedbackTag:varchar(50), strategyFeedback:varchar(255), reviewTime:datetime]`
- `ENABLED_EXCEPTION_ANALYSIS_COUNT=1`

## 5. 本轮结论

- 本轮 `EMPLOYEE` 全链补证依赖于上述本地运行时覆盖与真实库最小修复。
- 这些修复证明当前真实本地环境与仓库 schema/配置口径仍存在漂移，后续结论必须在状态文档中显式登记为风险，而不能直接忽略。
