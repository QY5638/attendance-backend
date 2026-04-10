package com.quyong.attendance.module.device.support;

import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.device.dto.DeviceSaveDTO;
import com.quyong.attendance.module.device.entity.Device;
import com.quyong.attendance.module.device.mapper.DeviceMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DeviceValidationSupport {

    private final DeviceMapper deviceMapper;

    public DeviceValidationSupport(DeviceMapper deviceMapper) {
        this.deviceMapper = deviceMapper;
    }

    public DeviceSaveDTO validateForCreate(DeviceSaveDTO saveDTO) {
        DeviceSaveDTO target = saveDTO == null ? new DeviceSaveDTO() : saveDTO;

        String deviceId = normalize(target.getDeviceId());
        if (!StringUtils.hasText(deviceId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "地点编号不能为空");
        }

        String name = normalize(target.getName());
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "管理名称不能为空");
        }

        if (deviceMapper.selectById(deviceId) != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "地点编号已存在");
        }

        target.setDeviceId(deviceId);
        target.setName(name);
        target.setLocation(normalize(target.getLocation()));
        target.setDescription(normalize(target.getDescription()));
        target.setStatus(resolveStatus(target.getStatus()));
        return target;
    }

    public DeviceSaveDTO validateForUpdate(DeviceSaveDTO saveDTO) {
        DeviceSaveDTO target = saveDTO == null ? new DeviceSaveDTO() : saveDTO;

        String deviceId = normalize(target.getDeviceId());
        if (!StringUtils.hasText(deviceId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "地点编号不能为空");
        }

        Device existingDevice = requireExistingDevice(deviceId);

        String name = normalize(target.getName());
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "管理名称不能为空");
        }

        target.setDeviceId(deviceId);
        target.setName(name);
        target.setLocation(normalize(target.getLocation()));
        target.setDescription(normalize(target.getDescription()));
        target.setStatus(target.getStatus() == null ? existingDevice.getStatus() : requireValidStatus(target.getStatus()));
        return target;
    }

    public Device requireExistingDevice(String deviceId) {
        String normalizedDeviceId = requireDeviceId(deviceId);
        Device device = deviceMapper.selectById(normalizedDeviceId);
        if (device == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "打卡地点不存在");
        }
        return device;
    }

    public Integer requireValidStatus(Integer status) {
        if (status == null || (!Integer.valueOf(0).equals(status) && !Integer.valueOf(1).equals(status))) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "地点状态不合法");
        }
        return status;
    }

    public Device requireDeletableDevice(String deviceId) {
        Device device = requireExistingDevice(deviceId);
        if (deviceMapper.countAttendanceRecordByDeviceId(device.getId()) > 0
                || deviceMapper.countAttendanceRecordByLocation(device.getLocation()) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "打卡地点已关联考勤记录，不能删除，请先停用地点");
        }
        return device;
    }

    public String requireDeviceId(String deviceId) {
        String normalizedDeviceId = normalize(deviceId);
        if (!StringUtils.hasText(normalizedDeviceId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "地点编号不能为空");
        }
        return normalizedDeviceId;
    }

    private Integer resolveStatus(Integer status) {
        if (status == null) {
            return 1;
        }
        return requireValidStatus(status);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
