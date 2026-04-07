package com.quyong.attendance.module.auth.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisTokenStoreTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisTokenStore redisTokenStore;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        redisTokenStore = new RedisTokenStore(stringRedisTemplate, new ObjectMapper());
    }

    @Test
    void shouldReturnNullWhenStoredTokenSessionIsCorrupted() {
        when(valueOperations.get("auth:token:broken-token")).thenReturn("{invalid-json");

        assertNull(redisTokenStore.get("broken-token"));
    }
}
