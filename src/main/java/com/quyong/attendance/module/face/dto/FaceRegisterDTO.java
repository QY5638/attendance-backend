package com.quyong.attendance.module.face.dto;

public class FaceRegisterDTO {

    private Long userId;
    private String imageData;

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
}
