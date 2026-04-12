package com.quyong.attendance.module.auth.security;

import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.auth.config.AuthSecurityProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Component
@Profile("!test")
public class RedisLoginThrottleService implements LoginThrottleService {

    private static final String USER_FAIL_PREFIX = "auth:login:fail:user:";
    private static final String IP_FAIL_PREFIX = "auth:login:fail:ip:";
    private static final String USER_LOCK_PREFIX = "auth:login:lock:user:";
    private static final String IP_LOCK_PREFIX = "auth:login:lock:ip:";

    private final StringRedisTemplate stringRedisTemplate;
    private final AuthSecurityProperties properties;

    public RedisLoginThrottleService(StringRedisTemplate stringRedisTemplate, AuthSecurityProperties properties) {
        this.stringRedisTemplate = stringRedisTemplate;
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
        long userCount = increment(userFailKey(username), failWindow());
        long ipCount = increment(ipFailKey(clientIp), failWindow());

        boolean locked = false;
        if (userCount >= usernameThreshold()) {
            stringRedisTemplate.opsForValue().set(userLockKey(username), "1", lockDuration());
            stringRedisTemplate.delete(userFailKey(username));
            locked = true;
        }
        if (ipCount >= ipThreshold()) {
            stringRedisTemplate.opsForValue().set(ipLockKey(clientIp), "1", lockDuration());
            stringRedisTemplate.delete(ipFailKey(clientIp));
            locked = true;
        }

        if (!locked) {
            return null;
        }
        return buildLockedMessage(lockDuration());
    }

    @Override
    public void clearFailures(String username, String clientIp) {
        List<String> keys = Arrays.asList(userFailKey(username), ipFailKey(clientIp));
        stringRedisTemplate.delete(keys);
    }

    private Duration resolveLockRemaining(String username, String clientIp) {
        Duration userRemaining = ttl(userLockKey(username));
        Duration ipRemaining = ttl(ipLockKey(clientIp));
        if (userRemaining == null) {
            return ipRemaining;
        }
        if (ipRemaining == null || userRemaining.compareTo(ipRemaining) >= 0) {
            return userRemaining;
        }
        return ipRemaining;
    }

    private long increment(String key, Duration ttl) {
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count.longValue() == 1L) {
            stringRedisTemplate.expire(key, ttl);
        }
        return count == null ? 0L : count.longValue();
    }

    private Duration ttl(String key) {
        Long seconds = stringRedisTemplate.getExpire(key);
        if (seconds == null || seconds.longValue() <= 0L) {
            return null;
        }
        return Duration.ofSeconds(seconds.longValue());
    }

    private String userFailKey(String username) {
        return USER_FAIL_PREFIX + normalize(username);
    }

    private String ipFailKey(String clientIp) {
        return IP_FAIL_PREFIX + normalize(clientIp);
    }

    private String userLockKey(String username) {
        return USER_LOCK_PREFIX + normalize(username);
    }

    private String ipLockKey(String clientIp) {
        return IP_LOCK_PREFIX + normalize(clientIp);
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
}
