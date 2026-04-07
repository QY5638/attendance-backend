package com.quyong.attendance.module.model.log.support;

import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.model.log.dto.ModelCallLogQueryDTO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class ModelCallLogValidationSupport {

    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ModelCallLogQueryDTO validateQuery(ModelCallLogQueryDTO dto) {
        ModelCallLogQueryDTO safe = dto == null ? new ModelCallLogQueryDTO() : dto;
        safe.setPageNum(resolvePageNum(safe.getPageNum()));
        safe.setPageSize(resolvePageSize(safe.getPageSize()));
        safe.setBusinessType(normalize(safe.getBusinessType()));
        safe.setStatus(normalize(safe.getStatus()));
        safe.setStartDate(normalize(safe.getStartDate()));
        safe.setEndDate(normalize(safe.getEndDate()));
        if (StringUtils.hasText(safe.getStatus()) && !"SUCCESS".equals(safe.getStatus()) && !"FAILED".equals(safe.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "模型调用状态不合法");
        }
        return safe;
    }

    public LocalDateTime parseQueryStart(String value) {
        return parseQueryDateTime(value, true);
    }

    public LocalDateTime parseQueryEnd(String value) {
        return parseQueryDateTime(value, false);
    }

    private LocalDateTime parseQueryDateTime(String value, boolean startOfDay) {
        String normalizedValue = normalize(value);
        if (!StringUtils.hasText(normalizedValue)) {
            return null;
        }
        try {
            return LocalDateTime.parse(normalizedValue, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ignore) {
            try {
                LocalDate date = LocalDate.parse(normalizedValue, DATE_FORMATTER);
                return startOfDay ? date.atStartOfDay() : date.atTime(23, 59, 59);
            } catch (DateTimeParseException exception) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "查询时间格式不正确");
            }
        }
    }

    private Integer resolvePageNum(Integer pageNum) {
        if (pageNum == null || pageNum.intValue() < 1) {
            return DEFAULT_PAGE_NUM;
        }
        return pageNum;
    }

    private Integer resolvePageSize(Integer pageSize) {
        if (pageSize == null || pageSize.intValue() < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize.intValue(), MAX_PAGE_SIZE);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
