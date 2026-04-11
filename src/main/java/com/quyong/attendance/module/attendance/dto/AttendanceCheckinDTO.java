package com.quyong.attendance.module.attendance.dto;

import java.math.BigDecimal;

public class AttendanceCheckinDTO {

    private Long userId;
    private String checkType;
    private String deviceId;
    private String deviceInfo;
    private String ipAddr;
    private String location;
    private BigDecimal clientLongitude;
    private BigDecimal clientLatitude;
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

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
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

    public BigDecimal getClientLongitude() {
        return clientLongitude;
    }

    public void setClientLongitude(BigDecimal clientLongitude) {
        this.clientLongitude = clientLongitude;
    }

    public BigDecimal getClientLatitude() {
        return clientLatitude;
    }

    public void setClientLatitude(BigDecimal clientLatitude) {
        this.clientLatitude = clientLatitude;
    }

    public String getImageData() {
        return imageData;
    }

    public void setImageData(String imageData) {
        this.imageData = imageData;
    }
}
