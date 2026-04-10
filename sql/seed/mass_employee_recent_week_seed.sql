USE `system`;
SET NAMES utf8mb4;

-- 说明：
-- 1. 本脚本用于把员工扩展到 50 人，并补齐最近 7 天（2026-04-04 ~ 2026-04-10）的上/下班打卡数据。
-- 2. 员工账号为拼音，密码统一为 123456（对应 BCrypt 密文）。
-- 3. 异常记录保持稀疏分布，但覆盖更多异常类型，便于统计分析页和概览工作台展示。

INSERT INTO `user` (`id`, `username`, `password`, `realName`, `gender`, `phone`, `deptId`, `roleId`, `status`, `createTime`) VALUES
(1021, 'zhouyi', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '周怡', '女', '13800000022', 1, 2, 1, '2026-03-01 11:30:00'),
(1022, 'hanxu', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '韩旭', '男', '13800000023', 1, 2, 1, '2026-03-01 11:40:00'),
(1023, 'zhenghao', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '郑昊', '男', '13800000024', 2, 2, 1, '2026-03-01 11:50:00'),
(1024, 'yuewen', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '岳雯', '女', '13800000025', 2, 2, 1, '2026-03-01 12:00:00'),
(1025, 'guanlin', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '关琳', '女', '13800000026', 3, 2, 1, '2026-03-01 12:10:00'),
(1026, 'peiyu', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '裴宇', '男', '13800000027', 3, 2, 1, '2026-03-01 12:20:00'),
(1027, 'shuyan', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '舒妍', '女', '13800000028', 4, 2, 1, '2026-03-01 12:30:00'),
(1028, 'jiarui', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '贾睿', '男', '13800000029', 4, 2, 1, '2026-03-01 12:40:00'),
(1029, 'wenhao', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '文昊', '男', '13800000030', 5, 2, 1, '2026-03-01 12:50:00'),
(1030, 'simin', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '司敏', '女', '13800000031', 5, 2, 1, '2026-03-01 13:00:00'),
(1031, 'ruilin', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '瑞林', '男', '13800000032', 6, 2, 1, '2026-03-01 13:10:00'),
(1032, 'jianing', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '嘉宁', '女', '13800000033', 6, 2, 1, '2026-03-01 13:20:00'),
(1033, 'yutong', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '雨桐', '女', '13800000034', 7, 2, 1, '2026-03-01 13:30:00'),
(1034, 'zhixin', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '芷欣', '女', '13800000035', 7, 2, 1, '2026-03-01 13:40:00'),
(1035, 'bohan', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '柏涵', '男', '13800000036', 8, 2, 1, '2026-03-01 13:50:00'),
(1036, 'yuanjing', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '媛静', '女', '13800000037', 8, 2, 1, '2026-03-01 14:00:00'),
(1037, 'junkai', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '俊凯', '男', '13800000038', 9, 2, 1, '2026-03-01 14:10:00'),
(1038, 'xinyue', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '欣悦', '女', '13800000039', 9, 2, 1, '2026-03-01 14:20:00'),
(1039, 'shilei', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '诗蕾', '女', '13800000040', 2, 2, 1, '2026-03-01 14:30:00'),
(1040, 'anran', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '安然', '女', '13800000041', 2, 2, 1, '2026-03-01 14:40:00'),
(1041, 'kerui', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '可睿', '男', '13800000042', 3, 2, 1, '2026-03-01 14:50:00'),
(1042, 'yichen', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '奕辰', '男', '13800000043', 3, 2, 1, '2026-03-01 15:00:00'),
(1043, 'haonan', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '浩楠', '男', '13800000044', 4, 2, 1, '2026-03-01 15:10:00'),
(1044, 'yilin', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '怡琳', '女', '13800000045', 4, 2, 1, '2026-03-01 15:20:00'),
(1045, 'zexuan', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '泽轩', '男', '13800000046', 5, 2, 1, '2026-03-01 15:30:00'),
(1046, 'qingya', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '清雅', '女', '13800000047', 5, 2, 1, '2026-03-01 15:40:00'),
(1047, 'weiming', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '伟铭', '男', '13800000048', 6, 2, 1, '2026-03-01 15:50:00'),
(1048, 'xiaoyu', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '晓雨', '女', '13800000049', 6, 2, 1, '2026-03-01 16:00:00'),
(1049, 'chenxi', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '晨曦', '女', '13800000050', 7, 2, 1, '2026-03-01 16:10:00'),
(1050, 'zimo', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '子墨', '男', '13800000051', 7, 2, 1, '2026-03-01 16:20:00')
ON DUPLICATE KEY UPDATE
`password` = VALUES(`password`),
`realName` = VALUES(`realName`),
`gender` = VALUES(`gender`),
`phone` = VALUES(`phone`),
`deptId` = VALUES(`deptId`),
`status` = VALUES(`status`);

