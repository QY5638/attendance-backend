DROP DATABASE IF EXISTS `system`;
CREATE DATABASE `system`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE `system`;
SET NAMES utf8mb4;

CREATE TABLE `department` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '部门ID',
  `name` VARCHAR(100) NOT NULL COMMENT '部门名称',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '部门描述',
  UNIQUE KEY `ukDepartmentName` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

CREATE TABLE `role` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
  `code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `name` VARCHAR(100) NOT NULL COMMENT '角色名称',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '角色状态',
  UNIQUE KEY `ukRoleCode` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE `device` (
  `id` VARCHAR(64) PRIMARY KEY COMMENT '设备编号',
  `name` VARCHAR(100) NOT NULL COMMENT '设备名称',
  `location` VARCHAR(255) DEFAULT NULL COMMENT '设备位置',
  `longitude` DECIMAL(10,6) DEFAULT NULL COMMENT '设备经度',
  `latitude` DECIMAL(10,6) DEFAULT NULL COMMENT '设备纬度',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '设备状态',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '设备描述'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备表';

CREATE TABLE `user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '登录账号',
  `password` VARCHAR(100) NOT NULL COMMENT '登录密码',
  `realName` VARCHAR(50) NOT NULL COMMENT '真实姓名',
  `gender` VARCHAR(10) DEFAULT NULL COMMENT '性别',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `deptId` BIGINT NOT NULL COMMENT '部门ID',
  `roleId` BIGINT NOT NULL COMMENT '角色ID',
  `status` INT NOT NULL DEFAULT 1 COMMENT '用户状态',
  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY `ukUserUsername` (`username`),
  KEY `idxUserDeptId` (`deptId`),
  KEY `idxUserRoleId` (`roleId`),
  CONSTRAINT `fkUserDept` FOREIGN KEY (`deptId`) REFERENCES `department` (`id`),
  CONSTRAINT `fkUserRole` FOREIGN KEY (`roleId`) REFERENCES `role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE `faceFeature` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '人脸特征ID',
  `userId` BIGINT NOT NULL COMMENT '用户ID',
  `featureData` TEXT NOT NULL COMMENT '人脸特征数据',
  `featureHash` VARCHAR(128) NOT NULL COMMENT '特征摘要',
  `encryptFlag` INT NOT NULL DEFAULT 1 COMMENT '加密标记',
  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY `idxFaceFeatureUserId` (`userId`),
  UNIQUE KEY `ukFaceFeatureHash` (`featureHash`),
  CONSTRAINT `fkFaceFeatureUser` FOREIGN KEY (`userId`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人脸特征表';

CREATE TABLE `rule` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '规则ID',
  `name` VARCHAR(100) NOT NULL COMMENT '规则名称',
  `startTime` TIME NOT NULL COMMENT '上班时间',
  `endTime` TIME NOT NULL COMMENT '下班时间',
  `lateThreshold` INT NOT NULL DEFAULT 10 COMMENT '迟到阈值',
  `earlyThreshold` INT NOT NULL DEFAULT 10 COMMENT '早退阈值',
  `repeatLimit` INT NOT NULL DEFAULT 3 COMMENT '重复打卡阈值',
  `status` INT NOT NULL DEFAULT 1 COMMENT '规则状态',
  UNIQUE KEY `ukRuleName` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤规则表';

CREATE TABLE `promptTemplate` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '提示词模板ID',
  `code` VARCHAR(50) NOT NULL COMMENT '模板编码',
  `name` VARCHAR(100) NOT NULL COMMENT '模板名称',
  `sceneType` VARCHAR(50) NOT NULL COMMENT '适用场景',
  `version` VARCHAR(50) NOT NULL COMMENT '模板版本',
  `content` TEXT NOT NULL COMMENT '模板内容',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '模板状态',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `ukPromptTemplateCodeVersion` (`code`, `version`),
  KEY `idxPromptTemplateSceneTypeStatus` (`sceneType`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提示词模板表';

CREATE TABLE `attendanceRecord` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '打卡记录ID',
  `userId` BIGINT NOT NULL COMMENT '用户ID',
  `checkTime` DATETIME NOT NULL COMMENT '打卡时间',
  `checkType` VARCHAR(20) NOT NULL COMMENT '打卡类型',
  `deviceId` VARCHAR(64) NOT NULL COMMENT '设备ID',
  `ipAddr` VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
  `location` VARCHAR(255) DEFAULT NULL COMMENT '打卡地点',
  `longitude` DECIMAL(10,6) DEFAULT NULL COMMENT '打卡经度快照',
  `latitude` DECIMAL(10,6) DEFAULT NULL COMMENT '打卡纬度快照',
  `faceScore` DECIMAL(5,2) DEFAULT NULL COMMENT '人脸相似度',
  `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '打卡状态',
  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY `idxAttendanceRecordUserTime` (`userId`, `checkTime`),
  KEY `idxAttendanceRecordDeviceTime` (`deviceId`, `checkTime`),
  CONSTRAINT `fkAttendanceRecordUser` FOREIGN KEY (`userId`) REFERENCES `user` (`id`),
  CONSTRAINT `fkAttendanceRecordDevice` FOREIGN KEY (`deviceId`) REFERENCES `device` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打卡记录表';

