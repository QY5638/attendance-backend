package com.quyong.attendance.module.face.support;

import com.quyong.attendance.module.face.config.FaceEngineProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;

@Service
@ConditionalOnProperty(prefix = "app.face", name = "provider", havingValue = "stub")
public class LocalFaceRecognitionProvider implements FaceRecognitionProvider {

    private static final String PROVIDER_CODE = "TEST_STUB";

    private final FaceEngineProperties properties;
    private final FaceTemplateCodec faceTemplateCodec;

    public LocalFaceRecognitionProvider(FaceEngineProperties properties, FaceTemplateCodec faceTemplateCodec) {
        this.properties = properties;
        this.faceTemplateCodec = faceTemplateCodec;
    }

    @Override
    public FaceRegistrationResult register(Long userId, String imageData, String previousFeatureData) {
        FaceTemplateMetadata metadata = new FaceTemplateMetadata();
        metadata.setProvider(PROVIDER_CODE);
        metadata.setVersion(Integer.valueOf(1));
        metadata.setEntityId(String.valueOf(userId));
        metadata.setReferenceDigest(digest(imageData));
        metadata.setQualityScore(new BigDecimal("100.00"));
        metadata.setLivenessScore(new BigDecimal("100.00"));

        FaceRegistrationResult result = new FaceRegistrationResult();
        result.setFeatureData(faceTemplateCodec.encode(metadata));
        result.setEncryptFlag(Integer.valueOf(1));
        result.setProvider(PROVIDER_CODE);
        result.setQualityScore(new BigDecimal("100.00"));
        result.setLivenessScore(new BigDecimal("100.00"));
        result.setLivenessPassed(Boolean.TRUE);
        return result;
    }

    @Override
    public FaceVerificationResult verify(Long userId, String imageData, String storedFeatureData) {
        String expectedDigest = resolveExpectedDigest(storedFeatureData);
        String currentDigest = digest(imageData);
        BigDecimal threshold = defaultMatchThreshold();
        boolean matched = currentDigest.equals(expectedDigest);

        FaceVerificationResult result = new FaceVerificationResult();
        result.setProvider(PROVIDER_CODE);
        result.setThreshold(threshold);
        result.setLivenessPassed(Boolean.TRUE);
        result.setLivenessScore(new BigDecimal("100.00"));
        result.setQualityScore(new BigDecimal("100.00"));
        result.setMatched(Boolean.valueOf(matched));
        result.setFaceScore(matched ? new BigDecimal("99.99") : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        result.setMessage(matched ? "人脸验证通过" : "人脸验证未通过");
        return result;
    }

    private String resolveExpectedDigest(String storedFeatureData) {
        FaceTemplateMetadata metadata = faceTemplateCodec.decode(storedFeatureData);
        if (metadata != null && StringUtils.hasText(metadata.getReferenceDigest())) {
            return metadata.getReferenceDigest();
        }
        return storedFeatureData == null ? "" : storedFeatureData.trim();
    }

    private String digest(String imageData) {
        String normalized = imageData == null ? "" : imageData.replaceAll("\\s+", "");
        return DigestUtils.md5DigestAsHex(normalized.getBytes(StandardCharsets.UTF_8));
    }

    private BigDecimal defaultMatchThreshold() {
        BigDecimal threshold = properties.getMatchScoreThreshold();
        if (threshold == null) {
            return new BigDecimal("85.00");
        }
        return threshold.setScale(2, RoundingMode.HALF_UP);
    }
}
