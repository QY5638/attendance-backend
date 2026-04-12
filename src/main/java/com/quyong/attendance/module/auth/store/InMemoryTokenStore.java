package com.quyong.attendance.module.auth.store;

import com.quyong.attendance.module.auth.model.AuthUser;
import com.quyong.attendance.module.auth.model.RefreshTokenSession;
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
    private final Map<String, RefreshTokenValue> refreshTokenValues = new ConcurrentHashMap<String, RefreshTokenValue>();

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

    @Override
    public void delete(String token) {
        if (token == null) {
            return;
        }
        tokenValues.remove(token);
    }

    @Override
    public void storeRefreshToken(String refreshToken, RefreshTokenSession refreshTokenSession, Duration ttl) {
        if (refreshTokenSession.getExpireAt() == null) {
            refreshTokenSession.setExpireAt(Instant.now().plus(ttl));
        }
        refreshTokenValues.put(refreshToken, new RefreshTokenValue(refreshTokenSession));
    }

    @Override
    public RefreshTokenSession getRefreshToken(String refreshToken) {
        RefreshTokenValue refreshTokenValue = refreshTokenValues.get(refreshToken);
        if (refreshTokenValue == null) {
            return null;
        }
        if (refreshTokenValue.refreshTokenSession.getExpireAt() == null
                || refreshTokenValue.refreshTokenSession.getExpireAt().isBefore(Instant.now())) {
            refreshTokenValues.remove(refreshToken);
            return null;
        }
        return refreshTokenValue.refreshTokenSession;
    }

    @Override
    public void deleteRefreshToken(String refreshToken) {
        if (refreshToken == null) {
            return;
        }
        refreshTokenValues.remove(refreshToken);
    }

    private static class TokenValue {

        private final AuthUser authUser;

        private TokenValue(AuthUser authUser) {
            this.authUser = authUser;
        }
    }

    private static class RefreshTokenValue {

        private final RefreshTokenSession refreshTokenSession;

        private RefreshTokenValue(RefreshTokenSession refreshTokenSession) {
            this.refreshTokenSession = refreshTokenSession;
        }
    }
}
