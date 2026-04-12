package com.quyong.attendance.module.face.service.impl;

import com.quyong.attendance.module.face.config.FaceEngineProperties;
import com.quyong.attendance.module.face.dto.FaceRegisterDTO;
import com.quyong.attendance.module.face.dto.FaceVerifyDTO;
import com.quyong.attendance.module.face.entity.FaceFeature;
import com.quyong.attendance.module.face.mapper.FaceFeatureMapper;
import com.quyong.attendance.module.face.service.FaceLivenessService;
import com.quyong.attendance.module.face.service.FaceService;
import com.quyong.attendance.module.face.support.FaceLivenessProof;
import com.quyong.attendance.module.face.support.FaceRegistrationResult;
import com.quyong.attendance.module.face.support.FaceRecognitionProvider;
import com.quyong.attendance.module.face.support.FaceValidationSupport;
import com.quyong.attendance.module.face.support.FaceVerificationResult;
import com.quyong.attendance.module.face.vo.FaceRegisterVO;
import com.quyong.attendance.module.face.vo.FaceVerifyVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.util.UUID;

@Service
public class FaceServiceImpl implements FaceService {

    private static final BigDecimal UNREGISTERED_SCORE = new BigDecimal("0.00");

    private final FaceFeatureMapper faceFeatureMapper;
    private final FaceValidationSupport faceValidationSupport;
    private final FaceLivenessService faceLivenessService;
    private final FaceRecognitionProvider faceRecognitionProvider;
    private final FaceEngineProperties faceEngineProperties;

    public FaceServiceImpl(FaceFeatureMapper faceFeatureMapper,
                           FaceValidationSupport faceValidationSupport,
                           FaceLivenessService faceLivenessService,
                           FaceRecognitionProvider faceRecognitionProvider,
                           FaceEngineProperties faceEngineProperties) {
        this.faceFeatureMapper = faceFeatureMapper;
        this.faceValidationSupport = faceValidationSupport;
        this.faceLivenessService = faceLivenessService;
        this.faceRecognitionProvider = faceRecognitionProvider;
        this.faceEngineProperties = faceEngineProperties;
    }

    @Override
    public FaceRegisterVO register(FaceRegisterDTO registerDTO) {
        FaceRegisterDTO validatedRegisterDTO = faceValidationSupport.validateRegister(registerDTO);
        FaceLivenessProof livenessProof = faceLivenessService.requireValidProof(
                validatedRegisterDTO.getUserId(),
                validatedRegisterDTO.getLivenessToken(),
                validatedRegisterDTO.getImageData(),
                true
        );
        FaceFeature latestFaceFeature = faceFeatureMapper.selectLatestByUserId(validatedRegisterDTO.getUserId());
        FaceRegistrationResult registrationResult = faceRecognitionProvider.register(
                validatedRegisterDTO.getUserId(),
                validatedRegisterDTO.getImageData(),
                latestFaceFeature == null ? null : latestFaceFeature.getFeatureData()
        );
        applyLivenessMetadata(registrationResult, livenessProof);

        FaceFeature faceFeature = new FaceFeature();
        faceFeature.setUserId(validatedRegisterDTO.getUserId());
        faceFeature.setFeatureData(registrationResult.getFeatureData());
        faceFeature.setFeatureHash(generateFeatureHash(validatedRegisterDTO.getUserId(), registrationResult.getFeatureData()));
        faceFeature.setEncryptFlag(registrationResult.getEncryptFlag());
        faceFeatureMapper.insert(faceFeature);

        FaceFeature savedFaceFeature = faceFeatureMapper.selectById(faceFeature.getId());
        return toRegisterVO(savedFaceFeature == null ? faceFeature : savedFaceFeature, registrationResult);
    }

