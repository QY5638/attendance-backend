package com.quyong.attendance.module.warning.vo;

import java.math.BigDecimal;

public class WarningAdviceVO {

    private Long id;
    private Long exceptionId;
    private BigDecimal priorityScore;
    private String aiSummary;
    private String disposeSuggestion;
    private String decisionSource;

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

    public BigDecimal getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(BigDecimal priorityScore) {
        this.priorityScore = priorityScore;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public String getDisposeSuggestion() {
        return disposeSuggestion;
    }

    public void setDisposeSuggestion(String disposeSuggestion) {
        this.disposeSuggestion = disposeSuggestion;
    }

    public String getDecisionSource() {
        return decisionSource;
    }

    public void setDecisionSource(String decisionSource) {
        this.decisionSource = decisionSource;
    }
}