INSERT INTO `attendanceRecord` (`userId`, `checkTime`, `checkType`, `deviceId`, `ipAddr`, `location`, `faceScore`, `status`, `createTime`)
SELECT
    u.id,
    CASE
        WHEN t.checkType = 'IN' THEN TIMESTAMP(d.workDate, SEC_TO_TIME((8 * 3600) + ((35 + MOD(u.id + d.dayOffset * 3, 28)) * 60)))
        ELSE TIMESTAMP(d.workDate, SEC_TO_TIME((17 * 3600) + ((32 + MOD(u.id + d.dayOffset * 5, 52)) * 60)))
    END AS checkTime,
    t.checkType,
    CASE WHEN MOD(u.deptId + d.dayOffset, 2) = 0 THEN 'DEV-001' ELSE 'DEV-002' END AS deviceId,
    CONCAT('10.26.', u.deptId, '.', 20 + d.dayOffset) AS ipAddr,
    CASE WHEN MOD(u.deptId + d.dayOffset, 2) = 0 THEN '办公区A' ELSE '办公区B' END AS location,
    ROUND(CASE WHEN t.checkType = 'IN' THEN 96 + MOD(u.id + d.dayOffset, 12) / 10 ELSE 96.3 + MOD(u.id + d.dayOffset, 10) / 10 END, 2) AS faceScore,
    'NORMAL' AS status,
    CASE
        WHEN t.checkType = 'IN' THEN TIMESTAMP(d.workDate, SEC_TO_TIME((8 * 3600) + ((35 + MOD(u.id + d.dayOffset * 3, 28)) * 60)))
        ELSE TIMESTAMP(d.workDate, SEC_TO_TIME((17 * 3600) + ((32 + MOD(u.id + d.dayOffset * 5, 52)) * 60)))
    END AS createTime
FROM `user` u
JOIN (
    SELECT DATE('2026-04-04') AS workDate, 0 AS dayOffset
    UNION ALL SELECT DATE('2026-04-05'), 1
    UNION ALL SELECT DATE('2026-04-06'), 2
    UNION ALL SELECT DATE('2026-04-07'), 3
    UNION ALL SELECT DATE('2026-04-08'), 4
    UNION ALL SELECT DATE('2026-04-09'), 5
    UNION ALL SELECT DATE('2026-04-10'), 6
) d
JOIN (
    SELECT 'IN' AS checkType
    UNION ALL SELECT 'OUT'
) t
WHERE u.roleId = 2
  AND NOT EXISTS (
      SELECT 1
      FROM `attendanceRecord` ar
      WHERE ar.userId = u.id
        AND ar.checkType = t.checkType
        AND DATE(ar.checkTime) = d.workDate
  );

UPDATE `attendanceRecord`
SET `checkTime` = '2026-04-04 09:18:00', `createTime` = '2026-04-04 09:18:00', `status` = 'ABNORMAL', `deviceId` = 'DEV-001', `location` = '办公区A', `faceScore` = 95.20
WHERE `userId` = 1021 AND `checkType` = 'IN' AND DATE(`checkTime`) = '2026-04-04';

UPDATE `attendanceRecord`
SET `checkTime` = '2026-04-05 08:43:00', `createTime` = '2026-04-05 08:43:00', `status` = 'ABNORMAL', `deviceId` = 'DEV-009', `location` = '外部区域', `faceScore` = 82.40
WHERE `userId` = 1024 AND `checkType` = 'IN' AND DATE(`checkTime`) = '2026-04-05';

UPDATE `attendanceRecord`
SET `checkTime` = '2026-04-06 17:18:00', `createTime` = '2026-04-06 17:18:00', `status` = 'ABNORMAL', `deviceId` = 'DEV-001', `location` = '办公区A', `faceScore` = 96.20
WHERE `userId` = 1027 AND `checkType` = 'OUT' AND DATE(`checkTime`) = '2026-04-06';

UPDATE `attendanceRecord`
SET `checkTime` = '2026-04-07 09:03:00', `createTime` = '2026-04-07 09:03:00', `status` = 'ABNORMAL', `deviceId` = 'DEV-002', `location` = '办公区B', `faceScore` = 94.80
WHERE `userId` = 1030 AND `checkType` = 'IN' AND DATE(`checkTime`) = '2026-04-07';

UPDATE `attendanceRecord`
SET `checkTime` = '2026-04-08 06:12:00', `createTime` = '2026-04-08 06:12:00', `status` = 'ABNORMAL', `deviceId` = 'DEV-002', `location` = '远程办公点', `faceScore` = 91.50
WHERE `userId` = 1033 AND `checkType` = 'IN' AND DATE(`checkTime`) = '2026-04-08';

UPDATE `attendanceRecord`
SET `checkTime` = '2026-04-09 09:21:00', `createTime` = '2026-04-09 09:21:00', `status` = 'ABNORMAL', `deviceId` = 'DEV-002', `location` = '办公区B', `faceScore` = 95.10
WHERE `userId` = 1036 AND `checkType` = 'IN' AND DATE(`checkTime`) = '2026-04-09';

UPDATE `attendanceRecord`
SET `checkTime` = '2026-04-10 08:41:00', `createTime` = '2026-04-10 08:41:00', `status` = 'ABNORMAL', `deviceId` = 'DEV-009', `location` = '外部区域', `faceScore` = 83.10
WHERE `userId` = 1039 AND `checkType` = 'IN' AND DATE(`checkTime`) = '2026-04-10';

