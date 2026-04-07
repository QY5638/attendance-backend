package com.quyong.attendance.module.attendance.support;

import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.attendance.dto.AttendanceCheckinDTO;
import com.quyong.attendance.module.attendance.dto.AttendanceListQueryDTO;
import com.quyong.attendance.module.attendance.dto.AttendanceRecordQueryDTO;
import com.quyong.attendance.module.attendance.dto.AttendanceRepairDTO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class AttendanceValidationSupport {

    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AttendanceCheckinDTO validateCheckin(AttendanceCheckinDTO dto) {
        AttendanceCheckinDTO target = dto == null ? new AttendanceCheckinDTO() : dto;
        if (target.getUserId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户编号不能为空");
        }
        target.setCheckType(requireCheckType(target.getCheckType()));
        target.setDeviceId(requireText(target.getDeviceId(), "设备编号不能为空"));
        target.setIpAddr(normalize(target.getIpAddr()));
        target.setLocation(normalize(target.getLocation()));
        target.setImageData(requireText(target.getImageData(), "人脸图像不能为空"));
        return target;
    }

    public AttendanceRepairDTO validateRepair(AttendanceRepairDTO dto) {
        AttendanceRepairDTO target = dto == null ? new AttendanceRepairDTO() : dto;
        if (target.getUserId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户编号不能为空");
        }
        target.setCheckType(requireCheckType(target.getCheckType()));
        target.setCheckTime(requireText(target.getCheckTime(), "补卡时间不能为空"));
        target.setRepairReason(requireText(target.getRepairReason(), "补卡原因不能为空"));
        return target;
    }

    public AttendanceRecordQueryDTO validateRecordQuery(AttendanceRecordQueryDTO dto) {
        AttendanceRecordQueryDTO target = dto == null ? new AttendanceRecordQueryDTO() : dto;
        target.setPageNum(resolvePageNum(target.getPageNum()));
        target.setPageSize(resolvePageSize(target.getPageSize()));
        target.setStartDate(normalize(target.getStartDate()));
        target.setEndDate(normalize(target.getEndDate()));
        return target;
    }

    public AttendanceListQueryDTO validateListQuery(AttendanceListQueryDTO dto) {
        AttendanceListQueryDTO target = dto == null ? new AttendanceListQueryDTO() : dto;
        target.setPageNum(resolvePageNum(target.getPageNum()));
        target.setPageSize(resolvePageSize(target.getPageSize()));
        target.setCheckType(normalizeCheckType(target.getCheckType()));
        target.setStatus(normalize(target.getStatus()));
        target.setStartDate(normalize(target.getStartDate()));
        target.setEndDate(normalize(target.getEndDate()));
        return target;
    }

    public LocalDateTime parseDateTime(String value, String fieldName) {
        try {
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), fieldName + "格式不正确");
        }
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

    private String requireCheckType(String checkType) {
        String normalizedCheckType = normalize(checkType);
        if (!StringUtils.hasText(normalizedCheckType)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "打卡类型不能为空");
        }
        if (!"IN".equals(normalizedCheckType) && !"OUT".equals(normalizedCheckType)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "打卡类型不合法");
        }
        return normalizedCheckType;
    }

    private String normalizeCheckType(String checkType) {
        String normalizedCheckType = normalize(checkType);
        if (!StringUtils.hasText(normalizedCheckType)) {
            return null;
        }
        return requireCheckType(normalizedCheckType);
    }

    private Integer resolvePageNum(Integer pageNum) {
        if (pageNum == null || pageNum < 1) {
            return DEFAULT_PAGE_NUM;
        }
        return pageNum;
    }

    private Integer resolvePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private String requireText(String value, String message) {
        String normalizedValue = normalize(value);
        if (!StringUtils.hasText(normalizedValue)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), message);
        }
        return normalizedValue;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
