USE `system`;
SET NAMES utf8mb4;

-- 说明：
-- 1. 本脚本用于把现有库升级到“站内通知 + 员工说明 + 缺勤检测 + 复核回告”的闭环版本。
-- 2. 适用于已存在旧版 `system` 数据库的增量迁移场景，不要求重新建库。
-- 3. 建议先备份数据库，再执行本脚本。

ALTER TABLE `attendanceException`
  MODIFY COLUMN `recordId` BIGINT DEFAULT NULL COMMENT '打卡记录ID';

SET @warning_record_add_interaction_status = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `warningRecord` ADD COLUMN `interactionStatus` VARCHAR(32) NOT NULL DEFAULT ''NONE'' COMMENT ''交互状态'' AFTER `sendTime`',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = 'system' AND table_name = 'warningRecord' AND column_name = 'interactionStatus'
);
PREPARE stmt_warning_record_add_interaction_status FROM @warning_record_add_interaction_status;
EXECUTE stmt_warning_record_add_interaction_status;
DEALLOCATE PREPARE stmt_warning_record_add_interaction_status;

SET @warning_record_add_employee_reply_deadline = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `warningRecord` ADD COLUMN `employeeReplyDeadline` DATETIME DEFAULT NULL COMMENT ''员工回复截止时间'' AFTER `interactionStatus`',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = 'system' AND table_name = 'warningRecord' AND column_name = 'employeeReplyDeadline'
);
PREPARE stmt_warning_record_add_employee_reply_deadline FROM @warning_record_add_employee_reply_deadline;
EXECUTE stmt_warning_record_add_employee_reply_deadline;
DEALLOCATE PREPARE stmt_warning_record_add_employee_reply_deadline;

SET @warning_record_add_assigned_admin_id = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `warningRecord` ADD COLUMN `assignedAdminId` BIGINT DEFAULT NULL COMMENT ''当前处理管理员ID'' AFTER `employeeReplyDeadline`',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = 'system' AND table_name = 'warningRecord' AND column_name = 'assignedAdminId'
);
PREPARE stmt_warning_record_add_assigned_admin_id FROM @warning_record_add_assigned_admin_id;
EXECUTE stmt_warning_record_add_assigned_admin_id;
DEALLOCATE PREPARE stmt_warning_record_add_assigned_admin_id;

SET @warning_record_add_last_interact_time = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `warningRecord` ADD COLUMN `lastInteractTime` DATETIME DEFAULT NULL COMMENT ''最近交互时间'' AFTER `assignedAdminId`',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = 'system' AND table_name = 'warningRecord' AND column_name = 'lastInteractTime'
);
PREPARE stmt_warning_record_add_last_interact_time FROM @warning_record_add_last_interact_time;
EXECUTE stmt_warning_record_add_last_interact_time;
DEALLOCATE PREPARE stmt_warning_record_add_last_interact_time;

UPDATE `warningRecord`
SET `interactionStatus` = 'NONE'
WHERE `interactionStatus` IS NULL OR `interactionStatus` = '';

UPDATE `warningRecord`
SET `lastInteractTime` = `sendTime`
WHERE `lastInteractTime` IS NULL;

CREATE TABLE IF NOT EXISTS `notificationRecord` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '通知ID',
  `recipientUserId` BIGINT NOT NULL COMMENT '接收用户ID',
  `senderUserId` BIGINT DEFAULT NULL COMMENT '发送用户ID',
  `businessType` VARCHAR(32) NOT NULL COMMENT '业务类型',
  `businessId` BIGINT NOT NULL COMMENT '业务ID',
  `category` VARCHAR(32) NOT NULL COMMENT '通知分类',
  `title` VARCHAR(120) NOT NULL COMMENT '通知标题',
  `content` VARCHAR(1000) NOT NULL COMMENT '通知内容',
  `level` VARCHAR(16) NOT NULL DEFAULT 'INFO' COMMENT '通知等级',
  `actionCode` VARCHAR(32) NOT NULL DEFAULT 'VIEW' COMMENT '动作编码',
  `readStatus` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读',
  `deadline` DATETIME DEFAULT NULL COMMENT '截止时间',
  `extraJson` TEXT DEFAULT NULL COMMENT '扩展信息',
  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `readTime` DATETIME DEFAULT NULL COMMENT '阅读时间',
  KEY `idxNotificationRecipientRead` (`recipientUserId`, `readStatus`, `createTime`),
  KEY `idxNotificationBusiness` (`businessType`, `businessId`, `category`),
  CONSTRAINT `fkNotificationRecipient` FOREIGN KEY (`recipientUserId`) REFERENCES `user` (`id`),
  CONSTRAINT `fkNotificationSender` FOREIGN KEY (`senderUserId`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知表';

CREATE TABLE IF NOT EXISTS `warningInteractionRecord` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '交互记录ID',
  `warningId` BIGINT NOT NULL COMMENT '预警ID',
  `exceptionId` BIGINT NOT NULL COMMENT '异常ID',
  `senderUserId` BIGINT DEFAULT NULL COMMENT '发送人ID',
  `senderRole` VARCHAR(16) NOT NULL COMMENT '发送人角色',
  `messageType` VARCHAR(32) NOT NULL COMMENT '消息类型',
  `content` VARCHAR(2000) NOT NULL COMMENT '消息内容',
  `attachmentsJson` TEXT DEFAULT NULL COMMENT '附件信息',
  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY `idxWarningInteractionWarning` (`warningId`, `createTime`),
  CONSTRAINT `fkWarningInteractionWarning` FOREIGN KEY (`warningId`) REFERENCES `warningRecord` (`id`),
  CONSTRAINT `fkWarningInteractionException` FOREIGN KEY (`exceptionId`) REFERENCES `attendanceException` (`id`),
  CONSTRAINT `fkWarningInteractionSender` FOREIGN KEY (`senderUserId`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警交互记录表';

INSERT INTO `exceptionType` (`code`, `name`, `description`, `status`, `createTime`, `updateTime`)
SELECT 'ABSENT', '缺勤', '在规定时段内未完成上班打卡', 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `exceptionType` WHERE `code` = 'ABSENT'
);

INSERT INTO `exceptionType` (`code`, `name`, `description`, `status`, `createTime`, `updateTime`)
SELECT 'MISSING_CHECKOUT', '下班缺卡', '在规定时段内未完成下班打卡', 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `exceptionType` WHERE `code` = 'MISSING_CHECKOUT'
);
