package com.quyong.attendance.module.auth.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quyong.attendance.module.auth.model.AuthUser;
import com.quyong.attendance.module.auth.model.RefreshTokenSession;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

@Component
@Profile("!test")
public class RedisTokenStore implements TokenStore {

    private static final String KEY_PREFIX = "auth:token:";
    private static final String REFRESH_KEY_PREFIX = "auth:refresh:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisTokenStore(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void store(String token, AuthUser authUser, Duration ttl) {
        try {
            stringRedisTemplate.opsForValue().set(
                    buildKey(token),
                    objectMapper.writeValueAsString(TokenSession.from(authUser)),
                    ttl
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("token 序列化失败", exception);
        }
    }

    @Override
    public AuthUser get(String token) {
        String key = buildKey(token);
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        try {
            TokenSession tokenSession = objectMapper.readValue(value, TokenSession.class);
            return tokenSession.toAuthUser();
        } catch (IOException exception) {
            stringRedisTemplate.delete(key);
            return null;
        }
    }

    @Override
    public void delete(String token) {
        if (token == null) {
            return;
        }
        stringRedisTemplate.delete(buildKey(token));
    }

    @Override
    public void storeRefreshToken(String refreshToken, RefreshTokenSession refreshTokenSession, Duration ttl) {
        try {
            stringRedisTemplate.opsForValue().set(
                    buildRefreshKey(refreshToken),
                    objectMapper.writeValueAsString(refreshTokenSession),
                    ttl
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("refresh token 序列化失败", exception);
        }
    }

    @Override
    public RefreshTokenSession getRefreshToken(String refreshToken) {
        String key = buildRefreshKey(refreshToken);
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readValue(value, RefreshTokenSession.class);
        } catch (IOException exception) {
            stringRedisTemplate.delete(key);
            return null;
        }
    }

    @Override
    public void deleteRefreshToken(String refreshToken) {
        if (refreshToken == null) {
            return;
        }
        stringRedisTemplate.delete(buildRefreshKey(refreshToken));
    }

    private String buildKey(String token) {
        return KEY_PREFIX + token;
    }

    private String buildRefreshKey(String refreshToken) {
        return REFRESH_KEY_PREFIX + refreshToken;
    }

    static class TokenSession {

        private Long userId;
        private String username;
        private String realName;
        private String roleCode;
        private Integer status;
        private long expireAtEpochMilli;

        public TokenSession() {
        }

        static TokenSession from(AuthUser authUser) {
            TokenSession tokenSession = new TokenSession();
            tokenSession.userId = authUser.getUserId();
            tokenSession.username = authUser.getUsername();
            tokenSession.realName = authUser.getRealName();
            tokenSession.roleCode = authUser.getRoleCode();
            tokenSession.status = authUser.getStatus();
            tokenSession.expireAtEpochMilli = authUser.getExpireAt().toEpochMilli();
            return tokenSession;
        }

        AuthUser toAuthUser() {
            return new AuthUser(
                    userId,
                    username,
                    realName,
                    roleCode,
                    status,
                    Instant.ofEpochMilli(expireAtEpochMilli)
            );
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

        public long getExpireAtEpochMilli() {
            return expireAtEpochMilli;
        }

        public void setExpireAtEpochMilli(long expireAtEpochMilli) {
            this.expireAtEpochMilli = expireAtEpochMilli;
        }
    }
}
