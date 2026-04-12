package com.quyong.attendance.module.auth.controller;

import com.quyong.attendance.common.api.Result;
import com.quyong.attendance.module.auth.dto.LogoutDTO;
import com.quyong.attendance.module.auth.dto.LoginDTO;
import com.quyong.attendance.module.auth.dto.RefreshTokenDTO;
import com.quyong.attendance.module.auth.model.AuthUser;
import com.quyong.attendance.module.auth.service.AuthService;
import com.quyong.attendance.module.auth.support.AuthRequestSupport;
import com.quyong.attendance.module.auth.vo.LoginVO;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Validated LoginDTO loginDTO, HttpServletRequest request) {
        return Result.success(authService.login(loginDTO, AuthRequestSupport.resolveClientIp(request)));
    }

    @PostMapping("/refresh")
    public Result<LoginVO> refresh(@RequestBody @Validated RefreshTokenDTO refreshTokenDTO, HttpServletRequest request) {
        return Result.success(authService.refresh(refreshTokenDTO, AuthRequestSupport.resolveClientIp(request)));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                               @RequestBody(required = false) LogoutDTO logoutDTO,
                               Authentication authentication,
                               HttpServletRequest request) {
        AuthUser authUser = authentication != null && authentication.getPrincipal() instanceof AuthUser
                ? (AuthUser) authentication.getPrincipal()
                : null;
        authService.logout(authorization, logoutDTO, authUser, AuthRequestSupport.resolveClientIp(request));
        return Result.success(null);
    }
}
