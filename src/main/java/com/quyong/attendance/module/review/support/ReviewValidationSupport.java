package com.quyong.attendance.module.review.support;

import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.review.dto.ExceptionTypeQueryDTO;
import com.quyong.attendance.module.review.dto.ExceptionTypeUpdateDTO;
import com.quyong.attendance.module.review.dto.ReviewFeedbackDTO;
import com.quyong.attendance.module.review.dto.ReviewSubmitDTO;
import org.springframework.stereotype.Component;

@Component
public class ReviewValidationSupport {

    public ReviewSubmitDTO validateSubmit(ReviewSubmitDTO dto) {
        if (dto == null || dto.getExceptionId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "异常记录ID不能为空");
        }
        String reviewResult = normalize(dto.getReviewResult());
        if (!"CONFIRMED".equals(reviewResult) && !"REJECTED".equals(reviewResult)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "复核结果不合法");
        }
        dto.setReviewResult(reviewResult);
        dto.setReviewComment(normalize(dto.getReviewComment()));
        return dto;
    }

    public ReviewFeedbackDTO validateFeedback(ReviewFeedbackDTO dto) {
        if (dto == null || dto.getReviewId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "复核记录ID不能为空");
        }
        String feedbackTag = normalize(dto.getFeedbackTag());
        if (feedbackTag == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "反馈标签不能为空");
        }
        dto.setFeedbackTag(feedbackTag);
        dto.setStrategyFeedback(normalize(dto.getStrategyFeedback()));
        return dto;
    }

    public ExceptionTypeQueryDTO validateExceptionTypeQuery(ExceptionTypeQueryDTO dto) {
        ExceptionTypeQueryDTO safe = dto == null ? new ExceptionTypeQueryDTO() : dto;
        safe.setPageNum(safe.getPageNum() == null || safe.getPageNum().intValue() < 1 ? 1 : safe.getPageNum());
        safe.setPageSize(safe.getPageSize() == null || safe.getPageSize().intValue() < 1 ? 10 : safe.getPageSize());
        if (safe.getStatus() != null && safe.getStatus().intValue() != 0 && safe.getStatus().intValue() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "异常类型状态不合法");
        }
        return safe;
    }

    public ExceptionTypeUpdateDTO validateExceptionTypeUpdate(ExceptionTypeUpdateDTO dto) {
        if (dto == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请求参数错误");
        }
        dto.setCode(normalize(dto.getCode()));
        dto.setName(normalize(dto.getName()));
        dto.setDescription(normalize(dto.getDescription()));
        if (dto.getCode() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "异常类型编码不能为空");
        }
        if (dto.getName() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "异常类型名称不能为空");
        }
        if (dto.getStatus() == null || (dto.getStatus().intValue() != 0 && dto.getStatus().intValue() != 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "异常类型状态不合法");
        }
        return dto;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
