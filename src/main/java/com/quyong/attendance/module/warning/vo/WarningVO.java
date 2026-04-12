package com.quyong.attendance.module.warning.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WarningVO {

    private Long id;
    private Long exceptionId;
    private String exceptionType;
    private String type;
    private String level;
    private String status;
    private BigDecimal priorityScore;
    private String aiSummary;
    private String disposeSuggestion;
    private String decisionSource;
    private LocalDateTime sendTime;
    private Boolean overdue;
    private Long overdueMinutes;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public LocalDateTime getSendTime() {
        return sendTime;
    }

    public void setSendTime(LocalDateTime sendTime) {
        this.sendTime = sendTime;
    }

    public Boolean getOverdue() {
        return overdue;
    }

    public void setOverdue(Boolean overdue) {
        this.overdue = overdue;
    }

    public Long getOverdueMinutes() {
        return overdueMinutes;
    }

    public void setOverdueMinutes(Long overdueMinutes) {
        this.overdueMinutes = overdueMinutes;
    }
}
