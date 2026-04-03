package com.quyong.attendance.module.face.support;

import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@Component
public class LocalFaceRecognitionProvider implements FaceRecognitionProvider {

    @Override
    public String extractFeature(String imageData) {
        String normalizedImageData = normalize(imageData);
        return DigestUtils.md5DigestAsHex(normalizedImageData.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public BigDecimal compare(String imageData, String storedFeatureData) {
        String currentFeature = extractFeature(imageData);
        if (currentFeature.equals(storedFeatureData)) {
            return new BigDecimal("99.99");
        }
        return new BigDecimal("0.00");
    }

    private String normalize(String imageData) {
        return imageData == null ? "" : imageData.replaceAll("\\s+", "");
    }
}
