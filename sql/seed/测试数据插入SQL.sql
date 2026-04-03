USE `system`;
SET NAMES utf8mb4;

-- 测试数据说明：
-- 1. 本文件依赖 `数据库建表SQL.sql` 已先执行成功。
-- 2. 本文件为演示和测试用途，采用固定主键，便于复现测试场景。
-- 3. 如需重复执行，可直接在全新数据库中重新导入建表脚本后再执行本文件。

-- 测试用户数据
INSERT IGNORE INTO `user` (`id`, `username`, `password`, `realName`, `gender`, `phone`, `deptId`, `roleId`, `status`, `createTime`) VALUES
(9001, 'admin', '$2a$10$DII2rUub7WSmcTFOa/4AtumHq9r3yDGwQ4gHW1pvyx51.dE.Abliu', '系统管理员', '男', '13800000001', 1, 1, 1, '2026-03-01 08:00:00'),
(1001, 'zhangsan', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '张三', '男', '13800000002', 2, 2, 1, '2026-03-01 08:10:00'),
(1002, 'lisi', '$2a$10$Cw71Sz28BSmh1fcOBJIAXOagYeMZjJRl6UEU4n8kQMGESv3RgL0SC', '李四', '女', '13800000003', 3, 2, 1, '2026-03-01 08:20:00');

-- 测试人脸特征数据
INSERT IGNORE INTO `faceFeature` (`id`, `userId`, `featureData`, `featureHash`, `encryptFlag`, `createTime`) VALUES
(1101, 1001, 'feature-data-zhangsan', 'hash-zhangsan-001', 1, '2026-03-01 09:00:00'),
(1102, 1002, 'feature-data-lisi', 'hash-lisi-001', 1, '2026-03-01 09:05:00'),
(1103, 9001, 'feature-data-admin', 'hash-admin-001', 1, '2026-03-01 09:10:00');

-- 测试打卡记录数据
INSERT IGNORE INTO `attendanceRecord` (`id`, `userId`, `checkTime`, `checkType`, `deviceId`, `ipAddr`, `location`, `faceScore`, `status`, `createTime`) VALUES
(2001, 1001, '2026-03-26 08:58:00', 'IN', 'DEV-001', '192.168.1.101', '办公区A', 96.50, 'NORMAL', '2026-03-26 08:58:00'),
(2002, 1001, '2026-03-26 18:02:00', 'OUT', 'DEV-001', '192.168.1.101', '办公区A', 97.10, 'NORMAL', '2026-03-26 18:02:00'),
(2003, 1002, '2026-03-26 08:58:30', 'IN', 'DEV-009', '10.10.10.9', '外部区域', 82.40, 'ABNORMAL', '2026-03-26 08:58:30'),
(2004, 1002, '2026-03-26 09:16:00', 'IN', 'DEV-002', '192.168.1.102', '办公区B', 95.20, 'ABNORMAL', '2026-03-26 09:16:00'),
(2005, 1001, '2026-03-26 09:00:00', 'IN', 'DEV-001', '192.168.1.101', '办公区A', 96.40, 'ABNORMAL', '2026-03-26 09:00:00');

-- 测试补卡申请数据
INSERT IGNORE INTO `attendanceRepair` (`id`, `userId`, `checkType`, `checkTime`, `repairReason`, `status`, `recordId`, `createTime`) VALUES
(2101, 1001, 'IN', '2026-03-27 09:03:00', '设备故障未成功打卡', 'PENDING', NULL, '2026-03-27 09:10:00');

-- 测试异常记录数据
INSERT IGNORE INTO `attendanceException` (`id`, `recordId`, `userId`, `type`, `riskLevel`, `sourceType`, `description`, `processStatus`, `createTime`) VALUES
(3001, 2003, 1002, 'PROXY_CHECKIN', 'HIGH', 'MODEL', '疑似代打卡，设备与地点异常且人脸分数偏低', 'REVIEWED', '2026-03-26 08:59:00'),
(3002, 2004, 1002, 'LATE', 'MEDIUM', 'RULE', '超过上班时间阈值，判定为迟到', 'PENDING', '2026-03-26 09:16:10'),
(3003, 2005, 1001, 'REPEAT_CHECK', 'MEDIUM', 'RULE', '短时间内重复打卡', 'PENDING', '2026-03-26 09:00:10');

-- 测试异常分析数据
INSERT IGNORE INTO `promptTemplate` (`id`, `code`, `name`, `sceneType`, `version`, `content`, `status`, `remark`, `createTime`, `updateTime`) VALUES
(8001, 'COMPLEX_EXCEPTION', '复杂异常分析模板', 'EXCEPTION_ANALYSIS', 'v1.0', '请基于输入摘要、风险特征和历史记录输出结构化结论、风险等级、判定依据与处理建议。', 'ENABLED', '默认复杂异常分析模板', '2026-03-20 10:00:00', '2026-03-20 10:00:00'),
(8002, 'WARNING_BRIEF', '预警摘要模板', 'WARNING_ADVICE', 'v1.0', '请根据异常结论生成预警摘要、优先级和处置建议。', 'ENABLED', '默认预警摘要模板', '2026-03-20 10:05:00', '2026-03-20 10:05:00'),
(8003, 'REVIEW_ASSISTANT', '复核辅助模板', 'REVIEW_ASSISTANT', 'v1.0', '请根据异常信息、历史记录和分析结果输出复核建议与相似案例摘要。', 'ENABLED', '默认复核辅助模板', '2026-03-20 10:10:00', '2026-03-20 10:10:00');

