package com.quyong.attendance.module.warning.vo;

import java.util.List;

public class WarningExceptionTrendItemVO {

    private String type;
    private String name;
    private Long totalCount;
    private Long highRiskCount;
    private List<Long> dailyCounts;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public Long getHighRiskCount() {
        return highRiskCount;
    }

    public void setHighRiskCount(Long highRiskCount) {
        this.highRiskCount = highRiskCount;
    }

    public List<Long> getDailyCounts() {
        return dailyCounts;
    }

    public void setDailyCounts(List<Long> dailyCounts) {
        this.dailyCounts = dailyCounts;
    }
}
