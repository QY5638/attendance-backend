package com.quyong.attendance.module.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quyong.attendance.module.device.dto.DeviceQueryDTO;
import com.quyong.attendance.module.device.dto.DeviceSaveDTO;
import com.quyong.attendance.module.device.dto.DeviceStatusDTO;
import com.quyong.attendance.module.device.entity.Device;
import com.quyong.attendance.module.device.mapper.DeviceMapper;
import com.quyong.attendance.module.device.service.DeviceService;
import com.quyong.attendance.module.device.support.DeviceValidationSupport;
import com.quyong.attendance.module.device.vo.DeviceVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeviceServiceImpl implements DeviceService {

    private final DeviceMapper deviceMapper;
    private final DeviceValidationSupport deviceValidationSupport;

    public DeviceServiceImpl(DeviceMapper deviceMapper,
                             DeviceValidationSupport deviceValidationSupport) {
        this.deviceMapper = deviceMapper;
        this.deviceValidationSupport = deviceValidationSupport;
    }

    @Override
    public List<DeviceVO> list(DeviceQueryDTO queryDTO) {
        String keyword = normalize(queryDTO == null ? null : queryDTO.getKeyword());

        LambdaQueryWrapper<Device> queryWrapper = new LambdaQueryWrapper<Device>();
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper.like(Device::getId, keyword)
                    .or()
                    .like(Device::getName, keyword)
                    .or()
                    .like(Device::getLocation, keyword));
        }
        if (queryDTO != null && queryDTO.getStatus() != null) {
            queryWrapper.eq(Device::getStatus, queryDTO.getStatus());
        }
        queryWrapper.orderByAsc(Device::getId);

        return deviceMapper.selectList(queryWrapper)
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public DeviceVO add(DeviceSaveDTO saveDTO) {
        DeviceSaveDTO validatedSaveDTO = deviceValidationSupport.validateForCreate(saveDTO);

        Device device = new Device();
        device.setId(validatedSaveDTO.getDeviceId());
        device.setName(validatedSaveDTO.getName());
        device.setLocation(validatedSaveDTO.getLocation());
        device.setStatus(validatedSaveDTO.getStatus());
        device.setDescription(validatedSaveDTO.getDescription());
        deviceMapper.insert(device);
        return toVO(deviceMapper.selectById(device.getId()));
    }

    @Override
    public DeviceVO update(DeviceSaveDTO saveDTO) {
        DeviceSaveDTO validatedSaveDTO = deviceValidationSupport.validateForUpdate(saveDTO);
        Device device = deviceValidationSupport.requireExistingDevice(validatedSaveDTO.getDeviceId());

        device.setName(validatedSaveDTO.getName());
        device.setLocation(validatedSaveDTO.getLocation());
        device.setStatus(validatedSaveDTO.getStatus());
        device.setDescription(validatedSaveDTO.getDescription());
        deviceMapper.updateById(device);
        return toVO(deviceMapper.selectById(device.getId()));
    }

    @Override
    public DeviceVO updateStatus(DeviceStatusDTO statusDTO) {
        Device device = deviceValidationSupport.requireExistingDevice(statusDTO == null ? null : statusDTO.getDeviceId());
        device.setStatus(deviceValidationSupport.requireValidStatus(statusDTO == null ? null : statusDTO.getStatus()));
        deviceMapper.updateById(device);
        return toVO(deviceMapper.selectById(device.getId()));
    }

    @Override
    public void delete(String deviceId) {
        Device device = deviceValidationSupport.requireDeletableDevice(deviceId);
        deviceMapper.deleteById(device.getId());
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private DeviceVO toVO(Device device) {
        DeviceVO vo = new DeviceVO();
        vo.setDeviceId(device.getId());
        vo.setName(device.getName());
        vo.setLocation(device.getLocation());
        vo.setStatus(device.getStatus());
        vo.setDescription(device.getDescription());
        return vo;
    }
}
