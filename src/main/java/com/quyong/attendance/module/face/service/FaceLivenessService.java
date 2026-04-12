package com.quyong.attendance.module.face.service;

import com.quyong.attendance.module.face.dto.FaceLivenessCompleteDTO;
import com.quyong.attendance.module.face.support.FaceLivenessProof;
import com.quyong.attendance.module.face.vo.FaceLivenessCompleteVO;
import com.quyong.attendance.module.face.vo.FaceLivenessSessionVO;

public interface FaceLivenessService {

    FaceLivenessSessionVO createSession(Long userId);

    FaceLivenessCompleteVO complete(Long userId, FaceLivenessCompleteDTO completeDTO);

    FaceLivenessProof requireValidProof(Long userId, String livenessToken, String imageData, boolean consume);
}