UPDATE `attendanceRecord`
SET `checkTime` = '2026-04-10 17:08:00', `createTime` = '2026-04-10 17:08:00', `status` = 'ABNORMAL', `deviceId` = 'DEV-001', `location` = '办公区A', `faceScore` = 96.40
WHERE `userId` = 1042 AND `checkType` = 'OUT' AND DATE(`checkTime`) = '2026-04-10';

INSERT IGNORE INTO `attendanceException` (`id`, `recordId`, `userId`, `type`, `riskLevel`, `sourceType`, `description`, `processStatus`, `createTime`)
SELECT 3030, ar.id, 1021, 'LATE', 'MEDIUM', 'RULE', '周怡迟到18分钟', 'PENDING', '2026-04-04 09:18:10'
FROM `attendanceRecord` ar
WHERE ar.userId = 1021 AND ar.checkType = 'IN' AND ar.checkTime = '2026-04-04 09:18:00';

INSERT IGNORE INTO `attendanceException` (`id`, `recordId`, `userId`, `type`, `riskLevel`, `sourceType`, `description`, `processStatus`, `createTime`)
SELECT 3031, ar.id, 1024, 'PROXY_CHECKIN', 'HIGH', 'MODEL', '岳雯在外部设备打卡且人脸分值偏低', 'REVIEWED', '2026-04-05 08:43:10'
FROM `attendanceRecord` ar
WHERE ar.userId = 1024 AND ar.checkType = 'IN' AND ar.checkTime = '2026-04-05 08:43:00';

INSERT IGNORE INTO `attendanceException` (`id`, `recordId`, `userId`, `type`, `riskLevel`, `sourceType`, `description`, `processStatus`, `createTime`)
SELECT 3032, ar.id, 1027, 'EARLY_LEAVE', 'LOW', 'RULE', '舒妍提前离岗', 'PENDING', '2026-04-06 17:18:10'
FROM `attendanceRecord` ar
WHERE ar.userId = 1027 AND ar.checkType = 'OUT' AND ar.checkTime = '2026-04-06 17:18:00';

INSERT IGNORE INTO `attendanceException` (`id`, `recordId`, `userId`, `type`, `riskLevel`, `sourceType`, `description`, `processStatus`, `createTime`)
SELECT 3033, ar.id, 1030, 'REPEAT_CHECK', 'LOW', 'RULE', '司敏短时间重复打卡', 'REVIEWED', '2026-04-07 09:03:10'
FROM `attendanceRecord` ar
WHERE ar.userId = 1030 AND ar.checkType = 'IN' AND ar.checkTime = '2026-04-07 09:03:00';

INSERT IGNORE INTO `attendanceException` (`id`, `recordId`, `userId`, `type`, `riskLevel`, `sourceType`, `description`, `processStatus`, `createTime`)
SELECT 3034, ar.id, 1033, 'ILLEGAL_TIME', 'MEDIUM', 'RULE', '雨桐在非工作时段打卡', 'PENDING', '2026-04-08 06:12:10'
FROM `attendanceRecord` ar
WHERE ar.userId = 1033 AND ar.checkType = 'IN' AND ar.checkTime = '2026-04-08 06:12:00';

INSERT IGNORE INTO `attendanceException` (`id`, `recordId`, `userId`, `type`, `riskLevel`, `sourceType`, `description`, `processStatus`, `createTime`)
SELECT 3035, ar.id, 1036, 'LATE', 'MEDIUM', 'RULE', '媛静迟到21分钟', 'PENDING', '2026-04-09 09:21:10'
FROM `attendanceRecord` ar
WHERE ar.userId = 1036 AND ar.checkType = 'IN' AND ar.checkTime = '2026-04-09 09:21:00';

INSERT IGNORE INTO `attendanceException` (`id`, `recordId`, `userId`, `type`, `riskLevel`, `sourceType`, `description`, `processStatus`, `createTime`)
SELECT 3036, ar.id, 1039, 'MULTI_LOCATION_CONFLICT', 'HIGH', 'MODEL', '诗蕾在外部区域设备完成跨地点打卡', 'REVIEWED', '2026-04-10 08:41:10'
FROM `attendanceRecord` ar
WHERE ar.userId = 1039 AND ar.checkType = 'IN' AND ar.checkTime = '2026-04-10 08:41:00';

INSERT IGNORE INTO `attendanceException` (`id`, `recordId`, `userId`, `type`, `riskLevel`, `sourceType`, `description`, `processStatus`, `createTime`)
SELECT 3037, ar.id, 1042, 'EARLY_LEAVE', 'MEDIUM', 'RULE', '奕辰提前离岗', 'PENDING', '2026-04-10 17:08:10'
FROM `attendanceRecord` ar
WHERE ar.userId = 1042 AND ar.checkType = 'OUT' AND ar.checkTime = '2026-04-10 17:08:00';

