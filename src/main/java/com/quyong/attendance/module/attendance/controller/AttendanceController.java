package com.quyong.attendance.module.attendance.controller;

import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.common.api.Result;
import com.quyong.attendance.module.attendance.dto.AttendanceCheckinDTO;
import com.quyong.attendance.module.attendance.dto.AttendanceListQueryDTO;
import com.quyong.attendance.module.attendance.dto.AttendanceRecordQueryDTO;
import com.quyong.attendance.module.attendance.dto.AttendanceRepairDTO;
import com.quyong.attendance.module.attendance.service.AttendanceService;
import com.quyong.attendance.module.attendance.vo.AttendanceCheckinVO;
import com.quyong.attendance.module.attendance.vo.AttendanceRecordVO;
import com.quyong.attendance.module.attendance.vo.AttendanceRepairVO;
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

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/checkin")
    public Result<AttendanceCheckinVO> checkin(@RequestBody(required = false) AttendanceCheckinDTO dto) {
        return Result.success(attendanceService.checkin(dto));
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
        return Result.success(attendanceService.repair(dto));
    }
}
