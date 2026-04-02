package com.quyong.attendance.module.auth.store;

import com.quyong.attendance.module.auth.model.AuthUser;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("test")
public class InMemoryTokenStore implements TokenStore {

    private final Map<String, TokenValue> tokenValues = new ConcurrentHashMap<String, TokenValue>();

    @Override
    public void store(String token, AuthUser authUser, Duration ttl) {
        if (authUser.getExpireAt() == null) {
            authUser.setExpireAt(Instant.now().plus(ttl));
        }
        tokenValues.put(token, new TokenValue(authUser));
    }

    @Override
    public AuthUser get(String token) {
        TokenValue tokenValue = tokenValues.get(token);
        if (tokenValue == null) {
            return null;
        }
        if (tokenValue.authUser.getExpireAt() == null || tokenValue.authUser.getExpireAt().isBefore(Instant.now())) {
            tokenValues.remove(token);
            return null;
        }
        return tokenValue.authUser;
    }

    private static class TokenValue {

        private final AuthUser authUser;

        private TokenValue(AuthUser authUser) {
            this.authUser = authUser;
        }
    }
}