INSERT IGNORE INTO `exceptionAnalysis` (`id`, `exceptionId`, `promptTemplateId`, `inputSummary`, `modelResult`, `modelConclusion`, `confidenceScore`, `decisionReason`, `suggestion`, `reasonSummary`, `actionSuggestion`, `similarCaseSummary`, `promptVersion`, `createTime`) VALUES
(4030, 3030, 8001, '周怡在09:18完成签到', '识别为迟到异常，建议提醒准时到岗', 'LATE', 72.60, '超过上班阈值18分钟', '建议部门负责人跟进提醒', '到岗时间晚于正常时段', '建议关注后续一周到岗稳定性', '历史同类迟到样本多通过提醒改善', 'v1.0', '2026-04-04 09:18:20'),
(4031, 3031, 8001, '岳雯在外部区域设备打卡且分值偏低', '识别为疑似代打卡高风险，建议优先复核', 'PROXY_CHECKIN', 93.10, '外部设备、人脸分值偏低且行为异常', '建议管理员优先人工核验', '外部设备与低分值叠加明显提升代打卡风险', '建议查看同日轨迹与设备使用情况', '历史相似场景多被确认异常', 'v1.0', '2026-04-05 08:43:20'),
(4032, 3032, 8001, '舒妍于17:18完成下班打卡', '识别为早退异常，建议留档观察', 'EARLY_LEAVE', 60.40, '早于正常下班时间', '建议记录并观察是否重复发生', '离岗时间早于规则阈值', '建议结合近期出勤整体判断', '同类早退样本多为偶发情况', 'v1.0', '2026-04-06 17:18:20'),
(4033, 3033, 8001, '司敏在短时间内重复提交打卡', '识别为重复打卡，建议核对是否误操作', 'REPEAT_CHECK', 63.50, '重复打卡规则命中', '建议查看原始打卡记录', '短时重复签到与规则阈值冲突', '建议排查是否存在误触发', '历史同类样本多数为重复提交', 'v1.0', '2026-04-07 09:03:20'),
(4034, 3034, 8001, '雨桐在06:12完成签到', '识别为非法时间打卡，建议核验排班', 'ILLEGAL_TIME', 68.40, '发生在非规定工作时段', '建议核对排班与加班审批', '打卡时间超出规则时间窗', '建议核实是否存在临时加班安排', '历史同类样本多与排班变更有关', 'v1.0', '2026-04-08 06:12:20'),
(4035, 3035, 8001, '媛静在09:21完成签到', '识别为迟到异常，建议提醒考勤纪律', 'LATE', 74.20, '超过上班阈值21分钟', '建议部门负责人及时提醒', '到岗时间显著晚于正常区间', '建议关注后续到岗波动', '同类迟到样本多通过提醒改善', 'v1.0', '2026-04-09 09:21:20'),
(4036, 3036, 8001, '诗蕾在外部设备完成跨地点打卡', '识别为多地点异常高风险，建议优先复核', 'MULTI_LOCATION_CONFLICT', 90.80, '外部设备与地点跨度异常', '建议核验现场轨迹与设备情况', '短时跨区域行为不符合常规考勤路线', '建议重点复核同日位置变化', '历史高风险多地点异常样本较多', 'v1.0', '2026-04-10 08:41:20'),
(4037, 3037, 8001, '奕辰于17:08完成下班打卡', '识别为早退异常，建议留档观察', 'EARLY_LEAVE', 66.30, '明显早于下班时间阈值', '建议部门负责人关注', '下班时间明显提前', '建议核实是否存在提前离岗审批', '同类早退样本常需结合审批判断', 'v1.0', '2026-04-10 17:08:20');

INSERT IGNORE INTO `warningRecord` (`id`, `exceptionId`, `type`, `level`, `status`, `priorityScore`, `aiSummary`, `disposeSuggestion`, `decisionSource`, `sendTime`) VALUES
(5030, 3030, 'ATTENDANCE_WARNING', 'MEDIUM', 'UNPROCESSED', 72.60, '周怡出现迟到异常，建议及时提醒。', '建议部门负责人关注本周到岗情况。', 'RULE', '2026-04-04 09:18:30'),
(5031, 3031, 'RISK_WARNING', 'HIGH', 'PROCESSED', 93.10, '岳雯存在疑似代打卡高风险。', '建议优先查看异常详情并完成复核。', 'MODEL_FUSION', '2026-04-05 08:43:30'),
(5032, 3032, 'ATTENDANCE_WARNING', 'LOW', 'UNPROCESSED', 60.40, '舒妍出现早退情况。', '建议记录并持续观察后续出勤。', 'RULE', '2026-04-06 17:18:30'),
(5033, 3033, 'ATTENDANCE_WARNING', 'LOW', 'PROCESSED', 63.50, '司敏存在重复打卡行为。', '建议核对是否误操作并提醒员工。', 'RULE', '2026-04-07 09:03:30'),
(5034, 3034, 'ATTENDANCE_WARNING', 'MEDIUM', 'UNPROCESSED', 68.40, '雨桐出现非法时间打卡。', '建议核对排班或加班审批。', 'RULE', '2026-04-08 06:12:30'),
(5035, 3035, 'ATTENDANCE_WARNING', 'MEDIUM', 'UNPROCESSED', 74.20, '媛静迟到时长偏高，建议关注。', '建议部门负责人及时提醒。', 'RULE', '2026-04-09 09:21:30'),
(5036, 3036, 'RISK_WARNING', 'HIGH', 'PROCESSED', 90.80, '诗蕾短时跨地点打卡，存在高风险。', '建议立即复核同日地点轨迹。', 'MODEL_FUSION', '2026-04-10 08:41:30'),
(5037, 3037, 'ATTENDANCE_WARNING', 'MEDIUM', 'UNPROCESSED', 66.30, '奕辰提前离岗，建议核对审批。', '建议核对离岗审批情况。', 'RULE', '2026-04-10 17:08:30');

