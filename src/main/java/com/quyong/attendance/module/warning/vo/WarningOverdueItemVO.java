package com.quyong.attendance.module.warning.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.time.LocalDateTime;

public class WarningOverdueItemVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long warningId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long exceptionId;
    private String title;
    private String level;
    private String realName;
    private LocalDateTime sendTime;
    private Long overdueMinutes;

    public Long getWarningId() {
        return warningId;
    }

    public void setWarningId(Long warningId) {
        this.warningId = warningId;
    }

    public Long getExceptionId() {
        return exceptionId;
    }

    public void setExceptionId(Long exceptionId) {
        this.exceptionId = exceptionId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public LocalDateTime getSendTime() {
        return sendTime;
    }

    public void setSendTime(LocalDateTime sendTime) {
        this.sendTime = sendTime;
    }

    public Long getOverdueMinutes() {
        return overdueMinutes;
    }

    public void setOverdueMinutes(Long overdueMinutes) {
        this.overdueMinutes = overdueMinutes;
    }
}