CREATE TABLE `attendanceRepair` (
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

CREATE TABLE `attendanceException` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '异常记录ID',
  `recordId` BIGINT NOT NULL COMMENT '打卡记录ID',
  `userId` BIGINT NOT NULL COMMENT '用户ID',
  `type` VARCHAR(50) NOT NULL COMMENT '异常类型',
  `riskLevel` VARCHAR(20) NOT NULL COMMENT '风险等级',
  `sourceType` VARCHAR(20) NOT NULL DEFAULT 'RULE' COMMENT '来源类型',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '异常描述',
  `processStatus` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态',
  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY `idxAttendanceExceptionUserTime` (`userId`, `createTime`),
  KEY `idxAttendanceExceptionProcessStatus` (`processStatus`),
  CONSTRAINT `fkAttendanceExceptionRecord` FOREIGN KEY (`recordId`) REFERENCES `attendanceRecord` (`id`),
  CONSTRAINT `fkAttendanceExceptionUser` FOREIGN KEY (`userId`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异常记录表';

CREATE TABLE `exceptionAnalysis` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分析结果ID',
  `exceptionId` BIGINT NOT NULL COMMENT '异常记录ID',
  `promptTemplateId` BIGINT DEFAULT NULL COMMENT '提示词模板ID',
  `inputSummary` TEXT DEFAULT NULL COMMENT '输入摘要',
  `modelResult` TEXT DEFAULT NULL COMMENT '模型输出',
  `modelConclusion` VARCHAR(100) DEFAULT NULL COMMENT '模型结构化结论',
  `confidenceScore` DECIMAL(5,2) DEFAULT NULL COMMENT '置信度',
  `decisionReason` TEXT DEFAULT NULL COMMENT '判定依据',
  `suggestion` VARCHAR(255) DEFAULT NULL COMMENT '处理建议',
  `reasonSummary` TEXT DEFAULT NULL COMMENT '理由摘要',
  `actionSuggestion` VARCHAR(255) DEFAULT NULL COMMENT '行动建议',
  `similarCaseSummary` TEXT DEFAULT NULL COMMENT '相似案例摘要',
  `promptVersion` VARCHAR(50) DEFAULT NULL COMMENT '提示词版本',
  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY `ukExceptionAnalysisExceptionId` (`exceptionId`),
  CONSTRAINT `fkExceptionAnalysisException` FOREIGN KEY (`exceptionId`) REFERENCES `attendanceException` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异常分析表';

CREATE TABLE `warningRecord` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '预警记录ID',
  `exceptionId` BIGINT NOT NULL COMMENT '异常记录ID',
  `type` VARCHAR(50) NOT NULL COMMENT '预警类型',
  `level` VARCHAR(20) NOT NULL COMMENT '预警等级',
  `status` VARCHAR(20) NOT NULL DEFAULT 'UNPROCESSED' COMMENT '预警状态',
  `priorityScore` DECIMAL(5,2) DEFAULT NULL COMMENT '优先级分值',
  `aiSummary` TEXT DEFAULT NULL COMMENT 'AI预警摘要',
  `disposeSuggestion` VARCHAR(255) DEFAULT NULL COMMENT '处置建议',
  `decisionSource` VARCHAR(20) NOT NULL DEFAULT 'MODEL_FUSION' COMMENT '决策来源',
  `sendTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  UNIQUE KEY `ukWarningRecordExceptionId` (`exceptionId`),
  KEY `idxWarningRecordExceptionId` (`exceptionId`),
  CONSTRAINT `fkWarningRecordException` FOREIGN KEY (`exceptionId`) REFERENCES `attendanceException` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警记录表';

CREATE TABLE `riskLevel` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '风险等级ID',
  `code` VARCHAR(20) NOT NULL COMMENT '风险等级编码',
  `name` VARCHAR(100) NOT NULL COMMENT '风险等级名称',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '风险等级说明',
  `status` INT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `ukRiskLevelCode` (`code`),
  KEY `idxRiskLevelStatus` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险等级表';

CREATE TABLE `reviewRecord` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '复核记录ID',
  `exceptionId` BIGINT NOT NULL COMMENT '异常记录ID',
  `reviewUserId` BIGINT NOT NULL COMMENT '复核用户ID',
  `result` VARCHAR(20) NOT NULL COMMENT '复核结果',
  `comment` VARCHAR(255) DEFAULT NULL COMMENT '复核意见',
  `aiReviewSuggestion` TEXT DEFAULT NULL COMMENT 'AI复核建议',
  `similarCaseSummary` TEXT DEFAULT NULL COMMENT '相似案例摘要',
  `feedbackTag` VARCHAR(50) DEFAULT NULL COMMENT '反馈标签',
  `strategyFeedback` VARCHAR(255) DEFAULT NULL COMMENT '策略反馈说明',
  `reviewTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '复核时间',
  KEY `idxReviewRecordExceptionId` (`exceptionId`),
  CONSTRAINT `fkReviewRecordException` FOREIGN KEY (`exceptionId`) REFERENCES `attendanceException` (`id`),
  CONSTRAINT `fkReviewRecordUser` FOREIGN KEY (`reviewUserId`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='复核记录表';

CREATE TABLE `exceptionType` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '异常类型配置ID',
  `code` VARCHAR(50) NOT NULL COMMENT '异常类型编码',
  `name` VARCHAR(100) NOT NULL COMMENT '异常类型名称',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '异常类型说明',
  `status` INT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `ukExceptionTypeCode` (`code`),
  KEY `idxExceptionTypeStatus` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异常类型配置表';

CREATE TABLE `modelCallLog` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '模型调用日志ID',
  `businessType` VARCHAR(50) NOT NULL COMMENT '业务类型',
  `businessId` BIGINT NOT NULL COMMENT '业务编号',
  `promptTemplateId` BIGINT DEFAULT NULL COMMENT '提示词模板ID',
  `inputSummary` TEXT DEFAULT NULL COMMENT '输入摘要',
  `outputSummary` TEXT DEFAULT NULL COMMENT '输出摘要',
  `status` VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT '调用状态',
  `latencyMs` INT DEFAULT NULL COMMENT '耗时毫秒数',
  `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT '错误信息',
  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY `idxModelCallLogBusiness` (`businessType`, `businessId`),
  KEY `idxModelCallLogPromptTemplateId` (`promptTemplateId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型调用日志表';

CREATE TABLE `decisionTrace` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '决策追踪ID',
  `businessType` VARCHAR(50) NOT NULL COMMENT '业务类型',
  `businessId` BIGINT NOT NULL COMMENT '业务编号',
  `ruleResult` TEXT DEFAULT NULL COMMENT '规则结果',
  `modelResult` TEXT DEFAULT NULL COMMENT '模型结果',
  `finalDecision` TEXT DEFAULT NULL COMMENT '最终决策',
  `confidenceScore` DECIMAL(5,2) DEFAULT NULL COMMENT '决策置信度',
  `decisionReason` TEXT DEFAULT NULL COMMENT '决策依据',
  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY `idxDecisionTraceBusiness` (`businessType`, `businessId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='决策追踪表';

CREATE TABLE `operationLog` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
  `userId` BIGINT DEFAULT NULL COMMENT '用户ID',
  `type` VARCHAR(50) NOT NULL COMMENT '操作类型',
  `content` VARCHAR(255) NOT NULL COMMENT '操作内容',
  `operationTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  KEY `idxOperationLogUserTime` (`userId`, `operationTime`),
  CONSTRAINT `fkOperationLogUser` FOREIGN KEY (`userId`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

INSERT INTO `department` (`name`, `description`) VALUES
('综合管理部', '默认管理部门'),
('技术部', '技术研发部门'),
('行政部', '行政支持部门'),
('财务部', '财务核算与预算管理部门'),
('人力资源部', '招聘培训与员工关系管理部门'),
('市场部', '市场推广与品牌宣传部门'),
('运营部', '业务运营与流程协调部门'),
('客服部', '客户服务与工单处理部门'),
('法务合规部', '合同审核与合规管理部门');

INSERT INTO `role` (`code`, `name`, `description`, `status`) VALUES
('ADMIN', '管理员', '系统管理员角色', 1),
('EMPLOYEE', '员工', '普通员工角色', 1);

INSERT INTO `device` (`id`, `name`, `location`, `longitude`, `latitude`, `status`, `description`) VALUES
('DEV-001', '前台考勤机1', '办公区A', 116.397128, 39.916527, 1, '默认正常设备'),
('DEV-002', '前台考勤机2', '办公区B', 116.407396, 39.904200, 1, '默认正常设备'),
('DEV-009', '临时设备', '外部区域', 121.473701, 31.230416, 1, '用于异常场景测试');

INSERT INTO `rule` (`name`, `startTime`, `endTime`, `lateThreshold`, `earlyThreshold`, `repeatLimit`, `status`) VALUES
('默认考勤规则', '09:00:00', '18:00:00', 10, 10, 3, 1);

INSERT INTO `riskLevel` (`code`, `name`, `description`, `status`) VALUES
('HIGH', '高风险', '需要优先人工复核', 1),
('MEDIUM', '中风险', '建议尽快关注并结合历史记录判断', 1),
('LOW', '低风险', '记录留档并持续观察', 1);

INSERT INTO `exceptionType` (`code`, `name`, `description`, `status`) VALUES
('PROXY_CHECKIN', '疑似代打卡', '疑似由他人代为完成考勤打卡', 1),
('LATE', '迟到', '超过上班时间阈值的异常打卡', 1),
('EARLY_LEAVE', '早退', '早于下班时间阈值的异常打卡', 1),
('REPEAT_CHECK', '重复打卡', '短时间内重复提交同类打卡', 1),
('ILLEGAL_TIME', '非法时间打卡', '发生在非法时间段的异常打卡', 1),
('MULTI_LOCATION_CONFLICT', '多地点异常', '短时间内在多个地点完成打卡，疑似空间冲突', 1);
