package com.quyong.attendance.module.warning.dto;

public class WarningReevaluateDTO {

    private Long warningId;
    private String reason;

    public Long getWarningId() {
        return warningId;
    }

    public void setWarningId(Long warningId) {
        this.warningId = warningId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