    @Override
    public FaceVerifyVO verify(FaceVerifyDTO verifyDTO) {
        FaceVerifyDTO validatedVerifyDTO = faceValidationSupport.validateVerify(verifyDTO);
        FaceLivenessProof livenessProof = faceLivenessService.requireValidProof(
                validatedVerifyDTO.getUserId(),
                validatedVerifyDTO.getLivenessToken(),
                validatedVerifyDTO.getImageData(),
                Boolean.TRUE.equals(validatedVerifyDTO.getConsumeLiveness())
        );
        FaceFeature latestFaceFeature = faceFeatureMapper.selectLatestByUserId(validatedVerifyDTO.getUserId());
        BigDecimal threshold = defaultMatchThreshold();
        if (latestFaceFeature == null) {
            return buildVerifyVO(validatedVerifyDTO.getUserId(), false, false, UNREGISTERED_SCORE, threshold, null, null, null, providerCode(), "该用户未录入人脸");
        }

        FaceVerificationResult verificationResult = faceRecognitionProvider.verify(
                validatedVerifyDTO.getUserId(),
                validatedVerifyDTO.getImageData(),
                latestFaceFeature.getFeatureData()
        );
        applyLivenessMetadata(verificationResult, livenessProof);
        return buildVerifyVO(validatedVerifyDTO.getUserId(), true, verificationResult.getMatched(), verificationResult.getFaceScore(), verificationResult.getThreshold(), verificationResult.getQualityScore(), verificationResult.getLivenessScore(), verificationResult.getLivenessPassed(), verificationResult.getProvider(), verificationResult.getMessage());
    }

    private void applyLivenessMetadata(FaceRegistrationResult registrationResult, FaceLivenessProof livenessProof) {
        if (registrationResult == null || livenessProof == null) {
            return;
        }
        registrationResult.setLivenessPassed(Boolean.TRUE);
        registrationResult.setLivenessScore(livenessProof.getLivenessScore());
    }

    private void applyLivenessMetadata(FaceVerificationResult verificationResult, FaceLivenessProof livenessProof) {
        if (verificationResult == null || livenessProof == null) {
            return;
        }
        verificationResult.setLivenessPassed(Boolean.TRUE);
        verificationResult.setLivenessScore(livenessProof.getLivenessScore());
    }

    private FaceRegisterVO toRegisterVO(FaceFeature faceFeature, FaceRegistrationResult registrationResult) {
        FaceRegisterVO vo = new FaceRegisterVO();
        vo.setUserId(faceFeature.getUserId());
        vo.setRegistered(true);
        vo.setMessage("人脸录入成功");
        vo.setCreateTime(faceFeature.getCreateTime());
        vo.setQualityScore(registrationResult.getQualityScore());
        vo.setLivenessScore(registrationResult.getLivenessScore());
        vo.setLivenessPassed(registrationResult.getLivenessPassed());
        vo.setProvider(registrationResult.getProvider());
        return vo;
    }

    private FaceVerifyVO buildVerifyVO(Long userId,
                                       Boolean registered,
                                       Boolean matched,
                                       BigDecimal faceScore,
                                       BigDecimal threshold,
                                       BigDecimal qualityScore,
                                       BigDecimal livenessScore,
                                       Boolean livenessPassed,
                                       String provider,
                                       String message) {
        FaceVerifyVO vo = new FaceVerifyVO();
        vo.setUserId(userId);
        vo.setRegistered(registered);
        vo.setMatched(matched);
        vo.setFaceScore(faceScore);
        vo.setThreshold(threshold);
        vo.setQualityScore(qualityScore);
        vo.setLivenessScore(livenessScore);
        vo.setLivenessPassed(livenessPassed);
        vo.setProvider(provider);
        vo.setMessage(message);
        return vo;
    }

    private String generateFeatureHash(Long userId, String featureData) {
        try {
            String source = userId + ":" + featureData + ":" + UUID.randomUUID();
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digestBytes = messageDigest.digest(source.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digestBytes.length * 2);
            for (byte digestByte : digestBytes) {
                builder.append(String.format("%02x", digestByte));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("人脸模板摘要生成失败", exception);
        }
    }

    private BigDecimal defaultMatchThreshold() {
        BigDecimal threshold = faceEngineProperties.getMatchScoreThreshold();
        if (threshold == null) {
            return new BigDecimal("85.00");
        }
        return threshold.setScale(2, RoundingMode.HALF_UP);
    }

    private String providerCode() {
        String provider = faceEngineProperties.getProvider();
        if (provider == null) {
            return null;
        }
        return provider.trim().toUpperCase();
    }
}
