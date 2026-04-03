package com.quyong.attendance.module.exceptiondetect.service;

import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.module.exceptiondetect.dto.ExceptionQueryDTO;
import com.quyong.attendance.module.exceptiondetect.vo.AttendanceExceptionVO;
import com.quyong.attendance.module.exceptiondetect.vo.ExceptionAnalysisBriefVO;

public interface ExceptionQueryService {

    PageResult<AttendanceExceptionVO> list(ExceptionQueryDTO queryDTO);

    AttendanceExceptionVO getById(Long id);

    ExceptionAnalysisBriefVO getAnalysisBrief(Long id);
}
