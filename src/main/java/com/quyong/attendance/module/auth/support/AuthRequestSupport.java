package com.quyong.attendance.module.auth.support;

import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;

public final class AuthRequestSupport {

    private static final String BEARER_PREFIX = "Bearer ";

    private AuthRequestSupport() {
    }

    public static String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            String[] values = forwarded.split(",");
            if (values.length > 0 && StringUtils.hasText(values[0])) {
                return values[0].trim();
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    public static String extractBearerToken(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        return StringUtils.hasText(token) ? token : null;
    }

    public static String extractBearerToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return extractBearerToken(request.getHeader(HttpHeaders.AUTHORIZATION));
    }
}
