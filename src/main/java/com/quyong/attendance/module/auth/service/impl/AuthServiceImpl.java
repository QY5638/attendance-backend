package com.quyong.attendance.module.auth.service.impl;

import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.auth.config.AuthSecurityProperties;
import com.quyong.attendance.module.auth.dto.LogoutDTO;
import com.quyong.attendance.module.auth.dto.LoginDTO;
import com.quyong.attendance.module.auth.dto.RefreshTokenDTO;
import com.quyong.attendance.module.auth.model.AuthUser;
import com.quyong.attendance.module.auth.model.RefreshTokenSession;
import com.quyong.attendance.module.auth.security.LoginThrottleService;
import com.quyong.attendance.module.auth.service.AuthService;
import com.quyong.attendance.module.auth.support.AuthRequestSupport;
import com.quyong.attendance.module.auth.store.TokenStore;
import com.quyong.attendance.module.auth.vo.LoginVO;
import com.quyong.attendance.module.statistics.service.OperationLogService;
import com.quyong.attendance.module.role.entity.Role;
import com.quyong.attendance.module.role.mapper.RoleMapper;
import com.quyong.attendance.module.user.entity.User;
import com.quyong.attendance.module.user.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenStore tokenStore;
    private final LoginThrottleService loginThrottleService;
    private final OperationLogService operationLogService;
    private final AuthSecurityProperties authSecurityProperties;

    public AuthServiceImpl(UserMapper userMapper,
                           RoleMapper roleMapper,
                           PasswordEncoder passwordEncoder,
                           TokenStore tokenStore,
                           LoginThrottleService loginThrottleService,
                           OperationLogService operationLogService,
                           AuthSecurityProperties authSecurityProperties) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenStore = tokenStore;
        this.loginThrottleService = loginThrottleService;
        this.operationLogService = operationLogService;
        this.authSecurityProperties = authSecurityProperties;
    }

    @Override
    public LoginVO login(LoginDTO loginDTO, String clientIp) {
        String username = safeText(loginDTO.getUsername());
        loginThrottleService.ensureAllowed(username, clientIp);

        User user = userMapper.selectByUsername(username);
        if (user == null || !passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            String lockMessage = loginThrottleService.recordFailure(username, clientIp);
            operationLogService.save(user == null ? null : user.getId(), "LOGIN_FAILURE", buildAuthLogContent(username, clientIp, "登录失败：用户名或密码错误"));
            if (lockMessage != null) {
                operationLogService.save(user == null ? null : user.getId(), "LOGIN_LOCKED", buildAuthLogContent(username, clientIp, lockMessage));
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), lockMessage);
            }
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户名或密码错误");
        }

        loginThrottleService.clearFailures(username, clientIp);
        if (user.getStatus() == null || user.getStatus() != 1) {
            operationLogService.save(user.getId(), "LOGIN_FAILURE", buildAuthLogContent(user.getUsername(), clientIp, "登录失败：账号已禁用"));
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "账号已禁用");
        }

        Role role = roleMapper.selectRoleById(user.getRoleId());
        if (role == null) {
            operationLogService.save(user.getId(), "LOGIN_FAILURE", buildAuthLogContent(user.getUsername(), clientIp, "登录失败：账号角色不存在"));
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "账号角色不存在");
        }
        if (role.getStatus() == null || role.getStatus() != 1) {
            operationLogService.save(user.getId(), "LOGIN_FAILURE", buildAuthLogContent(user.getUsername(), clientIp, "登录失败：账号角色已禁用"));
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "账号角色已禁用");
        }

        TokenBundle tokenBundle = issueTokens(user, role);
        operationLogService.save(user.getId(), "LOGIN", buildAuthLogContent(user.getUsername(), clientIp, user.getRealName() + "登录系统"));
        return buildLoginVO(tokenBundle.accessToken, tokenBundle.refreshToken, role.getCode(), user.getRealName());
    }

    @Override
    public LoginVO refresh(RefreshTokenDTO refreshTokenDTO, String clientIp) {
        String refreshToken = safeText(refreshTokenDTO.getRefreshToken());
        RefreshTokenSession refreshTokenSession = tokenStore.getRefreshToken(refreshToken);
        if (isInvalidRefreshSession(refreshTokenSession)) {
            operationLogService.save(null, "TOKEN_REFRESH_FAILURE", buildAuthLogContent("unknown", clientIp, "刷新令牌无效或已过期"));
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "登录状态已失效，请重新登录");
        }

        User user = userMapper.selectById(refreshTokenSession.getUserId());
        if (user == null) {
            tokenStore.deleteRefreshToken(refreshToken);
            tokenStore.delete(refreshTokenSession.getAccessToken());
            operationLogService.save(null, "TOKEN_REFRESH_FAILURE", buildAuthLogContent("unknown", clientIp, "刷新失败：账号不存在"));
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "登录状态已失效，请重新登录");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            tokenStore.deleteRefreshToken(refreshToken);
            tokenStore.delete(refreshTokenSession.getAccessToken());
            operationLogService.save(user.getId(), "TOKEN_REFRESH_FAILURE", buildAuthLogContent(user.getUsername(), clientIp, "刷新失败：账号已禁用"));
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "账号已禁用，请联系管理员");
        }

        Role role = roleMapper.selectRoleById(user.getRoleId());
        if (role == null || role.getStatus() == null || role.getStatus() != 1) {
            tokenStore.deleteRefreshToken(refreshToken);
            tokenStore.delete(refreshTokenSession.getAccessToken());
            operationLogService.save(user.getId(), "TOKEN_REFRESH_FAILURE", buildAuthLogContent(user.getUsername(), clientIp, "刷新失败：账号角色不可用"));
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "账号角色已不可用，请重新登录");
        }

        tokenStore.delete(refreshTokenSession.getAccessToken());
        tokenStore.deleteRefreshToken(refreshToken);

        TokenBundle tokenBundle = issueTokens(user, role);
        operationLogService.save(user.getId(), "TOKEN_REFRESH", buildAuthLogContent(user.getUsername(), clientIp, user.getRealName() + "刷新登录令牌"));
        return buildLoginVO(tokenBundle.accessToken, tokenBundle.refreshToken, role.getCode(), user.getRealName());
    }

    @Override
    public void logout(String authorization, LogoutDTO logoutDTO, AuthUser authUser, String clientIp) {
        String accessToken = AuthRequestSupport.extractBearerToken(authorization);
        if (accessToken != null) {
            tokenStore.delete(accessToken);
        }

        String refreshToken = logoutDTO == null ? null : safeText(logoutDTO.getRefreshToken());
        if (refreshToken != null) {
            RefreshTokenSession refreshTokenSession = tokenStore.getRefreshToken(refreshToken);
            if (refreshTokenSession != null && safeText(refreshTokenSession.getAccessToken()) != null) {
                tokenStore.delete(refreshTokenSession.getAccessToken());
            }
            tokenStore.deleteRefreshToken(refreshToken);
        }

        if (authUser != null) {
            operationLogService.save(authUser.getUserId(), "LOGOUT", buildAuthLogContent(authUser.getUsername(), clientIp, authUser.getRealName() + "退出系统"));
        }
    }

    private TokenBundle issueTokens(User user, Role role) {
        Duration accessTokenTtl = accessTokenTtl();
        Duration refreshTokenTtl = refreshTokenTtl();
        String accessToken = UUID.randomUUID().toString().replace("-", "");
        String refreshToken = UUID.randomUUID().toString().replace("-", "");
        Instant accessExpireAt = Instant.now().plus(accessTokenTtl);
        Instant refreshExpireAt = Instant.now().plus(refreshTokenTtl);

        tokenStore.store(accessToken, new AuthUser(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                role.getCode(),
                user.getStatus(),
                accessExpireAt
        ), accessTokenTtl);
        tokenStore.storeRefreshToken(refreshToken, new RefreshTokenSession(user.getId(), accessToken, refreshExpireAt), refreshTokenTtl);
        return new TokenBundle(accessToken, refreshToken);
    }

    private LoginVO buildLoginVO(String accessToken, String refreshToken, String roleCode, String realName) {
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(accessToken);
        loginVO.setRefreshToken(refreshToken);
        loginVO.setRoleCode(roleCode);
        loginVO.setRealName(realName);
        return loginVO;
    }

    private boolean isInvalidRefreshSession(RefreshTokenSession refreshTokenSession) {
        return refreshTokenSession == null
                || refreshTokenSession.getUserId() == null
                || refreshTokenSession.getExpireAt() == null
                || !refreshTokenSession.getExpireAt().isAfter(Instant.now());
    }

    private Duration accessTokenTtl() {
        Long hours = authSecurityProperties.getAccessTokenTtlHours();
        return Duration.ofHours(hours == null || hours.longValue() < 1L ? 12L : hours.longValue());
    }

    private Duration refreshTokenTtl() {
        Long days = authSecurityProperties.getRefreshTokenTtlDays();
        return Duration.ofDays(days == null || days.longValue() < 1L ? 7L : days.longValue());
    }

    private String buildAuthLogContent(String username, String clientIp, String action) {
        String safeUsername = safeText(username) == null ? "unknown" : safeText(username);
        String safeIp = safeText(clientIp) == null ? "unknown" : safeText(clientIp);
        return action + "，账号=" + safeUsername + "，IP=" + safeIp;
    }

    private String safeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static class TokenBundle {

        private final String accessToken;
        private final String refreshToken;

        private TokenBundle(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }
}
