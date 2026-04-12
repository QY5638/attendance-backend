USE `system`;
SET NAMES utf8mb4;

ALTER TABLE `attendanceRecord`
  ADD COLUMN IF NOT EXISTS `deviceInfo` VARCHAR(128) DEFAULT NULL COMMENT '电脑设备信息' AFTER `deviceId`;

UPDATE `device`
SET `name` = '办公区A主点位',
    `location` = '办公区A',
    `description` = '办公区A默认打卡地点'
WHERE `id` = 'DEV-001';

UPDATE `device`
SET `name` = '办公区B主点位',
    `location` = '办公区B',
    `description` = '办公区B默认打卡地点'
WHERE `id` = 'DEV-002';

UPDATE `device`
SET `name` = '外部区域测试点',
    `location` = '外部区域',
    `description` = '用于异常场景演示的打卡地点'
WHERE `id` = 'DEV-009';

UPDATE `attendanceRecord`
SET `deviceInfo` = CASE MOD(`userId`, 6)
  WHEN 0 THEN '系统 Windows 11 / 浏览器 Google Chrome / 分辨率 1920x1080'
  WHEN 1 THEN '系统 Windows 10 / 浏览器 Microsoft Edge / 分辨率 1920x1080'
  WHEN 2 THEN '系统 macOS / 浏览器 Safari / 分辨率 2560x1600'
  WHEN 3 THEN '系统 Windows 11 / 浏览器 Mozilla Firefox / 分辨率 2560x1440'
  WHEN 4 THEN '系统 Windows 10 / 浏览器 Google Chrome / 分辨率 1366x768'
  ELSE '系统 Windows 11 / 浏览器 Microsoft Edge / 分辨率 1600x900'
END
WHERE (`deviceInfo` IS NULL OR `deviceInfo` = '')
  AND `deviceId` IN ('DEV-001', 'DEV-002', 'DEV-009');

UPDATE `attendanceException`
SET `description` = REPLACE(REPLACE(REPLACE(`description`, '外部设备', '外部电脑设备'), '设备与地点', '电脑设备与地点'), '设备', '电脑设备')
WHERE `description` LIKE '%设备%';

UPDATE `exceptionAnalysis`
SET `inputSummary` = REPLACE(REPLACE(REPLACE(`inputSummary`, '外部设备', '外部电脑设备'), '设备与地点', '电脑设备与地点'), '设备', '电脑设备')
WHERE `inputSummary` LIKE '%设备%';

UPDATE `exceptionAnalysis`
SET `decisionReason` = REPLACE(REPLACE(REPLACE(`decisionReason`, '外部设备', '外部电脑设备'), '设备与地点', '电脑设备与地点'), '设备', '电脑设备')
WHERE `decisionReason` LIKE '%设备%';

UPDATE `exceptionAnalysis`
SET `reasonSummary` = REPLACE(REPLACE(REPLACE(`reasonSummary`, '外部设备', '外部电脑设备'), '设备与地点', '电脑设备与地点'), '设备', '电脑设备')
WHERE `reasonSummary` LIKE '%设备%';

UPDATE `exceptionAnalysis`
SET `actionSuggestion` = REPLACE(REPLACE(REPLACE(`actionSuggestion`, '外部设备', '外部电脑设备'), '设备与地点', '电脑设备与地点'), '设备', '电脑设备')
WHERE `actionSuggestion` LIKE '%设备%';

UPDATE `exceptionAnalysis`
SET `similarCaseSummary` = REPLACE(REPLACE(REPLACE(`similarCaseSummary`, '外部设备', '外部电脑设备'), '设备与地点', '电脑设备与地点'), '设备', '电脑设备')
WHERE `similarCaseSummary` LIKE '%设备%';

UPDATE `warningRecord`
SET `aiSummary` = REPLACE(REPLACE(REPLACE(`aiSummary`, '外部设备', '外部电脑设备'), '设备与地点', '电脑设备与地点'), '设备', '电脑设备')
WHERE `aiSummary` LIKE '%设备%';

UPDATE `warningRecord`
SET `disposeSuggestion` = REPLACE(REPLACE(REPLACE(`disposeSuggestion`, '外部设备', '外部电脑设备'), '设备与地点', '电脑设备与地点'), '设备', '电脑设备')
WHERE `disposeSuggestion` LIKE '%设备%';

UPDATE `reviewRecord`
SET `aiReviewSuggestion` = REPLACE(REPLACE(REPLACE(`aiReviewSuggestion`, '外部设备', '外部电脑设备'), '设备与地点', '电脑设备与地点'), '设备', '电脑设备')
WHERE `aiReviewSuggestion` LIKE '%设备%';

UPDATE `reviewRecord`
SET `similarCaseSummary` = REPLACE(REPLACE(REPLACE(`similarCaseSummary`, '外部设备', '外部电脑设备'), '设备与地点', '电脑设备与地点'), '设备', '电脑设备')
WHERE `similarCaseSummary` LIKE '%设备%';
