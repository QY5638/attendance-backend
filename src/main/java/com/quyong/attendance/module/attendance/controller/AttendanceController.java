package com.quyong.attendance.module.attendance.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.common.api.Result;
import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.attendance.dto.AttendanceCheckinDTO;
import com.quyong.attendance.module.attendance.dto.AttendanceListQueryDTO;
import com.quyong.attendance.module.attendance.dto.AttendanceRecordQueryDTO;
import com.quyong.attendance.module.attendance.dto.AttendanceRepairDTO;
import com.quyong.attendance.module.attendance.service.AttendanceService;
import com.quyong.attendance.module.attendance.vo.AttendanceCheckinVO;
import com.quyong.attendance.module.attendance.vo.AttendanceDeviceOptionVO;
import com.quyong.attendance.module.attendance.vo.AttendanceRecordVO;
import com.quyong.attendance.module.attendance.vo.AttendanceRepairVO;
import com.quyong.attendance.module.auth.model.AuthUser;
import com.quyong.attendance.module.device.entity.Device;
import com.quyong.attendance.module.device.mapper.DeviceMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final DeviceMapper deviceMapper;

    public AttendanceController(AttendanceService attendanceService, DeviceMapper deviceMapper) {
        this.attendanceService = attendanceService;
        this.deviceMapper = deviceMapper;
    }

    @PostMapping("/checkin")
    public Result<AttendanceCheckinVO> checkin(HttpServletRequest request,
                                               @RequestBody(required = false) AttendanceCheckinDTO dto) {
        AttendanceCheckinDTO target = dto == null ? new AttendanceCheckinDTO() : dto;
        target.setUserId(currentAuthUser().getUserId());
        target.setIpAddr(request == null ? null : request.getRemoteAddr());
        return Result.success(attendanceService.checkin(target));
    }

    @GetMapping("/device-options")
    public Result<List<AttendanceDeviceOptionVO>> deviceOptions() {
        List<AttendanceDeviceOptionVO> options = deviceMapper.selectList(Wrappers.<Device>lambdaQuery()
                        .eq(Device::getStatus, 1)
                        .orderByAsc(Device::getId))
                .stream()
                .map(this::toDeviceOption)
                .collect(Collectors.toList());
        return Result.success(options);
    }

    @GetMapping("/record/me")
    public Result<PageResult<AttendanceRecordVO>> recordMe(AttendanceRecordQueryDTO dto) {
        return Result.success(attendanceService.record(currentAuthUser().getUserId(), dto));
    }

    @GetMapping("/record/{userId}")
    public Result<PageResult<AttendanceRecordVO>> record(@PathVariable Long userId, AttendanceRecordQueryDTO dto) {
        return Result.success(attendanceService.record(userId, dto));
    }

    @GetMapping("/list")
    public Result<PageResult<AttendanceRecordVO>> list(AttendanceListQueryDTO dto) {
        return Result.success(attendanceService.list(dto));
    }

    @PostMapping("/repair")
    public Result<AttendanceRepairVO> repair(@RequestBody(required = false) AttendanceRepairDTO dto) {
        AttendanceRepairDTO target = dto == null ? new AttendanceRepairDTO() : dto;
        target.setUserId(currentAuthUser().getUserId());
        return Result.success(attendanceService.repair(target));
    }

    private AuthUser currentAuthUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
        }
        return (AuthUser) authentication.getPrincipal();
    }

    private AttendanceDeviceOptionVO toDeviceOption(Device device) {
        AttendanceDeviceOptionVO option = new AttendanceDeviceOptionVO();
        option.setDeviceId(device.getId());
        option.setName(device.getName());
        option.setLocation(device.getLocation());
        return option;
    }
}
