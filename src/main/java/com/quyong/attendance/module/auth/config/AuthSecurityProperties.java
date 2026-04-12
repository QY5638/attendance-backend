package com.quyong.attendance.module.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthSecurityProperties {

    private Long accessTokenTtlHours = Long.valueOf(12L);
    private Long refreshTokenTtlDays = Long.valueOf(7L);
    private Long loginFailWindowMinutes = Long.valueOf(15L);
    private Long loginLockMinutes = Long.valueOf(15L);
    private Integer usernameMaxFailures = Integer.valueOf(5);
    private Integer ipMaxFailures = Integer.valueOf(12);

    public Long getAccessTokenTtlHours() {
        return accessTokenTtlHours;
    }

    public void setAccessTokenTtlHours(Long accessTokenTtlHours) {
        this.accessTokenTtlHours = accessTokenTtlHours;
    }

    public Long getRefreshTokenTtlDays() {
        return refreshTokenTtlDays;
    }

    public void setRefreshTokenTtlDays(Long refreshTokenTtlDays) {
        this.refreshTokenTtlDays = refreshTokenTtlDays;
    }

    public Long getLoginFailWindowMinutes() {
        return loginFailWindowMinutes;
    }

    public void setLoginFailWindowMinutes(Long loginFailWindowMinutes) {
        this.loginFailWindowMinutes = loginFailWindowMinutes;
    }

    public Long getLoginLockMinutes() {
        return loginLockMinutes;
    }

    public void setLoginLockMinutes(Long loginLockMinutes) {
        this.loginLockMinutes = loginLockMinutes;
    }

    public Integer getUsernameMaxFailures() {
        return usernameMaxFailures;
    }

    public void setUsernameMaxFailures(Integer usernameMaxFailures) {
        this.usernameMaxFailures = usernameMaxFailures;
    }

    public Integer getIpMaxFailures() {
        return ipMaxFailures;
    }

    public void setIpMaxFailures(Integer ipMaxFailures) {
        this.ipMaxFailures = ipMaxFailures;
    }
}
