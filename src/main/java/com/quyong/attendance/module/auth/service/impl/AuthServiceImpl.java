package com.quyong.attendance.module.auth.service.impl;

import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.auth.dto.LoginDTO;
import com.quyong.attendance.module.auth.model.AuthUser;
import com.quyong.attendance.module.auth.service.AuthService;
import com.quyong.attendance.module.auth.store.TokenStore;
import com.quyong.attendance.module.auth.vo.LoginVO;
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

    private static final Duration TOKEN_TTL = Duration.ofHours(12);

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenStore tokenStore;

    public AuthServiceImpl(UserMapper userMapper,
                           RoleMapper roleMapper,
                           PasswordEncoder passwordEncoder,
                           TokenStore tokenStore) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenStore = tokenStore;
    }

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        User user = userMapper.selectByUsername(loginDTO.getUsername());
        if (user == null || !passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "账号已禁用");
        }

        Role role = roleMapper.selectRoleById(user.getRoleId());
        if (role == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "账号角色不存在");
        }
        if (role.getStatus() == null || role.getStatus() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "账号角色已禁用");
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        Instant expireAt = Instant.now().plus(TOKEN_TTL);
        tokenStore.store(token, new AuthUser(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                role.getCode(),
                user.getStatus(),
                expireAt
        ), TOKEN_TTL);

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setRoleCode(role.getCode());
        loginVO.setRealName(user.getRealName());
        return loginVO;
    }
}
