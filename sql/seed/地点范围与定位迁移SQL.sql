USE `system`;
SET NAMES utf8mb4;

-- 说明：
-- 1. 本脚本用于把打卡地点升级为“地点 + 半径”模式。
-- 2. attendanceRecord 额外记录客户端坐标，用于后端判断员工是否在地点范围内。
-- 3. 兼容旧数据时，默认给现有地点设置 30 米半径。

ALTER TABLE `device`
  ADD COLUMN `radiusMeters` INT NOT NULL DEFAULT 30 COMMENT '打卡半径（米）' AFTER `latitude`;

ALTER TABLE `attendanceRecord`
  ADD COLUMN `terminalId` VARCHAR(64) DEFAULT NULL COMMENT '本机标识码' AFTER `deviceInfo`,
  ADD COLUMN `clientLongitude` DECIMAL(10,6) DEFAULT NULL COMMENT '客户端经度' AFTER `location`,
  ADD COLUMN `clientLatitude` DECIMAL(10,6) DEFAULT NULL COMMENT '客户端纬度' AFTER `clientLongitude`;

UPDATE `device` SET `radiusMeters` = 30 WHERE `radiusMeters` IS NULL OR `radiusMeters` <= 0 OR `radiusMeters` > 50;
