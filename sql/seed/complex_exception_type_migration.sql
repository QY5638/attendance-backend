CREATE TABLE IF NOT EXISTS `attendanceRepair` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '补卡申请ID',
  `userId` BIGINT NOT NULL COMMENT '用户ID',
  `checkType` VARCHAR(20) NOT NULL COMMENT '补卡类型',
  `checkTime` DATETIME NOT NULL COMMENT '补卡时间',
  `repairReason` VARCHAR(255) NOT NULL COMMENT '补卡原因',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '申请状态',
  `recordId` BIGINT DEFAULT NULL COMMENT '关联打卡记录ID',
  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY `idxAttendanceRepairUserTime` (`userId`, `checkTime`),
  UNIQUE KEY `ukAttendanceRepairPending` (`userId`, `checkType`, `checkTime`, `status`),
  CONSTRAINT `fkAttendanceRepairUser` FOREIGN KEY (`userId`) REFERENCES `user` (`id`),
  CONSTRAINT `fkAttendanceRepairRecord` FOREIGN KEY (`recordId`) REFERENCES `attendanceRecord` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补卡申请表';

INSERT INTO `exceptionType` (`code`, `name`, `description`, `status`, `createTime`, `updateTime`)
SELECT 'COMPLEX_ATTENDANCE_RISK', '综合识别异常', '模型或降级流程识别出的综合异常风险', 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `exceptionType` WHERE `code` = 'COMPLEX_ATTENDANCE_RISK'
);

UPDATE `attendanceException`
SET `type` = 'COMPLEX_ATTENDANCE_RISK'
WHERE `type` NOT IN (
  'PROXY_CHECKIN',
  'CONTINUOUS_LATE',
  'CONTINUOUS_EARLY_LEAVE',
  'CONTINUOUS_MULTI_LOCATION_CONFLICT',
  'CONTINUOUS_ILLEGAL_TIME',
  'CONTINUOUS_REPEAT_CHECK',
  'CONTINUOUS_PROXY_CHECKIN',
  'CONTINUOUS_ATTENDANCE_RISK',
  'COMPLEX_ATTENDANCE_RISK',
  'CONTINUOUS_MODEL_RISK',
  'LATE',
  'EARLY_LEAVE',
  'ILLEGAL_TIME',
  'REPEAT_CHECK',
  'MULTI_LOCATION_CONFLICT'
);

UPDATE `decisionTrace` dt
INNER JOIN `attendanceException` ae
  ON ae.`id` = dt.`businessId`
 AND dt.`businessType` = 'ATTENDANCE_EXCEPTION'
SET dt.`finalDecision` = 'COMPLEX_ATTENDANCE_RISK'
WHERE ae.`type` = 'COMPLEX_ATTENDANCE_RISK'
  AND dt.`finalDecision` NOT IN (
    'PROXY_CHECKIN',
    'CONTINUOUS_LATE',
    'CONTINUOUS_EARLY_LEAVE',
    'CONTINUOUS_MULTI_LOCATION_CONFLICT',
    'CONTINUOUS_ILLEGAL_TIME',
    'CONTINUOUS_REPEAT_CHECK',
    'CONTINUOUS_PROXY_CHECKIN',
    'CONTINUOUS_ATTENDANCE_RISK',
    'COMPLEX_ATTENDANCE_RISK',
    'CONTINUOUS_MODEL_RISK',
    'LATE',
    'EARLY_LEAVE',
    'ILLEGAL_TIME',
    'REPEAT_CHECK',
    'MULTI_LOCATION_CONFLICT'
  );

UPDATE `reviewRecord`
SET `result` = 'REJECTED'
WHERE `result` = 'FALSE_POSITIVE';
