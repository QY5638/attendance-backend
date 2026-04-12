package com.quyong.attendance.module.warning.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WarningAdviceVO {

    private Long id;
    private Long exceptionId;
    private String type;
    private String level;
    private String status;
    private BigDecimal priorityScore;
    private String aiSummary;
    private String disposeSuggestion;
    private String decisionSource;
    private LocalDateTime sendTime;
    private Long userId;
    private String username;
    private String realName;
    private Long recordId;
    private LocalDateTime checkTime;
    private String checkType;
    private String location;
    private String deviceId;
    private String deviceInfo;
    private String terminalId;
    private String ipAddr;
    private BigDecimal faceScore;
    private String recordStatus;
    private String exceptionType;
    private String exceptionSourceType;
    private String exceptionProcessStatus;
    private String exceptionDescription;
    private LocalDateTime exceptionCreateTime;
    private String modelConclusion;
    private BigDecimal confidenceScore;
    private String decisionReason;
    private String similarCaseSummary;
    private String reviewResult;
    private String reviewComment;
    private String reviewAiSuggestion;
    private String reviewUserName;
    private LocalDateTime reviewTime;

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public LocalDateTime getCheckTime() {
        return checkTime;
    }

    public void setCheckTime(LocalDateTime checkTime) {
        this.checkTime = checkTime;
    }

    public String getCheckType() {
        return checkType;
    }

    public void setCheckType(String checkType) {
        this.checkType = checkType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public String getIpAddr() {
        return ipAddr;
    }

    public void setIpAddr(String ipAddr) {
        this.ipAddr = ipAddr;
    }

    public BigDecimal getFaceScore() {
        return faceScore;
    }

    public void setFaceScore(BigDecimal faceScore) {
        this.faceScore = faceScore;
    }

    public String getRecordStatus() {
        return recordStatus;
    }

    public void setRecordStatus(String recordStatus) {
        this.recordStatus = recordStatus;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public String getExceptionSourceType() {
        return exceptionSourceType;
    }

    public void setExceptionSourceType(String exceptionSourceType) {
        this.exceptionSourceType = exceptionSourceType;
    }

    public String getExceptionProcessStatus() {
        return exceptionProcessStatus;
    }

    public void setExceptionProcessStatus(String exceptionProcessStatus) {
        this.exceptionProcessStatus = exceptionProcessStatus;
    }

    public String getExceptionDescription() {
        return exceptionDescription;
    }

    public void setExceptionDescription(String exceptionDescription) {
        this.exceptionDescription = exceptionDescription;
    }

    public LocalDateTime getExceptionCreateTime() {
        return exceptionCreateTime;
    }

    public void setExceptionCreateTime(LocalDateTime exceptionCreateTime) {
        this.exceptionCreateTime = exceptionCreateTime;
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

    public String getSimilarCaseSummary() {
        return similarCaseSummary;
    }

    public void setSimilarCaseSummary(String similarCaseSummary) {
        this.similarCaseSummary = similarCaseSummary;
    }

    public String getReviewResult() {
        return reviewResult;
    }

    public void setReviewResult(String reviewResult) {
        this.reviewResult = reviewResult;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public String getReviewAiSuggestion() {
        return reviewAiSuggestion;
    }

    public void setReviewAiSuggestion(String reviewAiSuggestion) {
        this.reviewAiSuggestion = reviewAiSuggestion;
    }

    public String getReviewUserName() {
        return reviewUserName;
    }

    public void setReviewUserName(String reviewUserName) {
        this.reviewUserName = reviewUserName;
    }

    public LocalDateTime getReviewTime() {
        return reviewTime;
    }

    public void setReviewTime(LocalDateTime reviewTime) {
        this.reviewTime = reviewTime;
    }
}
