package com.quyong.attendance.module.face.support;

import java.math.BigDecimal;
import java.time.Instant;

public class FaceLivenessProof {

    private String token;
    private Long userId;
    private String imageHash;
    private BigDecimal livenessScore;
    private Instant expireAt;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getImageHash() {
        return imageHash;
    }

    public void setImageHash(String imageHash) {
        this.imageHash = imageHash;
    }

    public BigDecimal getLivenessScore() {
        return livenessScore;
    }

    public void setLivenessScore(BigDecimal livenessScore) {
        this.livenessScore = livenessScore;
    }

    public Instant getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(Instant expireAt) {
        this.expireAt = expireAt;
    }
}
