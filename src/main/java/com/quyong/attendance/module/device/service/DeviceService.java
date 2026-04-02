package com.quyong.attendance.module.device.service;

import com.quyong.attendance.module.device.dto.DeviceQueryDTO;
import com.quyong.attendance.module.device.dto.DeviceSaveDTO;
import com.quyong.attendance.module.device.dto.DeviceStatusDTO;
import com.quyong.attendance.module.device.vo.DeviceVO;

import java.util.List;

public interface DeviceService {

    List<DeviceVO> list(DeviceQueryDTO queryDTO);

    DeviceVO add(DeviceSaveDTO saveDTO);

    DeviceVO update(DeviceSaveDTO saveDTO);

    DeviceVO updateStatus(DeviceStatusDTO statusDTO);

    void delete(String deviceId);
}
