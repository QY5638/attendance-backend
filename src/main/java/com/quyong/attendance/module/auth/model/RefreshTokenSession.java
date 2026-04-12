package com.quyong.attendance.module.auth.model;

import java.time.Instant;

public class RefreshTokenSession {

    private Long userId;
    private String accessToken;
    private Instant expireAt;

    public RefreshTokenSession() {
    }

    public RefreshTokenSession(Long userId, String accessToken, Instant expireAt) {
        this.userId = userId;
        this.accessToken = accessToken;
        this.expireAt = expireAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Instant getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(Instant expireAt) {
        this.expireAt = expireAt;
    }
}
