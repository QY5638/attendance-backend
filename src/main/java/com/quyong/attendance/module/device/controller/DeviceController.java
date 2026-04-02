package com.quyong.attendance.module.device.controller;

import com.quyong.attendance.common.api.Result;
import com.quyong.attendance.module.device.dto.DeviceQueryDTO;
import com.quyong.attendance.module.device.dto.DeviceSaveDTO;
import com.quyong.attendance.module.device.dto.DeviceStatusDTO;
import com.quyong.attendance.module.device.service.DeviceService;
import com.quyong.attendance.module.device.vo.DeviceVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/device")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping("/list")
    public Result<List<DeviceVO>> list(DeviceQueryDTO queryDTO) {
        return Result.success(deviceService.list(queryDTO));
    }

    @PostMapping("/add")
    public Result<DeviceVO> add(@RequestBody(required = false) DeviceSaveDTO saveDTO) {
        return Result.success(deviceService.add(saveDTO));
    }

    @PutMapping("/update")
    public Result<DeviceVO> update(@RequestBody(required = false) DeviceSaveDTO saveDTO) {
        return Result.success(deviceService.update(saveDTO));
    }

    @PutMapping("/status")
    public Result<DeviceVO> updateStatus(@RequestBody(required = false) DeviceStatusDTO statusDTO) {
        return Result.success(deviceService.updateStatus(statusDTO));
    }

    @DeleteMapping("/{deviceId}")
    public Result<Void> delete(@PathVariable String deviceId) {
        deviceService.delete(deviceId);
        return Result.success(null);
    }
}
