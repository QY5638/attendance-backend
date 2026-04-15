package com.quyong.attendance.module.exceptiondetect.support;

import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.exceptiondetect.dto.ComplexCheckDTO;
import com.quyong.attendance.module.exceptiondetect.dto.ExceptionQueryDTO;
import com.quyong.attendance.module.exceptiondetect.dto.RuleCheckDTO;
import org.springframework.stereotype.Component;

@Component
public class ExceptionValidationSupport {

    public RuleCheckDTO validateRuleCheck(RuleCheckDTO dto) {
        if (dto == null || (dto.getRecordId() == null && dto.getExceptionId() == null)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "考勤记录ID或异常ID不能为空");
        }
        return dto;
    }

    public ComplexCheckDTO validateComplexCheck(ComplexCheckDTO dto) {
        if (dto == null || (dto.getRecordId() == null && dto.getExceptionId() == null)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "考勤记录ID或异常ID不能为空");
        }
        return dto;
    }

    public ExceptionQueryDTO validateQuery(ExceptionQueryDTO dto) {
        ExceptionQueryDTO safe = dto == null ? new ExceptionQueryDTO() : dto;
        safe.setPageNum(safe.getPageNum() == null || safe.getPageNum().intValue() < 1 ? 1 : safe.getPageNum());
        safe.setPageSize(safe.getPageSize() == null || safe.getPageSize().intValue() < 1 ? 10 : safe.getPageSize());
        safe.setType(normalize(safe.getType()));
        safe.setRiskLevel(normalize(safe.getRiskLevel()));
        safe.setProcessStatus(normalize(safe.getProcessStatus()));
        safe.setUserKeyword(normalize(safe.getUserKeyword()));
        return safe;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
