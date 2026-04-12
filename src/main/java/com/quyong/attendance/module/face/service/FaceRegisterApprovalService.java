package com.quyong.attendance.module.face.service;

import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.module.face.dto.FaceRegisterApplyDTO;
import com.quyong.attendance.module.face.dto.FaceRegisterApprovalQueryDTO;
import com.quyong.attendance.module.face.dto.FaceRegisterApprovalReviewDTO;
import com.quyong.attendance.module.face.vo.FaceRegisterApprovalVO;
import com.quyong.attendance.module.face.vo.FaceRegisterStatusVO;

public interface FaceRegisterApprovalService {

    FaceRegisterStatusVO getStatus(Long userId);

    FaceRegisterStatusVO apply(Long userId, FaceRegisterApplyDTO dto);

    PageResult<FaceRegisterApprovalVO> list(FaceRegisterApprovalQueryDTO dto);

    FaceRegisterApprovalVO review(Long reviewUserId, FaceRegisterApprovalReviewDTO dto);

    void requireApproved(Long userId);

    void consumeApproved(Long userId);
}