-- 测试异常分析数据
INSERT IGNORE INTO `exceptionAnalysis` (`id`, `exceptionId`, `promptTemplateId`, `inputSummary`, `modelResult`, `modelConclusion`, `confidenceScore`, `decisionReason`, `suggestion`, `reasonSummary`, `actionSuggestion`, `similarCaseSummary`, `promptVersion`, `createTime`) VALUES
(4001, 3001, 8001, '用户1002在异常设备DEV-009、外部区域打卡，人脸分数82.4', '疑似代打卡，建议进入高优先级复核', 'PROXY_CHECKIN', 92.50, '设备异常、地点异常、分数临界且与历史行为不一致', '建议管理员人工复核', '设备切换、地点异常与临界人脸分数共同提升了代打卡风险', '建议优先由管理员确认是否存在代打卡行为', '历史上存在相似的设备异常与低分值组合案例', 'v1.0', '2026-03-26 08:59:10');

-- 测试预警数据
INSERT IGNORE INTO `warningRecord` (`id`, `exceptionId`, `type`, `level`, `status`, `priorityScore`, `aiSummary`, `disposeSuggestion`, `decisionSource`, `sendTime`) VALUES
(5001, 3001, 'RISK_WARNING', 'HIGH', 'PROCESSED', 96.00, '该异常具备代打卡高风险特征，建议优先处理。', '建议管理员立即查看异常详情并发起复核。', 'MODEL_FUSION', '2026-03-26 08:59:20'),
(5002, 3002, 'ATTENDANCE_WARNING', 'MEDIUM', 'UNPROCESSED', 68.00, '该员工存在迟到行为，建议继续观察近期出勤情况。', '建议记录本次异常并结合历史记录决定是否升级。', 'RULE', '2026-03-26 09:16:20'),
(5003, 3003, 'ATTENDANCE_WARNING', 'MEDIUM', 'UNPROCESSED', 70.00, '该员工存在重复打卡行为，建议复查是否为误操作。', '建议查看同时间段原始打卡记录。', 'RULE', '2026-03-26 09:00:20');

-- 测试复核数据
INSERT IGNORE INTO `reviewRecord` (`id`, `exceptionId`, `reviewUserId`, `result`, `comment`, `aiReviewSuggestion`, `similarCaseSummary`, `feedbackTag`, `strategyFeedback`, `reviewTime`) VALUES
(6001, 3001, 9001, 'CONFIRMED', '确认存在代打卡风险，已记录处理', 'AI 建议优先确认设备异常和地点跨度是否合理，当前风险偏高。', '历史上同类外部设备+低分值场景中，多数最终被确认异常。', 'CONFIRMED_EFFECTIVE', '建议保留该提示词模板并提高设备异常权重。', '2026-03-26 09:10:00');

-- 测试模型调用日志数据
INSERT IGNORE INTO `modelCallLog` (`id`, `businessType`, `businessId`, `promptTemplateId`, `inputSummary`, `outputSummary`, `status`, `latencyMs`, `errorMessage`, `createTime`) VALUES
(9001, 'EXCEPTION_ANALYSIS', 3001, 8001, '用户1002异常设备打卡摘要', '输出疑似代打卡、高风险、建议人工复核', 'SUCCESS', 1260, NULL, '2026-03-26 08:59:08'),
(9002, 'WARNING_ADVICE', 5001, 8002, '高风险异常预警生成摘要', '输出高优先级预警摘要和处置建议', 'SUCCESS', 880, NULL, '2026-03-26 08:59:18'),
(9003, 'REVIEW_ASSISTANT', 3001, 8003, '异常3001复核辅助摘要', '输出复核建议与相似案例摘要', 'SUCCESS', 940, NULL, '2026-03-26 09:05:00');

-- 测试决策追踪数据
INSERT IGNORE INTO `decisionTrace` (`id`, `businessType`, `businessId`, `ruleResult`, `modelResult`, `finalDecision`, `confidenceScore`, `decisionReason`, `createTime`) VALUES
(9501, 'ATTENDANCE_EXCEPTION', 3001, '规则层无法直接定性，仅命中设备异常和低分值特征', '模型判断为疑似代打卡，高风险，建议人工复核', '最终标记为高风险复杂异常并生成预警', 92.50, '规则特征与模型结论一致，提升至高风险并进入复核。', '2026-03-26 08:59:12');

-- 测试操作日志数据
INSERT IGNORE INTO `operationLog` (`id`, `userId`, `type`, `content`, `operationTime`) VALUES
(7001, 9001, 'LOGIN', '管理员登录系统', '2026-03-26 08:00:00'),
(7002, 1001, 'CHECKIN', '张三完成上班打卡', '2026-03-26 08:58:00'),
(7003, 1002, 'CHECKIN', '李四在异常设备完成打卡', '2026-03-26 08:58:30'),
(7004, 9001, 'REVIEW', '管理员复核异常记录3001', '2026-03-26 09:10:00');
