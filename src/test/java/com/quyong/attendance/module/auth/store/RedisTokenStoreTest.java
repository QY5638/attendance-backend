package com.quyong.attendance.module.auth.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quyong.attendance.module.auth.model.AuthUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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

    @Test
    void shouldStoreAndRestoreTokenSessionWhenAuthUserContainsInstant() {
        String token = "instant-token";
        Duration ttl = Duration.ofMinutes(30);
        Instant expireAt = Instant.parse("2026-04-07T08:09:10.123Z");
        AuthUser authUser = new AuthUser(1L, "alice", "Alice", "ADMIN", 1, expireAt);

        assertDoesNotThrow(() -> redisTokenStore.store(token, authUser, ttl));

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq("auth:token:" + token), valueCaptor.capture(), eq(ttl));

        String storedValue = valueCaptor.getValue();
        assertTrue(storedValue.contains("\"expireAtEpochMilli\":" + expireAt.toEpochMilli()));

        when(valueOperations.get("auth:token:" + token)).thenReturn(storedValue);

        AuthUser restored = assertDoesNotThrow(() -> redisTokenStore.get(token));
        assertAll(
                () -> assertNotNull(restored),
                () -> assertEquals(authUser.getUserId(), restored.getUserId()),
                () -> assertEquals(authUser.getUsername(), restored.getUsername()),
                () -> assertEquals(authUser.getRealName(), restored.getRealName()),
                () -> assertEquals(authUser.getRoleCode(), restored.getRoleCode()),
                () -> assertEquals(authUser.getStatus(), restored.getStatus()),
                () -> assertEquals(authUser.getExpireAt(), restored.getExpireAt())
        );
    }
}
