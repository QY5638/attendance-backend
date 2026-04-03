package com.quyong.attendance.module.exceptiondetect.dto;

import java.math.BigDecimal;

public class RiskFeaturesDTO {

    private BigDecimal faceScore;
    private Boolean deviceChanged;
    private Boolean locationChanged;
    private Integer historyAbnormalCount;

    public BigDecimal getFaceScore() {
        return faceScore;
    }

    public void setFaceScore(BigDecimal faceScore) {
        this.faceScore = faceScore;
    }

    public Boolean getDeviceChanged() {
        return deviceChanged;
    }

    public void setDeviceChanged(Boolean deviceChanged) {
        this.deviceChanged = deviceChanged;
    }

    public Boolean getLocationChanged() {
        return locationChanged;
    }

    public void setLocationChanged(Boolean locationChanged) {
        this.locationChanged = locationChanged;
    }

    public Integer getHistoryAbnormalCount() {
        return historyAbnormalCount;
    }

    public void setHistoryAbnormalCount(Integer historyAbnormalCount) {
        this.historyAbnormalCount = historyAbnormalCount;
    }
}
