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

-- 测试异常记录数据
INSERT IGNORE INTO `attendanceException` (`id`, `recordId`, `userId`, `type`, `riskLevel`, `sourceType`, `description`, `processStatus`, `createTime`) VALUES
(3001, 2003, 1002, 'PROXY_CHECKIN', 'HIGH', 'MODEL', '疑似代打卡，设备与地点异常且人脸分数偏低', 'REVIEWED', '2026-03-26 08:59:00'),
(3002, 2004, 1002, 'LATE', 'MEDIUM', 'RULE', '超过上班时间阈值，判定为迟到', 'PENDING', '2026-03-26 09:16:10'),
(3003, 2005, 1001, 'REPEAT_CHECK', 'MEDIUM', 'RULE', '短时间内重复打卡', 'PENDING', '2026-03-26 09:00:10');

-- 测试异常分析数据
INSERT IGNORE INTO `exceptionAnalysis` (`id`, `exceptionId`, `inputSummary`, `modelResult`, `confidenceScore`, `decisionReason`, `suggestion`, `createTime`) VALUES
(4001, 3001, '用户1002在异常设备DEV-009、外部区域打卡，人脸分数82.4', '疑似代打卡', 92.50, '设备异常、地点异常、分数临界且与历史行为不一致', '建议管理员人工复核', '2026-03-26 08:59:10');

-- 测试预警数据
INSERT IGNORE INTO `warningRecord` (`id`, `exceptionId`, `type`, `level`, `status`, `sendTime`) VALUES
(5001, 3001, 'RISK_WARNING', 'HIGH', 'PROCESSED', '2026-03-26 08:59:20'),
(5002, 3002, 'ATTENDANCE_WARNING', 'MEDIUM', 'UNPROCESSED', '2026-03-26 09:16:20'),
(5003, 3003, 'ATTENDANCE_WARNING', 'MEDIUM', 'UNPROCESSED', '2026-03-26 09:00:20');

-- 测试复核数据
INSERT IGNORE INTO `reviewRecord` (`id`, `exceptionId`, `reviewUserId`, `result`, `comment`, `reviewTime`) VALUES
(6001, 3001, 9001, 'CONFIRMED', '确认存在代打卡风险，已记录处理', '2026-03-26 09:10:00');

-- 测试操作日志数据
INSERT IGNORE INTO `operationLog` (`id`, `userId`, `type`, `content`, `operationTime`) VALUES
(7001, 9001, 'LOGIN', '管理员登录系统', '2026-03-26 08:00:00'),
(7002, 1001, 'CHECKIN', '张三完成上班打卡', '2026-03-26 08:58:00'),
(7003, 1002, 'CHECKIN', '李四在异常设备完成打卡', '2026-03-26 08:58:30'),
(7004, 9001, 'REVIEW', '管理员复核异常记录3001', '2026-03-26 09:10:00');
