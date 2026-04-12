package com.quyong.attendance.module.warning.vo;

public class WarningTrendPointVO {

    private String dateLabel;
    private Long totalCount;
    private Long processedCount;
    private Long unprocessedCount;
    private Long highRiskCount;

    public String getDateLabel() {
        return dateLabel;
    }

    public void setDateLabel(String dateLabel) {
        this.dateLabel = dateLabel;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public Long getProcessedCount() {
        return processedCount;
    }

    public void setProcessedCount(Long processedCount) {
        this.processedCount = processedCount;
    }

    public Long getUnprocessedCount() {
        return unprocessedCount;
    }

    public void setUnprocessedCount(Long unprocessedCount) {
        this.unprocessedCount = unprocessedCount;
    }

    public Long getHighRiskCount() {
        return highRiskCount;
    }

    public void setHighRiskCount(Long highRiskCount) {
        this.highRiskCount = highRiskCount;
    }
}
