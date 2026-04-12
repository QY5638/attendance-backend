CREATE TABLE IF NOT EXISTS `faceRegisterApproval` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '人脸重录申请ID',
  `userId` BIGINT NOT NULL COMMENT '申请用户ID',
  `reason` VARCHAR(255) NOT NULL COMMENT '申请说明',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '申请状态',
  `reviewUserId` BIGINT DEFAULT NULL COMMENT '审批用户ID',
  `reviewComment` VARCHAR(255) DEFAULT NULL COMMENT '审批备注',
  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `reviewTime` DATETIME DEFAULT NULL COMMENT '审批时间',
  KEY `idxFaceRegisterApprovalUserTime` (`userId`, `createTime`),
  KEY `idxFaceRegisterApprovalStatus` (`status`, `createTime`),
  CONSTRAINT `fkFaceRegisterApprovalUser` FOREIGN KEY (`userId`) REFERENCES `user` (`id`),
  CONSTRAINT `fkFaceRegisterApprovalReviewUser` FOREIGN KEY (`reviewUserId`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人脸重录申请表';
