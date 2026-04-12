package com.quyong.attendance.module.auth.security;

import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.auth.config.AuthSecurityProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("test")
public class InMemoryLoginThrottleService implements LoginThrottleService {

    private final AuthSecurityProperties properties;
    private final Map<String, AttemptWindow> attemptWindows = new ConcurrentHashMap<String, AttemptWindow>();
    private final Map<String, Instant> lockUntilMap = new ConcurrentHashMap<String, Instant>();

    public InMemoryLoginThrottleService(AuthSecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public void ensureAllowed(String username, String clientIp) {
        Duration remaining = resolveLockRemaining(username, clientIp);
        if (remaining != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), buildLockedMessage(remaining));
        }
    }

    @Override
    public String recordFailure(String username, String clientIp) {
        Instant now = Instant.now();
        Duration window = failWindow();
        int userCount = increment(userFailKey(username), now, window);
        int ipCount = increment(ipFailKey(clientIp), now, window);

        boolean locked = false;
        if (userCount >= usernameThreshold()) {
            lockUntilMap.put(userLockKey(username), now.plus(lockDuration()));
            attemptWindows.remove(userFailKey(username));
            locked = true;
        }
        if (ipCount >= ipThreshold()) {
            lockUntilMap.put(ipLockKey(clientIp), now.plus(lockDuration()));
            attemptWindows.remove(ipFailKey(clientIp));
            locked = true;
        }

        if (!locked) {
            return null;
        }
        return buildLockedMessage(lockDuration());
    }

    @Override
    public void clearFailures(String username, String clientIp) {
        attemptWindows.remove(userFailKey(username));
        attemptWindows.remove(ipFailKey(clientIp));
    }

    private Duration resolveLockRemaining(String username, String clientIp) {
        Instant now = Instant.now();
        Duration userRemaining = resolveRemaining(userLockKey(username), now);
        Duration ipRemaining = resolveRemaining(ipLockKey(clientIp), now);
        if (userRemaining == null) {
            return ipRemaining;
        }
        if (ipRemaining == null || userRemaining.compareTo(ipRemaining) >= 0) {
            return userRemaining;
        }
        return ipRemaining;
    }

    private Duration resolveRemaining(String key, Instant now) {
        Instant lockUntil = lockUntilMap.get(key);
        if (lockUntil == null) {
            return null;
        }
        if (!lockUntil.isAfter(now)) {
            lockUntilMap.remove(key);
            return null;
        }
        return Duration.between(now, lockUntil);
    }

    private int increment(String key, Instant now, Duration window) {
        AttemptWindow attemptWindow = attemptWindows.get(key);
        if (attemptWindow == null || !attemptWindow.expireAt.isAfter(now)) {
            attemptWindow = new AttemptWindow(now.plus(window));
            attemptWindows.put(key, attemptWindow);
        }
        attemptWindow.count = attemptWindow.count + 1;
        return attemptWindow.count;
    }

    private String userFailKey(String username) {
        return "user-fail:" + normalize(username);
    }

    private String ipFailKey(String clientIp) {
        return "ip-fail:" + normalize(clientIp);
    }

    private String userLockKey(String username) {
        return "user-lock:" + normalize(username);
    }

    private String ipLockKey(String clientIp) {
        return "ip-lock:" + normalize(clientIp);
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "unknown";
        }
        return value.trim().toLowerCase();
    }

    private Duration failWindow() {
        Long minutes = properties.getLoginFailWindowMinutes();
        return Duration.ofMinutes(minutes == null || minutes.longValue() < 1L ? 15L : minutes.longValue());
    }

    private Duration lockDuration() {
        Long minutes = properties.getLoginLockMinutes();
        return Duration.ofMinutes(minutes == null || minutes.longValue() < 1L ? 15L : minutes.longValue());
    }

    private int usernameThreshold() {
        Integer threshold = properties.getUsernameMaxFailures();
        return threshold == null || threshold.intValue() < 1 ? 5 : threshold.intValue();
    }

    private int ipThreshold() {
        Integer threshold = properties.getIpMaxFailures();
        return threshold == null || threshold.intValue() < 1 ? 12 : threshold.intValue();
    }

    private String buildLockedMessage(Duration duration) {
        long minutes = Math.max(1L, (duration.getSeconds() + 59L) / 60L);
        return "登录尝试过于频繁，请" + minutes + "分钟后重试";
    }

    private static class AttemptWindow {

        private int count;
        private final Instant expireAt;

        private AttemptWindow(Instant expireAt) {
            this.expireAt = expireAt;
        }
    }
}
