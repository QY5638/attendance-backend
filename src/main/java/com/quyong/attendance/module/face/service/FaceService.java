package com.quyong.attendance.module.face.service;

import com.quyong.attendance.module.face.dto.FaceRegisterDTO;
import com.quyong.attendance.module.face.dto.FaceVerifyDTO;
import com.quyong.attendance.module.face.vo.FaceRegisterVO;
import com.quyong.attendance.module.face.vo.FaceVerifyVO;

public interface FaceService {

    FaceRegisterVO register(FaceRegisterDTO registerDTO);

    FaceVerifyVO verify(FaceVerifyDTO verifyDTO);
}
