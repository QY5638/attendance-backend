package com.quyong.attendance.module.auth.service;

import com.quyong.attendance.module.auth.dto.LogoutDTO;
import com.quyong.attendance.module.auth.dto.LoginDTO;
import com.quyong.attendance.module.auth.dto.RefreshTokenDTO;
import com.quyong.attendance.module.auth.model.AuthUser;
import com.quyong.attendance.module.auth.vo.LoginVO;

public interface AuthService {

    LoginVO login(LoginDTO loginDTO, String clientIp);

    LoginVO refresh(RefreshTokenDTO refreshTokenDTO, String clientIp);

    void logout(String authorization, LogoutDTO logoutDTO, AuthUser authUser, String clientIp);
}
