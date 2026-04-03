package com.quyong.attendance.module.attendance.dto;

public class AttendanceCheckinDTO {

    private Long userId;
    private String checkType;
    private String deviceId;
    private String ipAddr;
    private String location;
    private String imageData;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCheckType() {
        return checkType;
    }

    public void setCheckType(String checkType) {
        this.checkType = checkType;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getIpAddr() {
        return ipAddr;
    }

    public void setIpAddr(String ipAddr) {
        this.ipAddr = ipAddr;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getImageData() {
        return imageData;
    }

    public void setImageData(String imageData) {
        this.imageData = imageData;
    }
}
