package com.quyong.attendance.module.exceptiondetect.dto;

public class RuleSaveDTO {

    private Long id;
    private String name;
    private String startTime;
    private String endTime;
    private Integer lateThreshold;
    private Integer earlyThreshold;
    private Integer repeatLimit;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Integer getLateThreshold() {
        return lateThreshold;
    }

    public void setLateThreshold(Integer lateThreshold) {
        this.lateThreshold = lateThreshold;
    }

    public Integer getEarlyThreshold() {
        return earlyThreshold;
    }

    public void setEarlyThreshold(Integer earlyThreshold) {
        this.earlyThreshold = earlyThreshold;
    }

    public Integer getRepeatLimit() {
        return repeatLimit;
    }

    public void setRepeatLimit(Integer repeatLimit) {
        this.repeatLimit = repeatLimit;
    }
}
