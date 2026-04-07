# Phase 4 提示词模板输入可观察性设计

## 背景

Phase 4 还剩最后一块阶段完成条件：`提示词模板变更对模型调用或分析输入有可观察影响`。当前复杂异常分析会读取启用中的 `PromptTemplate`，并把 `promptContent`、`promptVersion` 传给 `ModelGateway`，但写入 `ExceptionAnalysis` 与 `modelCallLog` 的 `inputSummary` 仍只包含记录、风险特征与 Qwen 提供方信息，缺少对“本次分析到底用了哪版模板内容”的稳定可观察证据。

前两轮已补齐：

- `Qwen 真实接入 -> 模型调用日志可观察`
- `复核反馈 -> decisionTrace/治理说明`

本轮只推进最后一条最小闭环：`提示词模板变更 -> complexCheck 输入/日志可观察影响`。

## 目标

当启用中的复杂异常提示词模板内容发生变化时，新的 `complexCheck()` 成功分析结果应在 `inputSummary` / `modelCallLog` 中留下稳定、可比对的模板上下文标识，使测试与排查都能直接看出：本次分析输入确实受模板变化影响。

## 方案

### 1. 推荐方案：向输入摘要追加模板版本与内容指纹

在复杂异常分析主链中，为当前模板生成两类上下文信息：

- `promptVersion=<template.version>`
- `promptFingerprint=<stableDigest(template.content)>`

然后把这两个字段追加到分析使用的 `inputSummary`。由于当前 `inputSummary` 同时会：

- 作为 `ModelInvokeRequest.inputSummary` 进入模型调用
- 写入 `exceptionAnalysis.inputSummary`
- 写入 `modelCallLog.inputSummary`

所以模板变化会同时体现在“分析输入”和“模型调用日志”两个可观察点上，满足 Phase 4 的剩余阶段条件。

### 2. 指纹生成策略

采用模板内容的稳定摘要值，不直接把模板正文写进日志：

- 输入：`promptTemplate.content`
- 输出：固定长度十六进制摘要串

这样既能证明模板内容不同导致指纹变化，又不会把完整提示词正文扩散到日志记录里。

### 3. 变更边界

- 仅修改 `ExceptionAnalysisOrchestratorImpl`
- 不改数据库结构
- 不改控制器契约
- 不改前端
- 不改 `PromptTemplateMapper` 查询逻辑

### 4. 不采用的方案

- 只在 `modelCallLog` 追加模板标识：无法直接证明分析输入也受影响。
- 把模板全文直接拼进 `inputSummary`：日志过长且会扩散模板正文，不适合作为最小闭环。
- 新增独立模板审计表：超出本轮范围。

## 测试策略

严格按 TDD，先补一条最小失败测试：

1. 插入一版启用中的复杂异常模板。
2. 触发 `complexCheck()` 成功路径。
3. 断言 `modelCallLog.inputSummary` 中包含 `promptVersion` 与 `promptFingerprint`。
4. 再做最小实现让测试通过。

若需要进一步锁定“模板变更会改变指纹”，再补第二条失败测试；但本轮优先以一条最小闭环用例收口。

## 约束

- 继续只在 `backend/.worktrees/phase4-qwen-log-observability` 内开发。
- 保持现有 Qwen 观测字段与反馈治理 trace 不回退。
- 不引入与模板管理前端、Mock 降级、Phase 5 文档交付无关的扩展改动。
