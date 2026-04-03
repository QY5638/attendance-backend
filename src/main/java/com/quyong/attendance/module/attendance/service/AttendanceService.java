package com.quyong.attendance.module.attendance.service;

import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.module.attendance.dto.AttendanceCheckinDTO;
import com.quyong.attendance.module.attendance.dto.AttendanceListQueryDTO;
import com.quyong.attendance.module.attendance.dto.AttendanceRecordQueryDTO;
import com.quyong.attendance.module.attendance.dto.AttendanceRepairDTO;
import com.quyong.attendance.module.attendance.vo.AttendanceCheckinVO;
import com.quyong.attendance.module.attendance.vo.AttendanceRecordVO;
import com.quyong.attendance.module.attendance.vo.AttendanceRepairVO;

public interface AttendanceService {

    AttendanceCheckinVO checkin(AttendanceCheckinDTO dto);

    PageResult<AttendanceRecordVO> record(Long userId, AttendanceRecordQueryDTO dto);

    PageResult<AttendanceRecordVO> list(AttendanceListQueryDTO dto);

    AttendanceRepairVO repair(AttendanceRepairDTO dto);
}
