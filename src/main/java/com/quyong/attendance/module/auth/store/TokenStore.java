package com.quyong.attendance.module.auth.store;

import com.quyong.attendance.module.auth.model.AuthUser;
import com.quyong.attendance.module.auth.model.RefreshTokenSession;

import java.time.Duration;

public interface TokenStore {

    void store(String token, AuthUser authUser, Duration ttl);

    AuthUser get(String token);

    void delete(String token);

    void storeRefreshToken(String refreshToken, RefreshTokenSession refreshTokenSession, Duration ttl);

    RefreshTokenSession getRefreshToken(String refreshToken);

    void deleteRefreshToken(String refreshToken);
}
