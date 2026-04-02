package com.quyong.attendance.module.auth.model;

import java.time.Instant;

public class AuthUser {

    private Long userId;
    private String username;
    private String realName;
    private String roleCode;
    private Integer status;
    private Instant expireAt;

    public AuthUser() {
    }

    public AuthUser(Long userId, String username, String realName, String roleCode, Integer status, Instant expireAt) {
        this.userId = userId;
        this.username = username;
        this.realName = realName;
        this.roleCode = roleCode;
        this.status = status;
        this.expireAt = expireAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Instant getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(Instant expireAt) {
        this.expireAt = expireAt;
    }
}
