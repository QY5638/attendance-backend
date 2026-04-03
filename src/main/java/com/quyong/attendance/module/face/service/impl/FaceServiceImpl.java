package com.quyong.attendance.module.face.service.impl;

import com.quyong.attendance.module.face.dto.FaceRegisterDTO;
import com.quyong.attendance.module.face.dto.FaceVerifyDTO;
import com.quyong.attendance.module.face.entity.FaceFeature;
import com.quyong.attendance.module.face.mapper.FaceFeatureMapper;
import com.quyong.attendance.module.face.service.FaceService;
import com.quyong.attendance.module.face.support.FaceRecognitionProvider;
import com.quyong.attendance.module.face.support.FaceValidationSupport;
import com.quyong.attendance.module.face.vo.FaceRegisterVO;
import com.quyong.attendance.module.face.vo.FaceVerifyVO;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class FaceServiceImpl implements FaceService {

    private static final BigDecimal FACE_THRESHOLD = new BigDecimal("85.00");
    private static final BigDecimal UNREGISTERED_SCORE = new BigDecimal("0.00");

    private final FaceFeatureMapper faceFeatureMapper;
    private final FaceValidationSupport faceValidationSupport;
    private final FaceRecognitionProvider faceRecognitionProvider;

    public FaceServiceImpl(FaceFeatureMapper faceFeatureMapper,
                           FaceValidationSupport faceValidationSupport,
                           FaceRecognitionProvider faceRecognitionProvider) {
        this.faceFeatureMapper = faceFeatureMapper;
        this.faceValidationSupport = faceValidationSupport;
        this.faceRecognitionProvider = faceRecognitionProvider;
    }

    @Override
    public FaceRegisterVO register(FaceRegisterDTO registerDTO) {
        FaceRegisterDTO validatedRegisterDTO = faceValidationSupport.validateRegister(registerDTO);
        String featureData = faceRecognitionProvider.extractFeature(validatedRegisterDTO.getImageData());

        FaceFeature faceFeature = new FaceFeature();
        faceFeature.setUserId(validatedRegisterDTO.getUserId());
        faceFeature.setFeatureData(featureData);
        faceFeature.setFeatureHash(generateFeatureHash(validatedRegisterDTO.getUserId(), featureData));
        faceFeature.setEncryptFlag(1);
        faceFeatureMapper.insert(faceFeature);

        FaceFeature savedFaceFeature = faceFeatureMapper.selectById(faceFeature.getId());
        return toRegisterVO(savedFaceFeature == null ? faceFeature : savedFaceFeature);
    }

    @Override
    public FaceVerifyVO verify(FaceVerifyDTO verifyDTO) {
        FaceVerifyDTO validatedVerifyDTO = faceValidationSupport.validateVerify(verifyDTO);
        FaceFeature latestFaceFeature = faceFeatureMapper.selectLatestByUserId(validatedVerifyDTO.getUserId());
        if (latestFaceFeature == null) {
            return buildVerifyVO(validatedVerifyDTO.getUserId(), false, false, UNREGISTERED_SCORE, "该用户未录入人脸");
        }

        BigDecimal faceScore = faceRecognitionProvider.compare(validatedVerifyDTO.getImageData(), latestFaceFeature.getFeatureData());
        boolean matched = faceScore.compareTo(FACE_THRESHOLD) >= 0;
        return buildVerifyVO(
                validatedVerifyDTO.getUserId(),
                true,
                matched,
                faceScore,
                matched ? "人脸验证通过" : "人脸验证未通过"
        );
    }

    private FaceRegisterVO toRegisterVO(FaceFeature faceFeature) {
        FaceRegisterVO vo = new FaceRegisterVO();
        vo.setUserId(faceFeature.getUserId());
        vo.setRegistered(true);
        vo.setMessage("人脸录入成功");
        vo.setCreateTime(faceFeature.getCreateTime());
        return vo;
    }

    private FaceVerifyVO buildVerifyVO(Long userId,
                                       Boolean registered,
                                       Boolean matched,
                                       BigDecimal faceScore,
                                       String message) {
        FaceVerifyVO vo = new FaceVerifyVO();
        vo.setUserId(userId);
        vo.setRegistered(registered);
        vo.setMatched(matched);
        vo.setFaceScore(faceScore);
        vo.setThreshold(FACE_THRESHOLD);
        vo.setMessage(message);
        return vo;
    }

    private String generateFeatureHash(Long userId, String featureData) {
        String source = userId + ":" + featureData + ":" + UUID.randomUUID();
        return DigestUtils.md5DigestAsHex(source.getBytes(StandardCharsets.UTF_8));
    }
}