INSERT IGNORE INTO `reviewRecord` (`id`, `exceptionId`, `reviewUserId`, `result`, `comment`, `aiReviewSuggestion`, `similarCaseSummary`, `feedbackTag`, `strategyFeedback`, `reviewTime`) VALUES
(6030, 3031, 9001, 'CONFIRMED', '确认外部设备打卡异常，已完成处理', 'AI 建议优先核对设备位置与人脸分值。', '相似场景多与高风险代打卡有关。', 'TRUE_POSITIVE', '建议继续保留外部设备高风险策略。', '2026-04-05 09:05:00'),
(6031, 3033, 9001, 'FALSE_POSITIVE', '核对后判定为员工误操作', 'AI 建议查看重复点击时间窗。', '相似案例中存在误触发情况。', 'FALSE_POSITIVE', '建议优化重复打卡识别阈值。', '2026-04-07 10:10:00'),
(6032, 3036, 9001, 'CONFIRMED', '确认跨区域打卡异常，已升级关注', 'AI 建议优先核对同日轨迹与设备日志。', '历史跨区域高风险异常多数被确认。', 'TRUE_POSITIVE', '建议继续强化多地点异常规则。', '2026-04-10 09:20:00');

-- 部门差异化异常画像补充：
-- 综合管理部：问题少但闭环快
-- 技术部：连续迟到更明显
-- 行政部：以早退为主
-- 市场部/运营部：外勤场景高风险异常更突出
-- 客服部：轻中度迟到
-- 法务合规部：异常少且闭环快

UPDATE `attendanceRecord`
SET `checkTime` = '2026-04-07 06:18:00', `createTime` = '2026-04-07 06:18:00', `status` = 'ABNORMAL', `deviceId` = 'DEV-001', `location` = '办公区A', `faceScore` = 92.10
WHERE `userId` = 1022 AND `checkType` = 'IN' AND DATE(`checkTime`) = '2026-04-07';

UPDATE `attendanceRecord`
SET `checkTime` = '2026-04-08 09:22:00', `createTime` = '2026-04-08 09:22:00', `status` = 'ABNORMAL', `deviceId` = 'DEV-001', `location` = '办公区A', `faceScore` = 95.00
WHERE `userId` = 1040 AND `checkType` = 'IN' AND DATE(`checkTime`) = '2026-04-08';

UPDATE `attendanceRecord`
SET `checkTime` = '2026-04-09 09:16:00', `createTime` = '2026-04-09 09:16:00', `status` = 'ABNORMAL', `deviceId` = 'DEV-001', `location` = '办公区A', `faceScore` = 95.20
WHERE `userId` = 1040 AND `checkType` = 'IN' AND DATE(`checkTime`) = '2026-04-09';

UPDATE `attendanceRecord`
SET `checkTime` = '2026-04-10 09:19:00', `createTime` = '2026-04-10 09:19:00', `status` = 'ABNORMAL', `deviceId` = 'DEV-002', `location` = '办公区B', `faceScore` = 94.90
WHERE `userId` = 1040 AND `checkType` = 'IN' AND DATE(`checkTime`) = '2026-04-10';

UPDATE `attendanceRecord`
SET `checkTime` = '2026-04-09 17:15:00', `createTime` = '2026-04-09 17:15:00', `status` = 'ABNORMAL', `deviceId` = 'DEV-001', `location` = '办公区A', `faceScore` = 96.40
WHERE `userId` = 1025 AND `checkType` = 'OUT' AND DATE(`checkTime`) = '2026-04-09';

UPDATE `attendanceRecord`
SET `checkTime` = '2026-04-10 17:20:00', `createTime` = '2026-04-10 17:20:00', `status` = 'ABNORMAL', `deviceId` = 'DEV-002', `location` = '办公区B', `faceScore` = 96.30
WHERE `userId` = 1026 AND `checkType` = 'OUT' AND DATE(`checkTime`) = '2026-04-10';

UPDATE `attendanceRecord`
SET `checkTime` = '2026-04-08 08:44:00', `createTime` = '2026-04-08 08:44:00', `status` = 'ABNORMAL', `deviceId` = 'DEV-009', `location` = '外部区域', `faceScore` = 84.20
WHERE `userId` = 1031 AND `checkType` = 'IN' AND DATE(`checkTime`) = '2026-04-08';

UPDATE `attendanceRecord`
SET `checkTime` = '2026-04-08 09:17:00', `createTime` = '2026-04-08 09:17:00', `status` = 'ABNORMAL', `deviceId` = 'DEV-002', `location` = '办公区B', `faceScore` = 95.60
WHERE `userId` = 1035 AND `checkType` = 'IN' AND DATE(`checkTime`) = '2026-04-08';

