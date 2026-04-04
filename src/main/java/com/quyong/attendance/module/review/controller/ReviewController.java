package com.quyong.attendance.module.review.controller;

import com.quyong.attendance.common.api.Result;
import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.auth.model.AuthUser;
import com.quyong.attendance.module.review.dto.ReviewFeedbackDTO;
import com.quyong.attendance.module.review.dto.ReviewSubmitDTO;
import com.quyong.attendance.module.review.service.ReviewService;
import com.quyong.attendance.module.review.vo.ReviewAssistantVO;
import com.quyong.attendance.module.review.vo.ReviewRecordVO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/review")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/{exceptionId}")
    public Result<ReviewRecordVO> getByExceptionId(@PathVariable Long exceptionId) {
        return Result.success(reviewService.getLatestByExceptionId(exceptionId));
    }

    @GetMapping("/{exceptionId}/assistant")
    public Result<ReviewAssistantVO> getAssistant(@PathVariable Long exceptionId) {
        return Result.success(reviewService.getAssistant(exceptionId));
    }

    @PostMapping("/submit")
    public Result<ReviewRecordVO> submit(@RequestBody(required = false) ReviewSubmitDTO dto) {
        return Result.success(reviewService.submit(dto, currentAuthUser().getUserId()));
    }

    @PostMapping("/feedback")
    public Result<Void> feedback(@RequestBody(required = false) ReviewFeedbackDTO dto) {
        reviewService.feedback(dto);
        return Result.success(null);
    }

    private AuthUser currentAuthUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
        }
        return (AuthUser) authentication.getPrincipal();
    }
}
