package com.quyong.attendance.module.face.support;

import java.math.BigDecimal;

public interface FaceRecognitionProvider {

    String extractFeature(String imageData);

    BigDecimal compare(String imageData, String storedFeatureData);
}
