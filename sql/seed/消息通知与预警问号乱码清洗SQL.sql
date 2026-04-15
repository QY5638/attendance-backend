SET NAMES utf8mb4;

-- 说明：
-- 1. 本脚本用于清洗历史消息通知、预警摘要、交互时间线中已经落库为“????”或“？？？？”的内容。
-- 2. 仅处理“去掉空白和常见标点后全部由 ? / ？ 组成”的记录，避免误伤正常内容。
-- 3. 建议先在测试库验证，再在正式库执行。

UPDATE notificationRecord
SET content = CASE
    WHEN category = 'REQUEST_EXPLANATION' THEN '历史说明请求内容无法直接显示，请联系管理员重新发起说明请求。'
    WHEN category = 'EMPLOYEE_REPLY' THEN '历史员工说明内容无法直接显示，请联系员工重新补充说明。'
    WHEN category = 'REVIEW_RESULT' THEN '历史复核结果说明无法直接显示，请联系管理员查看原始记录。'
    ELSE '历史通知内容无法直接显示，请联系管理员查看原始记录。'
END
WHERE content IS NOT NULL
  AND content <> ''
  AND REGEXP_REPLACE(content, '[[:space:]，。！？；：,.!?;:（）()]+', '') REGEXP '^[？?]{3,}$';

UPDATE warningInteractionRecord
SET content = CASE
    WHEN messageType = 'REQUEST_EXPLANATION' THEN '历史说明请求内容无法直接显示，请联系管理员重新发起说明请求。'
    WHEN messageType = 'EMPLOYEE_REPLY' THEN '历史员工说明内容无法直接显示，请联系员工重新补充说明。'
    WHEN messageType = 'REVIEW_RESULT' THEN '历史复核结果说明无法直接显示，请联系管理员查看原始记录。'
    ELSE '历史处理记录内容无法直接显示，请联系管理员查看原始记录。'
END
WHERE content IS NOT NULL
  AND content <> ''
  AND REGEXP_REPLACE(content, '[[:space:]，。！？；：,.!?;:（）()]+', '') REGEXP '^[？?]{3,}$';

UPDATE warningRecord
SET aiSummary = '历史系统摘要无法直接显示，请联系管理员查看原始记录。'
WHERE aiSummary IS NOT NULL
  AND aiSummary <> ''
  AND REGEXP_REPLACE(aiSummary, '[[:space:]，。！？；：,.!?;:（）()]+', '') REGEXP '^[？?]{3,}$';

UPDATE warningRecord
SET disposeSuggestion = '历史处置建议无法直接显示，请联系管理员查看原始记录。'
WHERE disposeSuggestion IS NOT NULL
  AND disposeSuggestion <> ''
  AND REGEXP_REPLACE(disposeSuggestion, '[[:space:]，。！？；：,.!?;:（）()]+', '') REGEXP '^[？?]{3,}$';

UPDATE attendanceException
SET description = '历史异常说明无法直接显示，请联系管理员查看原始记录。'
WHERE description IS NOT NULL
  AND description <> ''
  AND REGEXP_REPLACE(description, '[[:space:]，。！？；：,.!?;:（）()]+', '') REGEXP '^[？?]{3,}$';

UPDATE exceptionAnalysis
SET decisionReason = '历史判定依据无法直接显示，请联系管理员查看原始记录。'
WHERE decisionReason IS NOT NULL
  AND decisionReason <> ''
  AND REGEXP_REPLACE(decisionReason, '[[:space:]，。！？；：,.!?;:（）()]+', '') REGEXP '^[？?]{3,}$';

UPDATE exceptionAnalysis
SET similarCaseSummary = '历史相似案例摘要无法直接显示，请联系管理员查看原始记录。'
WHERE similarCaseSummary IS NOT NULL
  AND similarCaseSummary <> ''
  AND REGEXP_REPLACE(similarCaseSummary, '[[:space:]，。！？；：,.!?;:（）()]+', '') REGEXP '^[？?]{3,}$';

UPDATE reviewRecord
SET comment = '历史复核意见无法直接显示，请联系管理员查看原始记录。'
WHERE comment IS NOT NULL
  AND comment <> ''
  AND REGEXP_REPLACE(comment, '[[:space:]，。！？；：,.!?;:（）()]+', '') REGEXP '^[？?]{3,}$';

UPDATE reviewRecord
SET aiReviewSuggestion = '历史复核建议无法直接显示，请联系管理员查看原始记录。'
WHERE aiReviewSuggestion IS NOT NULL
  AND aiReviewSuggestion <> ''
  AND REGEXP_REPLACE(aiReviewSuggestion, '[[:space:]，。！？；：,.!?;:（）()]+', '') REGEXP '^[？?]{3,}$';
