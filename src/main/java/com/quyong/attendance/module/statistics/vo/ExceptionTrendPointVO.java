package com.quyong.attendance.module.statistics.vo;

public class ExceptionTrendPointVO {

    private String date;
    private Long recordCount;
    private Long exceptionCount;
    private Long analysisCount;
    private Long warningCount;
    private Long reviewCount;
    private Long closedLoopCount;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
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
}
