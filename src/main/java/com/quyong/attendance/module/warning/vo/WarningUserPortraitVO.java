package com.quyong.attendance.module.warning.vo;

import java.time.LocalDateTime;

public class WarningUserPortraitVO {

    private Long userId;
    private String username;
    private String realName;
    private Long totalWarnings;
    private Long highRiskWarnings;
    private Long unprocessedWarnings;
    private Long overdueWarnings;
    private String riskTier;
    private String latestExceptionType;
    private String latestExceptionTypeName;
    private String latestWarningLevel;
    private LocalDateTime latestWarningTime;

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

    public Long getTotalWarnings() {
        return totalWarnings;
    }

    public void setTotalWarnings(Long totalWarnings) {
        this.totalWarnings = totalWarnings;
    }

    public Long getHighRiskWarnings() {
        return highRiskWarnings;
    }

    public void setHighRiskWarnings(Long highRiskWarnings) {
        this.highRiskWarnings = highRiskWarnings;
    }

    public Long getUnprocessedWarnings() {
        return unprocessedWarnings;
    }

    public void setUnprocessedWarnings(Long unprocessedWarnings) {
        this.unprocessedWarnings = unprocessedWarnings;
    }

    public Long getOverdueWarnings() {
        return overdueWarnings;
    }

    public void setOverdueWarnings(Long overdueWarnings) {
        this.overdueWarnings = overdueWarnings;
    }

    public String getRiskTier() {
        return riskTier;
    }

    public void setRiskTier(String riskTier) {
        this.riskTier = riskTier;
    }

    public String getLatestExceptionType() {
        return latestExceptionType;
    }

    public void setLatestExceptionType(String latestExceptionType) {
        this.latestExceptionType = latestExceptionType;
    }

    public String getLatestExceptionTypeName() {
        return latestExceptionTypeName;
    }

    public void setLatestExceptionTypeName(String latestExceptionTypeName) {
        this.latestExceptionTypeName = latestExceptionTypeName;
    }

    public String getLatestWarningLevel() {
        return latestWarningLevel;
    }

    public void setLatestWarningLevel(String latestWarningLevel) {
        this.latestWarningLevel = latestWarningLevel;
    }

    public LocalDateTime getLatestWarningTime() {
        return latestWarningTime;
    }

    public void setLatestWarningTime(LocalDateTime latestWarningTime) {
        this.latestWarningTime = latestWarningTime;
    }
}
