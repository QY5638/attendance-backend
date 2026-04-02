package com.quyong.attendance.module.device.dto;

public class DeviceStatusDTO {

    private String deviceId;
    private Integer status;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