UPDATE `attendanceRecord`
SET `checkTime` = '2026-04-06 17:14:00', `createTime` = '2026-04-06 17:14:00', `status` = 'ABNORMAL', `deviceId` = 'DEV-001', `location` = '办公区A', `faceScore` = 96.50
WHERE `userId` = 1038 AND `checkType` = 'OUT' AND DATE(`checkTime`) = '2026-04-06';

UPDATE `attendanceRecord`
SET `checkTime` = '2026-04-09 08:46:00', `createTime` = '2026-04-09 08:46:00', `status` = 'ABNORMAL', `deviceId` = 'DEV-009', `location` = '外部区域', `faceScore` = 83.60
WHERE `userId` = 1049 AND `checkType` = 'IN' AND DATE(`checkTime`) = '2026-04-09';

INSERT IGNORE INTO `attendanceException` (`id`, `recordId`, `userId`, `type`, `riskLevel`, `sourceType`, `description`, `processStatus`, `createTime`)
SELECT 3040, ar.id, 1022, 'ILLEGAL_TIME', 'MEDIUM', 'RULE', '韩旭在非工作时段打卡', 'REVIEWED', '2026-04-07 06:18:10'
FROM `attendanceRecord` ar
WHERE ar.userId = 1022 AND ar.checkType = 'IN' AND ar.checkTime = '2026-04-07 06:18:00';

INSERT IGNORE INTO `attendanceException` (`id`, `recordId`, `userId`, `type`, `riskLevel`, `sourceType`, `description`, `processStatus`, `createTime`)
SELECT 3041, ar.id, 1040, 'LATE', 'MEDIUM', 'RULE', '安然连续第一天迟到', 'PENDING', '2026-04-08 09:22:10'
FROM `attendanceRecord` ar
WHERE ar.userId = 1040 AND ar.checkType = 'IN' AND ar.checkTime = '2026-04-08 09:22:00';

INSERT IGNORE INTO `attendanceException` (`id`, `recordId`, `userId`, `type`, `riskLevel`, `sourceType`, `description`, `processStatus`, `createTime`)
SELECT 3042, ar.id, 1040, 'LATE', 'MEDIUM', 'RULE', '安然连续第二天迟到', 'REVIEWED', '2026-04-09 09:16:10'
FROM `attendanceRecord` ar
WHERE ar.userId = 1040 AND ar.checkType = 'IN' AND ar.checkTime = '2026-04-09 09:16:00';

INSERT IGNORE INTO `attendanceException` (`id`, `recordId`, `userId`, `type`, `riskLevel`, `sourceType`, `description`, `processStatus`, `createTime`)
SELECT 3043, ar.id, 1040, 'LATE', 'MEDIUM', 'RULE', '安然连续第三天迟到', 'PENDING', '2026-04-10 09:19:10'
FROM `attendanceRecord` ar
WHERE ar.userId = 1040 AND ar.checkType = 'IN' AND ar.checkTime = '2026-04-10 09:19:00';

INSERT IGNORE INTO `attendanceException` (`id`, `recordId`, `userId`, `type`, `riskLevel`, `sourceType`, `description`, `processStatus`, `createTime`)
SELECT 3044, ar.id, 1025, 'EARLY_LEAVE', 'LOW', 'RULE', '关琳提前离岗45分钟', 'PENDING', '2026-04-09 17:15:10'
FROM `attendanceRecord` ar
WHERE ar.userId = 1025 AND ar.checkType = 'OUT' AND ar.checkTime = '2026-04-09 17:15:00';

INSERT IGNORE INTO `attendanceException` (`id`, `recordId`, `userId`, `type`, `riskLevel`, `sourceType`, `description`, `processStatus`, `createTime`)
SELECT 3045, ar.id, 1026, 'EARLY_LEAVE', 'LOW', 'RULE', '裴宇提前离岗40分钟', 'PENDING', '2026-04-10 17:20:10'
FROM `attendanceRecord` ar
WHERE ar.userId = 1026 AND ar.checkType = 'OUT' AND ar.checkTime = '2026-04-10 17:20:00';

INSERT IGNORE INTO `attendanceException` (`id`, `recordId`, `userId`, `type`, `riskLevel`, `sourceType`, `description`, `processStatus`, `createTime`)
SELECT 3046, ar.id, 1031, 'MULTI_LOCATION_CONFLICT', 'HIGH', 'MODEL', '瑞林在外部区域设备打卡且存在跨区域风险', 'REVIEWED', '2026-04-08 08:44:10'
FROM `attendanceRecord` ar
WHERE ar.userId = 1031 AND ar.checkType = 'IN' AND ar.checkTime = '2026-04-08 08:44:00';

INSERT IGNORE INTO `attendanceException` (`id`, `recordId`, `userId`, `type`, `riskLevel`, `sourceType`, `description`, `processStatus`, `createTime`)
SELECT 3047, ar.id, 1035, 'LATE', 'MEDIUM', 'RULE', '柏涵迟到17分钟', 'PENDING', '2026-04-08 09:17:10'
FROM `attendanceRecord` ar
WHERE ar.userId = 1035 AND ar.checkType = 'IN' AND ar.checkTime = '2026-04-08 09:17:00';

