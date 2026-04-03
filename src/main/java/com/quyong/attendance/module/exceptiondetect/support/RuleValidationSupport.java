package com.quyong.attendance.module.exceptiondetect.support;

import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.exceptiondetect.dto.RuleQueryDTO;
import com.quyong.attendance.module.exceptiondetect.dto.RuleSaveDTO;
import com.quyong.attendance.module.exceptiondetect.dto.RuleStatusDTO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class RuleValidationSupport {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public RuleQueryDTO validateQuery(RuleQueryDTO dto) {
        RuleQueryDTO safe = dto == null ? new RuleQueryDTO() : dto;
        safe.setKeyword(normalize(safe.getKeyword()));
        safe.setPageNum(safe.getPageNum() == null || safe.getPageNum() < 1 ? 1 : safe.getPageNum());
        safe.setPageSize(safe.getPageSize() == null || safe.getPageSize() < 1 ? 10 : safe.getPageSize());
        if (safe.getStatus() != null && safe.getStatus().intValue() != 0 && safe.getStatus().intValue() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "规则状态不合法");
        }
        return safe;
    }

    public RuleSaveDTO validateSave(RuleSaveDTO dto, boolean requireId) {
        if (dto == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请求参数错误");
        }
        if (requireId && dto.getId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "规则ID不能为空");
        }
        dto.setName(normalize(dto.getName()));
        dto.setStartTime(normalize(dto.getStartTime()));
        dto.setEndTime(normalize(dto.getEndTime()));
        if (!StringUtils.hasText(dto.getName())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "规则名称不能为空");
        }
        if (!StringUtils.hasText(dto.getStartTime()) || !StringUtils.hasText(dto.getEndTime())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "上下班时间不能为空");
        }
        if (dto.getLateThreshold() == null || dto.getLateThreshold().intValue() < 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "迟到阈值不合法");
        }
        if (dto.getEarlyThreshold() == null || dto.getEarlyThreshold().intValue() < 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "早退阈值不合法");
        }
        if (dto.getRepeatLimit() == null || dto.getRepeatLimit().intValue() < 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "重复打卡阈值不合法");
        }
        parseTime(dto.getStartTime(), "上班时间");
        parseTime(dto.getEndTime(), "下班时间");
        return dto;
    }

    public RuleStatusDTO validateStatus(RuleStatusDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "规则ID不能为空");
        }
        if (dto.getStatus() == null || (dto.getStatus().intValue() != 0 && dto.getStatus().intValue() != 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "规则状态不合法");
        }
        return dto;
    }

    public LocalTime parseTime(String value, String fieldName) {
        try {
            return LocalTime.parse(value, TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), fieldName + "格式错误");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
