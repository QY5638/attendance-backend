package com.quyong.attendance.module.face.vo;

import java.math.BigDecimal;

public class FaceLivenessCompleteVO {

    private String sessionId;
    private String livenessToken;
    private Long expiresAt;
    private BigDecimal livenessScore;
    private String message;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getLivenessToken() {
        return livenessToken;
    }

    public void setLivenessToken(String livenessToken) {
        this.livenessToken = livenessToken;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public BigDecimal getLivenessScore() {
        return livenessScore;
    }

    public void setLivenessScore(BigDecimal livenessScore) {
        this.livenessScore = livenessScore;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
