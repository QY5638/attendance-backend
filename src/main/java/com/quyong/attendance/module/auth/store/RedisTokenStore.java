package com.quyong.attendance.module.auth.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quyong.attendance.module.auth.model.AuthUser;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
@Profile("!test")
public class RedisTokenStore implements TokenStore {

    private static final String KEY_PREFIX = "auth:token:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisTokenStore(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void store(String token, AuthUser authUser, Duration ttl) {
        try {
            stringRedisTemplate.opsForValue().set(buildKey(token), objectMapper.writeValueAsString(authUser), ttl);
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
            return objectMapper.readValue(value, AuthUser.class);
        } catch (IOException exception) {
            stringRedisTemplate.delete(key);
            return null;
        }
    }

    private String buildKey(String token) {
        return KEY_PREFIX + token;
    }
}
