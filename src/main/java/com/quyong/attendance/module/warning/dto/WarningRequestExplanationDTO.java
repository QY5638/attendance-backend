package com.quyong.attendance.module.warning.dto;

public class WarningRequestExplanationDTO {

    private String content;
    private Integer deadlineHours;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getDeadlineHours() {
        return deadlineHours;
    }

    public void setDeadlineHours(Integer deadlineHours) {
        this.deadlineHours = deadlineHours;
    }
}
