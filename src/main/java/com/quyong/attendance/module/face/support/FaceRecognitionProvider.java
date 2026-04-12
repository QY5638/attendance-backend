package com.quyong.attendance.module.face.support;

public interface FaceRecognitionProvider {

    FaceRegistrationResult register(Long userId, String imageData, String previousFeatureData);

    FaceVerificationResult verify(Long userId, String imageData, String storedFeatureData);
}
