package com.quyong.attendance.module.face.support;

import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.face.dto.FaceRegisterDTO;
import com.quyong.attendance.module.face.dto.FaceVerifyDTO;
import com.quyong.attendance.module.user.mapper.UserMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class FaceValidationSupport {

    private final UserMapper userMapper;

    public FaceValidationSupport(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public FaceRegisterDTO validateRegister(FaceRegisterDTO registerDTO) {
        FaceRegisterDTO target = registerDTO == null ? new FaceRegisterDTO() : registerDTO;
        target.setUserId(requireExistingUser(target.getUserId()));
        target.setImageData(requireImageData(target.getImageData()));
        return target;
    }

    public FaceVerifyDTO validateVerify(FaceVerifyDTO verifyDTO) {
        FaceVerifyDTO target = verifyDTO == null ? new FaceVerifyDTO() : verifyDTO;
        target.setUserId(requireExistingUser(target.getUserId()));
        target.setImageData(requireImageData(target.getImageData()));
        return target;
    }

    private Long requireExistingUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户编号不能为空");
        }
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户不存在");
        }
        return userId;
    }

    private String requireImageData(String imageData) {
        String normalizedImageData = normalize(imageData);
        if (!StringUtils.hasText(normalizedImageData)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "人脸图像不能为空");
        }
        return normalizedImageData;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
