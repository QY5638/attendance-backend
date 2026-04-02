package com.quyong.attendance.module.user.support;

import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class UserPasswordSupport {

    private final PasswordEncoder passwordEncoder;

    public UserPasswordSupport(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String encodeForCreate(String password) {
        if (!StringUtils.hasText(password)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "密码不能为空");
        }
        return passwordEncoder.encode(password);
    }

    public String resolvePasswordForUpdate(String password, String currentPassword) {
        if (!StringUtils.hasText(password)) {
            return currentPassword;
        }
        return passwordEncoder.encode(password);
    }
}
