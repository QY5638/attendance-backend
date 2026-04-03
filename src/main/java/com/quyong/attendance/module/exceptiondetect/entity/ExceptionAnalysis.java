package com.quyong.attendance.module.exceptiondetect.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("exceptionAnalysis")
public class ExceptionAnalysis {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("exceptionId")
    private Long exceptionId;

    @TableField("promptTemplateId")
    private Long promptTemplateId;

    @TableField("inputSummary")
    private String inputSummary;

    @TableField("modelResult")
    private String modelResult;

    @TableField("modelConclusion")
    private String modelConclusion;

    @TableField("confidenceScore")
    private BigDecimal confidenceScore;

    @TableField("decisionReason")
    private String decisionReason;

    @TableField("suggestion")
    private String suggestion;

    @TableField("reasonSummary")
    private String reasonSummary;

    @TableField("actionSuggestion")
    private String actionSuggestion;

    @TableField("similarCaseSummary")
    private String similarCaseSummary;

    @TableField("promptVersion")
    private String promptVersion;

    @TableField("createTime")
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getExceptionId() {
        return exceptionId;
    }

    public void setExceptionId(Long exceptionId) {
        this.exceptionId = exceptionId;
    }

    public Long getPromptTemplateId() {
        return promptTemplateId;
    }

    public void setPromptTemplateId(Long promptTemplateId) {
        this.promptTemplateId = promptTemplateId;
    }

    public String getInputSummary() {
        return inputSummary;
    }

    public void setInputSummary(String inputSummary) {
        this.inputSummary = inputSummary;
    }

    public String getModelResult() {
        return modelResult;
    }

    public void setModelResult(String modelResult) {
        this.modelResult = modelResult;
    }

    public String getModelConclusion() {
        return modelConclusion;
    }

    public void setModelConclusion(String modelConclusion) {
        this.modelConclusion = modelConclusion;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(BigDecimal confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public String getReasonSummary() {
        return reasonSummary;
    }

    public void setReasonSummary(String reasonSummary) {
        this.reasonSummary = reasonSummary;
    }

    public String getActionSuggestion() {
        return actionSuggestion;
    }

    public void setActionSuggestion(String actionSuggestion) {
        this.actionSuggestion = actionSuggestion;
    }

    public String getSimilarCaseSummary() {
        return similarCaseSummary;
    }

    public void setSimilarCaseSummary(String similarCaseSummary) {
        this.similarCaseSummary = similarCaseSummary;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
