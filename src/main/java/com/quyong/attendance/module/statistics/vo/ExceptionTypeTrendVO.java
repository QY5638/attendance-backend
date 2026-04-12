package com.quyong.attendance.module.statistics.vo;

import java.util.List;

public class ExceptionTypeTrendVO {

    private String periodType;
    private List<String> labels;
    private List<ExceptionTypeTrendItemVO> items;

    public String getPeriodType() {
        return periodType;
    }

    public void setPeriodType(String periodType) {
        this.periodType = periodType;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public List<ExceptionTypeTrendItemVO> getItems() {
        return items;
    }

    public void setItems(List<ExceptionTypeTrendItemVO> items) {
        this.items = items;
    }
}
