package com.quyong.attendance.module.face.vo;

import java.math.BigDecimal;

public class FaceVerifyVO {

    private Long userId;
    private Boolean registered;
    private Boolean matched;
    private BigDecimal faceScore;
    private BigDecimal threshold;
    private String message;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Boolean getRegistered() {
        return registered;
    }

    public void setRegistered(Boolean registered) {
        this.registered = registered;
    }

    public Boolean getMatched() {
        return matched;
    }

    public void setMatched(Boolean matched) {
        this.matched = matched;
    }

    public BigDecimal getFaceScore() {
        return faceScore;
    }

    public void setFaceScore(BigDecimal faceScore) {
        this.faceScore = faceScore;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }

    public void setThreshold(BigDecimal threshold) {
        this.threshold = threshold;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
