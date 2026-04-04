package com.quyong.attendance.module.statistics.support;

import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.statistics.dto.DepartmentRiskBriefQueryDTO;
import com.quyong.attendance.module.statistics.dto.DepartmentStatisticsQueryDTO;
import com.quyong.attendance.module.statistics.dto.ExceptionTrendQueryDTO;
import com.quyong.attendance.module.statistics.dto.OperationLogQueryDTO;
import com.quyong.attendance.module.statistics.dto.PersonalStatisticsQueryDTO;
import com.quyong.attendance.module.statistics.dto.StatisticsExportQueryDTO;
import com.quyong.attendance.module.statistics.dto.StatisticsSummaryQueryDTO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class StatisticsValidationSupport {

    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public PersonalStatisticsQueryDTO validatePersonal(PersonalStatisticsQueryDTO dto) {
        PersonalStatisticsQueryDTO safe = dto == null ? new PersonalStatisticsQueryDTO() : dto;
        safe.setStartDate(normalize(safe.getStartDate()));
        safe.setEndDate(normalize(safe.getEndDate()));
        return safe;
    }

    public DepartmentStatisticsQueryDTO validateDepartment(DepartmentStatisticsQueryDTO dto) {
        DepartmentStatisticsQueryDTO safe = dto == null ? new DepartmentStatisticsQueryDTO() : dto;
        safe.setStartDate(normalize(safe.getStartDate()));
        safe.setEndDate(normalize(safe.getEndDate()));
        return safe;
    }

    public ExceptionTrendQueryDTO validateTrend(ExceptionTrendQueryDTO dto) {
        ExceptionTrendQueryDTO safe = dto == null ? new ExceptionTrendQueryDTO() : dto;
        safe.setStartDate(normalize(safe.getStartDate()));
        safe.setEndDate(normalize(safe.getEndDate()));
        safe.setPeriodType(resolvePeriodType(safe.getPeriodType()));
        return safe;
    }

    public StatisticsSummaryQueryDTO validateSummary(StatisticsSummaryQueryDTO dto) {
        StatisticsSummaryQueryDTO safe = dto == null ? new StatisticsSummaryQueryDTO() : dto;
        safe.setStartDate(normalize(safe.getStartDate()));
        safe.setEndDate(normalize(safe.getEndDate()));
        safe.setPeriodType(resolvePeriodType(safe.getPeriodType()));
        return safe;
    }

    public DepartmentRiskBriefQueryDTO validateDepartmentRiskBrief(DepartmentRiskBriefQueryDTO dto) {
        DepartmentRiskBriefQueryDTO safe = dto == null ? new DepartmentRiskBriefQueryDTO() : dto;
        if (safe.getDeptId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "部门编号不能为空");
        }
        safe.setStartDate(normalize(safe.getStartDate()));
        safe.setEndDate(normalize(safe.getEndDate()));
        return safe;
    }

    public StatisticsExportQueryDTO validateExport(StatisticsExportQueryDTO dto) {
        StatisticsExportQueryDTO safe = dto == null ? new StatisticsExportQueryDTO() : dto;
        safe.setExportType(resolveExportType(safe.getExportType()));
        safe.setStartDate(normalize(safe.getStartDate()));
        safe.setEndDate(normalize(safe.getEndDate()));
        safe.setPeriodType(resolvePeriodType(safe.getPeriodType()));
        safe.setPageNum(resolvePageNum(safe.getPageNum()));
        safe.setPageSize(resolvePageSize(safe.getPageSize()));
        safe.setType(normalize(safe.getType()));
        return safe;
    }

    public OperationLogQueryDTO validateOperationLogQuery(OperationLogQueryDTO dto) {
        OperationLogQueryDTO safe = dto == null ? new OperationLogQueryDTO() : dto;
        safe.setPageNum(resolvePageNum(safe.getPageNum()));
        safe.setPageSize(resolvePageSize(safe.getPageSize()));
        safe.setType(normalize(safe.getType()));
        safe.setStartDate(normalize(safe.getStartDate()));
        safe.setEndDate(normalize(safe.getEndDate()));
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

    private String resolveExportType(String exportType) {
        String normalizedValue = normalize(exportType);
        if (!StringUtils.hasText(normalizedValue)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "导出类型不能为空");
        }
        if (!"PERSONAL".equals(normalizedValue)
                && !"DEPARTMENT".equals(normalizedValue)
                && !"TREND".equals(normalizedValue)
                && !"AUDIT".equals(normalizedValue)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "导出类型不合法");
        }
        return normalizedValue;
    }

    private String resolvePeriodType(String periodType) {
        String normalizedValue = normalize(periodType);
        if (!StringUtils.hasText(normalizedValue)) {
            return "DAY";
        }
        if (!"DAY".equals(normalizedValue) && !"WEEK".equals(normalizedValue) && !"MONTH".equals(normalizedValue)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "统计周期不合法");
        }
        return normalizedValue;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
