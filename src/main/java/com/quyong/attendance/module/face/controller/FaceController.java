package com.quyong.attendance.module.face.controller;

import com.quyong.attendance.common.api.Result;
import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.auth.model.AuthUser;
import com.quyong.attendance.module.face.dto.FaceRegisterDTO;
import com.quyong.attendance.module.face.dto.FaceVerifyDTO;
import com.quyong.attendance.module.face.service.FaceService;
import com.quyong.attendance.module.face.vo.FaceRegisterVO;
import com.quyong.attendance.module.face.vo.FaceVerifyVO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/face")
public class FaceController {

    private final FaceService faceService;

    public FaceController(FaceService faceService) {
        this.faceService = faceService;
    }

    @PostMapping("/register")
    public Result<FaceRegisterVO> register(@RequestBody(required = false) FaceRegisterDTO registerDTO) {
        FaceRegisterDTO target = registerDTO == null ? new FaceRegisterDTO() : registerDTO;
        target.setUserId(currentAuthUser().getUserId());
        return Result.success(faceService.register(target));
    }

    @PostMapping("/verify")
    public Result<FaceVerifyVO> verify(@RequestBody(required = false) FaceVerifyDTO verifyDTO) {
        FaceVerifyDTO target = verifyDTO == null ? new FaceVerifyDTO() : verifyDTO;
        target.setUserId(currentAuthUser().getUserId());
        return Result.success(faceService.verify(target));
    }

    private AuthUser currentAuthUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
        }
        return (AuthUser) authentication.getPrincipal();
    }
}
