package com.quyong.attendance.module.attendance.dto;

public class AttendanceRepairDTO {

    private Long userId;
    private Long recordId;
    private String checkType;
    private String checkTime;
    private String repairReason;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public String getCheckType() {
        return checkType;
    }

    public void setCheckType(String checkType) {
        this.checkType = checkType;
    }

    public String getCheckTime() {
        return checkTime;
    }

    public void setCheckTime(String checkTime) {
        this.checkTime = checkTime;
    }

    public String getRepairReason() {
        return repairReason;
    }

    public void setRepairReason(String repairReason) {
        this.repairReason = repairReason;
    }
}
