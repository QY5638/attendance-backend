package com.quyong.attendance.module.review.service;

import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.module.review.dto.ExceptionTypeQueryDTO;
import com.quyong.attendance.module.review.dto.ExceptionTypeUpdateDTO;
import com.quyong.attendance.module.review.vo.ExceptionTypeVO;

public interface ExceptionTypeService {

    PageResult<ExceptionTypeVO> list(ExceptionTypeQueryDTO dto);

    void update(ExceptionTypeUpdateDTO dto);
}