INSERT IGNORE INTO `attendanceException` (`id`, `recordId`, `userId`, `type`, `riskLevel`, `sourceType`, `description`, `processStatus`, `createTime`)
SELECT 3048, ar.id, 1038, 'EARLY_LEAVE', 'LOW', 'RULE', '欣悦提前离岗46分钟', 'REVIEWED', '2026-04-06 17:14:10'
FROM `attendanceRecord` ar
WHERE ar.userId = 1038 AND ar.checkType = 'OUT' AND ar.checkTime = '2026-04-06 17:14:00';

INSERT IGNORE INTO `attendanceException` (`id`, `recordId`, `userId`, `type`, `riskLevel`, `sourceType`, `description`, `processStatus`, `createTime`)
SELECT 3049, ar.id, 1049, 'PROXY_CHECKIN', 'HIGH', 'MODEL', '晨曦在外部区域设备打卡且存在疑似代打卡风险', 'REVIEWED', '2026-04-09 08:46:10'
FROM `attendanceRecord` ar
WHERE ar.userId = 1049 AND ar.checkType = 'IN' AND ar.checkTime = '2026-04-09 08:46:00';

INSERT IGNORE INTO `exceptionAnalysis` (`id`, `exceptionId`, `promptTemplateId`, `inputSummary`, `modelResult`, `modelConclusion`, `confidenceScore`, `decisionReason`, `suggestion`, `reasonSummary`, `actionSuggestion`, `similarCaseSummary`, `promptVersion`, `createTime`) VALUES
(4040, 3040, 8001, '韩旭在06:18完成签到', '识别为非法时间打卡，建议核验排班', 'ILLEGAL_TIME', 69.20, '发生在非规定工作时段', '建议核对排班或加班审批', '打卡时间明显早于正常区间', '建议确认是否存在临时工作安排', '类似管理部异常多数已快速闭环', 'v1.0', '2026-04-07 06:18:20'),
(4041, 3041, 8001, '安然在09:22完成签到', '识别为迟到异常，建议提醒准时到岗', 'LATE', 71.80, '超过上班阈值22分钟', '建议部门负责人及时提醒', '到岗时间明显晚于正常区间', '建议持续观察是否形成连续迟到趋势', '技术部门近期存在到岗波动样本', 'v1.0', '2026-04-08 09:22:20'),
(4042, 3042, 8001, '安然在09:16完成签到', '识别为迟到异常，建议继续关注', 'LATE', 70.60, '连续第二天超过上班阈值', '建议结合连续迟到情况跟进', '已形成连续迟到迹象', '建议复核后评估是否需要部门提醒', '连续迟到样本通常需结合工作安排判断', 'v1.0', '2026-04-09 09:16:20'),
(4043, 3043, 8001, '安然在09:19完成签到', '识别为迟到异常，建议升级关注', 'LATE', 74.10, '连续第三天超过上班阈值', '建议重点关注本周到岗纪律', '连续迟到模式进一步强化', '建议结合周度趋势进行管理提醒', '技术部门连续迟到样本较少但需关注', 'v1.0', '2026-04-10 09:19:20'),
(4044, 3044, 8001, '关琳于17:15完成下班打卡', '识别为早退异常，建议留档观察', 'EARLY_LEAVE', 60.80, '明显早于下班时间阈值', '建议记录并持续观察', '离岗时间早于规则要求', '建议核实是否存在提前离岗审批', '行政部门早退样本多为轻中度异常', 'v1.0', '2026-04-09 17:15:20'),
(4045, 3045, 8001, '裴宇于17:20完成下班打卡', '识别为早退异常，建议继续关注', 'EARLY_LEAVE', 61.40, '早于正常下班时间40分钟', '建议留档并观察后续出勤', '早退时长较明显', '建议结合审批和工作安排判断', '行政部门相似样本多需人工确认', 'v1.0', '2026-04-10 17:20:20'),
(4046, 3046, 8001, '瑞林在外部区域设备打卡', '识别为多地点异常高风险，建议优先复核', 'MULTI_LOCATION_CONFLICT', 90.60, '外部设备与地点跨度异常', '建议核验现场轨迹与设备使用情况', '短时跨区域行为不符合常规考勤路线', '建议重点复核同日位置变化', '市场部外勤场景高风险异常应重点关注', 'v1.0', '2026-04-08 08:44:20'),
(4047, 3047, 8001, '柏涵在09:17完成签到', '识别为迟到异常，建议提醒准时到岗', 'LATE', 72.10, '超过上班阈值17分钟', '建议班组负责人提醒', '到岗时间晚于正常区间', '建议关注客服班次稳定性', '客服部门轻中度迟到样本相对集中', 'v1.0', '2026-04-08 09:17:20'),
(4048, 3048, 8001, '欣悦于17:14完成下班打卡', '识别为早退异常，建议核验审批后结案', 'EARLY_LEAVE', 58.90, '早于正常下班时间', '建议核对审批并快速闭环', '离岗时间略早于正常要求', '建议结合当日审批情况确认', '法务合规部异常通常闭环较快', 'v1.0', '2026-04-06 17:14:20'),
(4049, 3049, 8001, '晨曦在外部区域设备打卡且分值偏低', '识别为疑似代打卡高风险，建议立即复核', 'PROXY_CHECKIN', 94.20, '外部设备、人脸分值偏低且行为异常', '建议优先完成人工复核', '多项高风险特征同时命中', '建议核验设备与人员轨迹', '运营部门高风险异常需优先闭环', 'v1.0', '2026-04-09 08:46:20');

