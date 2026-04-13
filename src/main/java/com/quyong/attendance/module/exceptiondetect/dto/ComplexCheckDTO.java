package com.quyong.attendance.module.exceptiondetect.dto;

public class ComplexCheckDTO {

    private Long recordId;
    private Long exceptionId;
    private Long userId;
    private RiskFeaturesDTO riskFeatures;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public Long getExceptionId() {
        return exceptionId;
    }

    public void setExceptionId(Long exceptionId) {
        this.exceptionId = exceptionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public RiskFeaturesDTO getRiskFeatures() {
        return riskFeatures;
    }

    public void setRiskFeatures(RiskFeaturesDTO riskFeatures) {
        this.riskFeatures = riskFeatures;
    }
}
