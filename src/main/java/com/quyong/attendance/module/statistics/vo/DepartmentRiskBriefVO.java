package com.quyong.attendance.module.statistics.vo;

import java.math.BigDecimal;

public class DepartmentRiskBriefVO {

    private Long deptId;
    private String deptName;
    private BigDecimal riskScore;
    private String riskSummary;
    private String manageSuggestion;

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public BigDecimal getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(BigDecimal riskScore) {
        this.riskScore = riskScore;
    }

    public String getRiskSummary() {
        return riskSummary;
    }

    public void setRiskSummary(String riskSummary) {
        this.riskSummary = riskSummary;
    }

    public String getManageSuggestion() {
        return manageSuggestion;
    }

    public void setManageSuggestion(String manageSuggestion) {
        this.manageSuggestion = manageSuggestion;
    }
}
