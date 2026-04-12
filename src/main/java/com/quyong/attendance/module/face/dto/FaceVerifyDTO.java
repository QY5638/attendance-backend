package com.quyong.attendance.module.face.dto;

public class FaceVerifyDTO {

    private Long userId;
    private String imageData;
    private String livenessToken;
    private Boolean consumeLiveness;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getImageData() {
        return imageData;
    }

    public void setImageData(String imageData) {
        this.imageData = imageData;
    }

    public String getLivenessToken() {
        return livenessToken;
    }

    public void setLivenessToken(String livenessToken) {
        this.livenessToken = livenessToken;
    }

    public Boolean getConsumeLiveness() {
        return consumeLiveness;
    }

    public void setConsumeLiveness(Boolean consumeLiveness) {
        this.consumeLiveness = consumeLiveness;
    }
}
