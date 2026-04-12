-- 历史异常决策追踪回填 SQL
-- 用途：为缺少 ATTENDANCE_EXCEPTION 决策追踪的历史异常补齐最小可审计记录
-- 特点：幂等，可重复执行；仅回填当前缺失的记录，不影响已有决策追踪
-- 执行建议：使用 mysql 客户端直接 source 此文件，避免通过 PowerShell 管道传输时把中文字面量写成 ?

START TRANSACTION;

-- 执行前查看当前缺口
SELECT COUNT(*) AS missingTraceCountBefore
FROM `attendanceException` ae
LEFT JOIN `decisionTrace` dt
  ON dt.`businessType` = 'ATTENDANCE_EXCEPTION'
 AND dt.`businessId` = ae.`id`
WHERE dt.`id` IS NULL;

INSERT INTO `decisionTrace` (
  `businessType`,
  `businessId`,
  `ruleResult`,
  `modelResult`,
  `finalDecision`,
  `confidenceScore`,
  `decisionReason`,
  `createTime`
)
SELECT
  'ATTENDANCE_EXCEPTION' AS `businessType`,
  ae.`id` AS `businessId`,
  CASE
    WHEN ae.`sourceType` = 'RULE' THEN
      CASE ae.`type`
        WHEN 'LATE' THEN CONCAT(
          '规则判定迟到；签到时间=',
          COALESCE(DATE_FORMAT(ar.`checkTime`, '%Y-%m-%d %H:%i:%s'), '未提供'),
          '；规则依据=',
          COALESCE(NULLIF(TRIM(ea.`decisionReason`), ''), NULLIF(TRIM(ae.`description`), ''), '历史数据未保留更详细规则依据')
        )
        WHEN 'EARLY_LEAVE' THEN CONCAT(
          '规则判定早退；签退时间=',
          COALESCE(DATE_FORMAT(ar.`checkTime`, '%Y-%m-%d %H:%i:%s'), '未提供'),
          '；规则依据=',
          COALESCE(NULLIF(TRIM(ea.`decisionReason`), ''), NULLIF(TRIM(ae.`description`), ''), '历史数据未保留更详细规则依据')
        )
        WHEN 'REPEAT_CHECK' THEN CONCAT(
          '规则判定重复打卡；记录时间=',
          COALESCE(DATE_FORMAT(ar.`checkTime`, '%Y-%m-%d %H:%i:%s'), '未提供'),
          '；规则依据=',
          COALESCE(NULLIF(TRIM(ea.`decisionReason`), ''), NULLIF(TRIM(ae.`description`), ''), '历史数据未保留更详细规则依据')
        )
        WHEN 'ILLEGAL_TIME' THEN CONCAT(
          '规则判定非法时间打卡；记录时间=',
          COALESCE(DATE_FORMAT(ar.`checkTime`, '%Y-%m-%d %H:%i:%s'), '未提供'),
          '；规则依据=',
          COALESCE(NULLIF(TRIM(ea.`decisionReason`), ''), NULLIF(TRIM(ae.`description`), ''), '历史数据未保留更详细规则依据')
        )
        ELSE CONCAT(
          '规则判定异常；规则依据=',
          COALESCE(NULLIF(TRIM(ea.`decisionReason`), ''), NULLIF(TRIM(ae.`description`), ''), '历史数据未保留更详细规则依据')
        )
      END
    ELSE CONCAT(
      '历史回填规则特征：打卡地点编号=', COALESCE(NULLIF(TRIM(ar.`deviceId`), ''), '未提供'),
      '；打卡地点=', COALESCE(NULLIF(TRIM(ar.`location`), ''), '未提供'),
      '；服务端人脸分数=', COALESCE(CAST(ar.`faceScore` AS CHAR), '未提供'),
      '；客户端风险特征=历史数据未留存'
    )
  END AS `ruleResult`,
  CASE
    WHEN ae.`sourceType` IN ('MODEL', 'MODEL_FALLBACK') THEN NULLIF(TRIM(ea.`modelResult`), '')
    ELSE NULL
  END AS `modelResult`,
  COALESCE(NULLIF(TRIM(ea.`modelConclusion`), ''), ae.`type`) AS `finalDecision`,
  CASE
    WHEN ae.`sourceType` IN ('MODEL', 'MODEL_FALLBACK') THEN ea.`confidenceScore`
    ELSE NULL
  END AS `confidenceScore`,
  CASE
    WHEN ae.`sourceType` = 'RULE' THEN COALESCE(
      NULLIF(TRIM(ea.`decisionReason`), ''),
      NULLIF(TRIM(ea.`reasonSummary`), ''),
      NULLIF(TRIM(ae.`description`), ''),
      '历史规则异常回填决策依据'
    )
    ELSE COALESCE(
      NULLIF(TRIM(ea.`decisionReason`), ''),
      NULLIF(TRIM(ea.`reasonSummary`), ''),
      NULLIF(TRIM(ae.`description`), ''),
      '历史模型异常回填决策依据'
    )
  END AS `decisionReason`,
  COALESCE(ea.`createTime`, ae.`createTime`, NOW()) AS `createTime`
FROM `attendanceException` ae
LEFT JOIN `attendanceRecord` ar
  ON ar.`id` = ae.`recordId`
LEFT JOIN `exceptionAnalysis` ea
  ON ea.`id` = (
    SELECT ea2.`id`
    FROM `exceptionAnalysis` ea2
    WHERE ea2.`exceptionId` = ae.`id`
    ORDER BY ea2.`createTime` DESC, ea2.`id` DESC
    LIMIT 1
  )
LEFT JOIN `decisionTrace` dt
  ON dt.`businessType` = 'ATTENDANCE_EXCEPTION'
 AND dt.`businessId` = ae.`id`
WHERE dt.`id` IS NULL;

SELECT ROW_COUNT() AS insertedTraceCount;

-- 执行后复核剩余缺口
SELECT COUNT(*) AS missingTraceCountAfter
FROM `attendanceException` ae
LEFT JOIN `decisionTrace` dt
  ON dt.`businessType` = 'ATTENDANCE_EXCEPTION'
 AND dt.`businessId` = ae.`id`
WHERE dt.`id` IS NULL;

COMMIT;