INSERT IGNORE INTO `warningRecord` (`id`, `exceptionId`, `type`, `level`, `status`, `priorityScore`, `aiSummary`, `disposeSuggestion`, `decisionSource`, `sendTime`) VALUES
(5040, 3040, 'ATTENDANCE_WARNING', 'MEDIUM', 'PROCESSED', 69.20, '韩旭出现非法时间打卡。', '建议核对排班或加班审批。', 'RULE', '2026-04-07 06:18:30'),
(5041, 3041, 'ATTENDANCE_WARNING', 'MEDIUM', 'UNPROCESSED', 71.80, '安然出现连续迟到趋势，建议关注。', '建议部门负责人尽快跟进。', 'RULE', '2026-04-08 09:22:30'),
(5042, 3042, 'ATTENDANCE_WARNING', 'MEDIUM', 'PROCESSED', 70.60, '安然连续第二天迟到。', '建议结合复核结果决定后续动作。', 'RULE', '2026-04-09 09:16:30'),
(5043, 3043, 'ATTENDANCE_WARNING', 'MEDIUM', 'UNPROCESSED', 74.10, '安然连续第三天迟到，风险上升。', '建议部门负责人重点关注。', 'RULE', '2026-04-10 09:19:30'),
(5044, 3044, 'ATTENDANCE_WARNING', 'LOW', 'UNPROCESSED', 60.80, '关琳出现早退情况。', '建议记录并持续观察。', 'RULE', '2026-04-09 17:15:30'),
(5045, 3045, 'ATTENDANCE_WARNING', 'LOW', 'UNPROCESSED', 61.40, '裴宇出现早退情况。', '建议留档并核对审批。', 'RULE', '2026-04-10 17:20:30'),
(5046, 3046, 'RISK_WARNING', 'HIGH', 'PROCESSED', 90.60, '瑞林存在多地点高风险异常。', '建议优先核验同日轨迹与设备情况。', 'MODEL_FUSION', '2026-04-08 08:44:30'),
(5047, 3047, 'ATTENDANCE_WARNING', 'MEDIUM', 'UNPROCESSED', 72.10, '柏涵迟到时长偏高，建议关注。', '建议班组负责人及时提醒。', 'RULE', '2026-04-08 09:17:30'),
(5048, 3048, 'ATTENDANCE_WARNING', 'LOW', 'PROCESSED', 58.90, '欣悦出现早退情况。', '建议核对审批后尽快结案。', 'RULE', '2026-04-06 17:14:30'),
(5049, 3049, 'RISK_WARNING', 'HIGH', 'PROCESSED', 94.20, '晨曦存在疑似代打卡高风险。', '建议优先查看异常详情并完成复核。', 'MODEL_FUSION', '2026-04-09 08:46:30');

INSERT IGNORE INTO `reviewRecord` (`id`, `exceptionId`, `reviewUserId`, `result`, `comment`, `aiReviewSuggestion`, `similarCaseSummary`, `feedbackTag`, `strategyFeedback`, `reviewTime`) VALUES
(6040, 3040, 9001, 'FALSE_POSITIVE', '核实后为临时加班打卡，已闭环', 'AI 建议核对排班或加班审批。', '管理部同类样本多在当天完成确认。', 'FALSE_POSITIVE', '建议保留非法时间核验提示，但加强审批联动。', '2026-04-07 08:20:00'),
(6041, 3042, 9001, 'CONFIRMED', '确认连续迟到情况，已提醒负责人跟进', 'AI 建议结合连续到岗趋势重点关注。', '连续迟到样本通常需要部门提醒。', 'TRUE_POSITIVE', '建议保留连续迟到预警策略。', '2026-04-09 10:00:00'),
(6042, 3046, 9001, 'CONFIRMED', '确认外勤轨迹异常，已完成处理', 'AI 建议优先核对同日轨迹与设备日志。', '市场部外勤类高风险异常多数需人工确认。', 'TRUE_POSITIVE', '建议继续强化多地点异常规则。', '2026-04-08 09:10:00'),
(6043, 3048, 9001, 'FALSE_POSITIVE', '经核实存在提前离岗审批，已结案', 'AI 建议核对审批后快速闭环。', '法务部门同类样本通常审批完备。', 'FALSE_POSITIVE', '建议在审批联动场景下弱化同类预警。', '2026-04-06 18:00:00'),
(6044, 3049, 9001, 'CONFIRMED', '确认高风险代打卡异常，已留档处理', 'AI 建议优先确认设备与人员轨迹。', '运营部门同类外部设备样本确认率较高。', 'TRUE_POSITIVE', '建议持续保留高风险代打卡策略。', '2026-04-09 09:18:00');
