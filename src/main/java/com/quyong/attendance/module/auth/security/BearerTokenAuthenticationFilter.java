package com.quyong.attendance.module.auth.security;

import com.quyong.attendance.module.auth.model.AuthUser;
import com.quyong.attendance.module.auth.store.TokenStore;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;

@Component
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenStore tokenStore;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    public BearerTokenAuthenticationFilter(TokenStore tokenStore, RestAuthenticationEntryPoint authenticationEntryPoint) {
        this.tokenStore = tokenStore;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        AuthUser authUser = tokenStore.get(token);
        if (isInvalidAuthUser(authUser)) {
            authenticationEntryPoint.commence(request, response, new BadCredentialsException("token 无效或已过期"));
            return;
        }

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                authUser,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + authUser.getRoleCode()))
        );
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "/api/health".equals(path) || "/api/auth/login".equals(path);
    }

    private boolean isInvalidAuthUser(AuthUser authUser) {
        return authUser == null
                || authUser.getUserId() == null
                || !StringUtils.hasText(authUser.getUsername())
                || !StringUtils.hasText(authUser.getRealName())
                || !StringUtils.hasText(authUser.getRoleCode())
                || authUser.getStatus() == null
                || authUser.getStatus() != 1
                || authUser.getExpireAt() == null
                || !authUser.getExpireAt().isAfter(Instant.now());
    }
}
