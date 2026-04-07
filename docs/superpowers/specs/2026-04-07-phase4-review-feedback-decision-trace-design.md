# Phase 4 复核反馈决策追踪设计

## 背景

Phase 4 的剩余硬缺口集中在“反馈治理链路仍不完整”。当前 `ReviewServiceImpl.feedback()` 只会把 `feedbackTag` 与 `strategyFeedback` 写回 `reviewRecord`，但不会进入 `decisionTrace` 或其他可审计治理说明，因此复核反馈仍停留在记录层，无法形成治理闭环。

上一轮已补齐 `Qwen 真实接入 -> 模型调用日志可观察`。本轮只继续推进下一条最小闭环：`复核反馈 -> decisionTrace/治理说明`。

## 目标

提交复核反馈成功后，同一异常要新增一条可查询、可审计的 `decisionTrace`，明确沉淀：

- 来自哪条 `reviewRecord`
- 归一化后的 `feedbackTag`
- 可选的 `strategyFeedback`

同时不能破坏现有复核辅助信息读取逻辑，避免反馈型 trace 覆盖原始规则/模型判因。

## 方案

### 1. 推荐方案：新增一条反馈 trace

在 `ReviewServiceImpl.feedback()` 更新 `reviewRecord` 成功后，为同一 `ATTENDANCE_EXCEPTION + exceptionId` 新增一条 `decisionTrace`：

- `businessType = ATTENDANCE_EXCEPTION`
- `businessId = reviewRecord.exceptionId`
- `finalDecision = REVIEW_FEEDBACK`
- `ruleResult = reviewId=<reviewId>, feedbackTag=<normalizedFeedbackTag>`
- `decisionReason = strategyFeedback=<strategyFeedback>`；若未填写策略反馈，则写固定说明 `strategyFeedback=未填写`
- `modelResult = null`
- `confidenceScore = null`

这样可以保留原始规则/模型决策 trace，同时把后续治理反馈追加为新的审计节点，不覆盖历史证据。

### 2. 兼容保护：复核辅助信息忽略反馈 trace

`ReviewServiceImpl.getAssistant()` 当前默认取最后一条 trace 作为判因来源。若直接新增反馈 trace，会导致复核辅助信息被“治理反馈”覆盖。

最小兼容策略：

- 在 trace 列表中从后往前寻找第一条 `finalDecision != REVIEW_FEEDBACK` 的记录作为 assistant 判因来源。
- 若只剩反馈 trace 但存在 `ExceptionAnalysis`，则退回 `ExceptionAnalysis`。
- 若既无有效决策 trace 又无 `ExceptionAnalysis`，保持当前错误行为不变。

### 3. 不采用的方案

- 更新现有最后一条 trace：会污染原始规则/模型证据，审计性较差。
- 新建独立反馈治理表：改动面超出“最小闭环”，不符合本轮范围。

## 测试策略

严格按 TDD 分两步：

1. 先补失败测试，锁定 `/api/review/feedback` 成功后必须新增一条 `decisionTrace`，且包含 `reviewId`、归一化后的 `feedbackTag`、`strategyFeedback`。
2. 再补失败测试，锁定新增反馈 trace 后，`/api/review/{exceptionId}/assistant` 仍返回原始规则/模型判因，不被 `REVIEW_FEEDBACK` 污染。

## 约束

- 不改数据库结构。
- 不改前端接口与页面。
- 不把反馈 trace 混入原始模型/规则决策展示语义。
- 仅在现有后端 worktree `backend/.worktrees/phase4-qwen-log-observability` 内继续开发。
