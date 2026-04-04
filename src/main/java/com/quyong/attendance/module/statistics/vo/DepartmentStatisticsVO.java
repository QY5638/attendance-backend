package com.quyong.attendance.module.statistics.vo;

import java.util.Map;

public class DepartmentStatisticsVO {

    private Long deptId;
    private String deptName;
    private Long recordCount;
    private Long exceptionCount;
    private Long analysisCount;
    private Long warningCount;
    private Long reviewCount;
    private Long closedLoopCount;
    private Map<String, Long> exceptionTypeDistribution;
    private Map<String, Long> riskLevelDistribution;
    private Map<String, Long> warningStatusDistribution;
    private Map<String, Long> reviewResultDistribution;

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

    public Long getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(Long recordCount) {
        this.recordCount = recordCount;
    }

    public Long getExceptionCount() {
        return exceptionCount;
    }

    public void setExceptionCount(Long exceptionCount) {
        this.exceptionCount = exceptionCount;
    }

    public Long getAnalysisCount() {
        return analysisCount;
    }

    public void setAnalysisCount(Long analysisCount) {
        this.analysisCount = analysisCount;
    }

    public Long getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(Long warningCount) {
        this.warningCount = warningCount;
    }

    public Long getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Long reviewCount) {
        this.reviewCount = reviewCount;
    }

    public Long getClosedLoopCount() {
        return closedLoopCount;
    }

    public void setClosedLoopCount(Long closedLoopCount) {
        this.closedLoopCount = closedLoopCount;
    }

    public Map<String, Long> getExceptionTypeDistribution() {
        return exceptionTypeDistribution;
    }

    public void setExceptionTypeDistribution(Map<String, Long> exceptionTypeDistribution) {
        this.exceptionTypeDistribution = exceptionTypeDistribution;
    }

    public Map<String, Long> getRiskLevelDistribution() {
        return riskLevelDistribution;
    }

    public void setRiskLevelDistribution(Map<String, Long> riskLevelDistribution) {
        this.riskLevelDistribution = riskLevelDistribution;
    }

    public Map<String, Long> getWarningStatusDistribution() {
        return warningStatusDistribution;
    }

    public void setWarningStatusDistribution(Map<String, Long> warningStatusDistribution) {
        this.warningStatusDistribution = warningStatusDistribution;
    }

    public Map<String, Long> getReviewResultDistribution() {
        return reviewResultDistribution;
    }

    public void setReviewResultDistribution(Map<String, Long> reviewResultDistribution) {
        this.reviewResultDistribution = reviewResultDistribution;
    }
}
