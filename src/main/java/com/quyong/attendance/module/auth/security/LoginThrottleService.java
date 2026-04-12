package com.quyong.attendance.module.auth.security;

public interface LoginThrottleService {

    void ensureAllowed(String username, String clientIp);

    String recordFailure(String username, String clientIp);

    void clearFailures(String username, String clientIp);
}
