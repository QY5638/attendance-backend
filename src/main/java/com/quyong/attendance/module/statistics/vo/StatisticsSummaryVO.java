package com.quyong.attendance.module.statistics.vo;

public class StatisticsSummaryVO {

    private String periodType;
    private String summary;
    private String highlightRisks;
    private String manageSuggestion;

    public String getPeriodType() {
        return periodType;
    }

    public void setPeriodType(String periodType) {
        this.periodType = periodType;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getHighlightRisks() {
        return highlightRisks;
    }

    public void setHighlightRisks(String highlightRisks) {
        this.highlightRisks = highlightRisks;
    }

    public String getManageSuggestion() {
        return manageSuggestion;
    }

    public void setManageSuggestion(String manageSuggestion) {
        this.manageSuggestion = manageSuggestion;
    }
}
