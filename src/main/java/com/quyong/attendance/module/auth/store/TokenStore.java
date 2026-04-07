package com.quyong.attendance.module.auth.store;

import com.quyong.attendance.module.auth.model.AuthUser;

import java.time.Duration;

public interface TokenStore {

    void store(String token, AuthUser authUser, Duration ttl);

    AuthUser get(String token);
}
