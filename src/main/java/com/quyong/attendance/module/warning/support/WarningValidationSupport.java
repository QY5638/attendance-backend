package com.quyong.attendance.module.warning.support;

import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.warning.dto.RiskLevelQueryDTO;
import com.quyong.attendance.module.warning.dto.RiskLevelUpdateDTO;
import com.quyong.attendance.module.warning.dto.WarningQueryDTO;
import com.quyong.attendance.module.warning.dto.WarningReplyDTO;
import com.quyong.attendance.module.warning.dto.WarningRequestExplanationDTO;
import com.quyong.attendance.module.warning.dto.WarningReevaluateDTO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WarningValidationSupport {

    public WarningQueryDTO validateQuery(WarningQueryDTO dto) {
        WarningQueryDTO safe = dto == null ? new WarningQueryDTO() : dto;
        safe.setPageNum(safe.getPageNum() == null || safe.getPageNum().intValue() < 1 ? 1 : safe.getPageNum());
        safe.setPageSize(safe.getPageSize() == null || safe.getPageSize().intValue() < 1 ? 10 : safe.getPageSize());
        if (safe.getUserId() != null && safe.getUserId().longValue() <= 0L) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "预警用户不合法");
        }
        safe.setLevel(normalize(safe.getLevel()));
        safe.setStatus(normalize(safe.getStatus()));
        safe.setType(normalize(safe.getType()));
        validateRiskLevel(safe.getLevel(), "预警等级不合法");
        if (safe.getStatus() != null && !"UNPROCESSED".equals(safe.getStatus()) && !"PROCESSED".equals(safe.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "预警状态不合法");
        }
        return safe;
    }

    public WarningReevaluateDTO validateReevaluate(WarningReevaluateDTO dto) {
        if (dto == null || dto.getWarningId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "预警ID不能为空");
        }
        dto.setReason(normalize(dto.getReason()));
        if (!StringUtils.hasText(dto.getReason())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "重评估原因不能为空");
        }
        return dto;
    }

    public WarningRequestExplanationDTO validateRequestExplanation(WarningRequestExplanationDTO dto, Integer defaultDeadlineHours) {
        WarningRequestExplanationDTO safe = dto == null ? new WarningRequestExplanationDTO() : dto;
        safe.setContent(normalize(safe.getContent()));
        if (!StringUtils.hasText(safe.getContent())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "说明请求内容不能为空");
        }
        Integer deadlineHours = safe.getDeadlineHours();
        if (deadlineHours == null) {
            deadlineHours = defaultDeadlineHours;
        }
        if (deadlineHours == null || deadlineHours.intValue() < 1 || deadlineHours.intValue() > 168) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "说明回复时限不合法");
        }
        safe.setDeadlineHours(deadlineHours);
        return safe;
    }

    public WarningReplyDTO validateReply(WarningReplyDTO dto) {
        WarningReplyDTO safe = dto == null ? new WarningReplyDTO() : dto;
        safe.setContent(normalize(safe.getContent()));
        if (!StringUtils.hasText(safe.getContent())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "回复内容不能为空");
        }
        return safe;
    }

    public RiskLevelQueryDTO validateRiskLevelQuery(RiskLevelQueryDTO dto) {
        RiskLevelQueryDTO safe = dto == null ? new RiskLevelQueryDTO() : dto;
        safe.setPageNum(safe.getPageNum() == null || safe.getPageNum().intValue() < 1 ? 1 : safe.getPageNum());
        safe.setPageSize(safe.getPageSize() == null || safe.getPageSize().intValue() < 1 ? 10 : safe.getPageSize());
        if (safe.getStatus() != null && safe.getStatus().intValue() != 0 && safe.getStatus().intValue() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "风险等级状态不合法");
        }
        return safe;
    }

    public RiskLevelUpdateDTO validateRiskLevelUpdate(RiskLevelUpdateDTO dto) {
        if (dto == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请求参数错误");
        }
        dto.setCode(normalize(dto.getCode()));
        dto.setName(normalize(dto.getName()));
        dto.setDescription(normalize(dto.getDescription()));
        validateRiskLevel(dto.getCode(), "风险等级编码不合法");
        if (!StringUtils.hasText(dto.getName())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "风险等级名称不能为空");
        }
        if (!StringUtils.hasText(dto.getDescription())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "风险等级描述不能为空");
        }
        if (dto.getStatus() == null || (dto.getStatus().intValue() != 0 && dto.getStatus().intValue() != 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "风险等级状态不合法");
        }
        return dto;
    }

    private void validateRiskLevel(String value, String message) {
        if (value == null) {
            return;
        }
        if (!"HIGH".equals(value) && !"MEDIUM".equals(value) && !"LOW".equals(value)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), message);
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
