package com.quyong.attendance.module.statistics.vo;

import java.util.List;

public class ExceptionTrendVO {

    private String periodType;
    private List<ExceptionTrendPointVO> points;

    public String getPeriodType() {
        return periodType;
    }

    public void setPeriodType(String periodType) {
        this.periodType = periodType;
    }

    public List<ExceptionTrendPointVO> getPoints() {
        return points;
    }

    public void setPoints(List<ExceptionTrendPointVO> points) {
        this.points = points;
    }
}
