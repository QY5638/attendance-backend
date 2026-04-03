package com.quyong.attendance.module.face.controller;

import com.quyong.attendance.common.api.Result;
import com.quyong.attendance.module.face.dto.FaceRegisterDTO;
import com.quyong.attendance.module.face.dto.FaceVerifyDTO;
import com.quyong.attendance.module.face.service.FaceService;
import com.quyong.attendance.module.face.vo.FaceRegisterVO;
import com.quyong.attendance.module.face.vo.FaceVerifyVO;
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
        return Result.success(faceService.register(registerDTO));
    }

    @PostMapping("/verify")
    public Result<FaceVerifyVO> verify(@RequestBody(required = false) FaceVerifyDTO verifyDTO) {
        return Result.success(faceService.verify(verifyDTO));
    }
}
