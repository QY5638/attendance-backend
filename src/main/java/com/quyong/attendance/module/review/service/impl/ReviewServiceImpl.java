package com.quyong.attendance.module.review.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.auth.model.AuthUser;
import com.quyong.attendance.module.attendance.entity.AttendanceRecord;
import com.quyong.attendance.module.attendance.mapper.AttendanceRecordMapper;
import com.quyong.attendance.module.exceptiondetect.entity.AttendanceException;
import com.quyong.attendance.module.exceptiondetect.entity.ExceptionAnalysis;
import com.quyong.attendance.module.exceptiondetect.mapper.AttendanceExceptionMapper;
import com.quyong.attendance.module.exceptiondetect.mapper.ExceptionAnalysisMapper;
import com.quyong.attendance.module.model.trace.service.DecisionTraceService;
import com.quyong.attendance.module.model.trace.vo.DecisionTraceVO;
import com.quyong.attendance.module.review.dto.ReviewFeedbackDTO;
import com.quyong.attendance.module.review.dto.ReviewSubmitDTO;
import com.quyong.attendance.module.review.entity.ReviewRecord;
import com.quyong.attendance.module.review.mapper.ReviewRecordMapper;
import com.quyong.attendance.module.review.service.ReviewService;
import com.quyong.attendance.module.review.support.ReviewValidationSupport;
import com.quyong.attendance.module.review.vo.ReviewAssistantVO;
import com.quyong.attendance.module.review.vo.ReviewRecordVO;
import com.quyong.attendance.module.statistics.service.OperationLogService;
import com.quyong.attendance.module.warning.service.WarningService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final String LEGACY_CONFIRMED_EFFECTIVE = "CONFIRMED_EFFECTIVE";
    private static final String ATTENDANCE_EXCEPTION_BUSINESS_TYPE = "ATTENDANCE_EXCEPTION";
    private static final String REVIEW_FEEDBACK = "REVIEW_FEEDBACK";
    private static final String TRUE_POSITIVE = "TRUE_POSITIVE";
    private static final String REVIEWED_STATUS = "REVIEWED";
    private static final String REVIEW_RESULT_CONFIRMED = "CONFIRMED";
    private static final String REVIEW_RESULT_REJECTED = "REJECTED";
    private static final String RECORD_STATUS_NORMAL = "NORMAL";
    private static final String RECORD_STATUS_ABNORMAL = "ABNORMAL";

    private final AttendanceRecordMapper attendanceRecordMapper;
    private final AttendanceExceptionMapper attendanceExceptionMapper;
    private final ExceptionAnalysisMapper exceptionAnalysisMapper;
    private final DecisionTraceService decisionTraceService;
    private final ReviewRecordMapper reviewRecordMapper;
    private final ReviewValidationSupport reviewValidationSupport;
    private final OperationLogService operationLogService;
    private final WarningService warningService;

    public ReviewServiceImpl(AttendanceRecordMapper attendanceRecordMapper,
                             AttendanceExceptionMapper attendanceExceptionMapper,
                             ExceptionAnalysisMapper exceptionAnalysisMapper,
                             DecisionTraceService decisionTraceService,
                             ReviewRecordMapper reviewRecordMapper,
                             ReviewValidationSupport reviewValidationSupport,
                             OperationLogService operationLogService,
                             WarningService warningService) {
        this.attendanceRecordMapper = attendanceRecordMapper;
        this.attendanceExceptionMapper = attendanceExceptionMapper;
        this.exceptionAnalysisMapper = exceptionAnalysisMapper;
        this.decisionTraceService = decisionTraceService;
        this.reviewRecordMapper = reviewRecordMapper;
        this.reviewValidationSupport = reviewValidationSupport;
        this.operationLogService = operationLogService;
        this.warningService = warningService;
    }

    @Override
    public ReviewRecordVO getLatestByExceptionId(Long exceptionId) {
        AttendanceException attendanceException = attendanceExceptionMapper.selectById(exceptionId);
        if (attendanceException == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "异常记录不存在");
        }
        ReviewRecord reviewRecord = reviewRecordMapper.selectOne(Wrappers.<ReviewRecord>lambdaQuery()
                .eq(ReviewRecord::getExceptionId, exceptionId)
                .orderByDesc(ReviewRecord::getReviewTime)
                .orderByDesc(ReviewRecord::getId)
                .last("LIMIT 1"));
        if (reviewRecord == null) {
            return null;
        }
        return toVO(reviewRecord);
    }

    @Override
    public ReviewAssistantVO getAssistant(Long exceptionId) {
        ensureAttendanceExceptionExists(exceptionId);
        List<DecisionTraceVO> traces = decisionTraceService.list("ATTENDANCE_EXCEPTION", exceptionId);
        ExceptionAnalysis analysis = exceptionAnalysisMapper.selectOne(Wrappers.<ExceptionAnalysis>lambdaQuery()
                .eq(ExceptionAnalysis::getExceptionId, exceptionId)
                .orderByDesc(ExceptionAnalysis::getCreateTime)
                .last("LIMIT 1"));
        ReviewAssistantVO vo = new ReviewAssistantVO();
        if (analysis != null) {
            vo.setAiReviewSuggestion(joinSuggestion(analysis.getReasonSummary(), analysis.getActionSuggestion()));
            vo.setSimilarCaseSummary(analysis.getSimilarCaseSummary());
        }
        if (traces.isEmpty()) {
            if (analysis == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "复核辅助信息不存在");
            }
            vo.setDecisionReason(analysis.getDecisionReason());
            vo.setConfidenceScore(analysis.getConfidenceScore());
            return vo;
        }

        DecisionTraceVO trace = findLatestDecisionTrace(traces);
        if (trace == null) {
            if (analysis == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "复核辅助信息不存在");
            }
            vo.setDecisionReason(analysis.getDecisionReason());
            vo.setConfidenceScore(analysis.getConfidenceScore());
            return vo;
        }
        vo.setDecisionReason(trace.getDecisionReason());
        vo.setConfidenceScore(trace.getConfidenceScore());
        return vo;
    }

    @Override
    @Transactional
    public ReviewRecordVO submit(ReviewSubmitDTO dto, Long reviewUserId) {
        ReviewSubmitDTO validatedDTO = reviewValidationSupport.validateSubmit(dto);
        AttendanceException attendanceException = ensureAttendanceExceptionExists(validatedDTO.getExceptionId());
        ReviewAssistantVO assistantVO = getAssistant(validatedDTO.getExceptionId());

        ReviewRecord reviewRecord = new ReviewRecord();
        reviewRecord.setExceptionId(validatedDTO.getExceptionId());
        reviewRecord.setReviewUserId(reviewUserId);
        reviewRecord.setResult(validatedDTO.getReviewResult());
        reviewRecord.setComment(validatedDTO.getReviewComment());
        reviewRecord.setAiReviewSuggestion(assistantVO.getAiReviewSuggestion());
        reviewRecord.setSimilarCaseSummary(assistantVO.getSimilarCaseSummary());
        reviewRecordMapper.insert(reviewRecord);

        attendanceException.setProcessStatus(REVIEWED_STATUS);
        attendanceExceptionMapper.updateById(attendanceException);
        syncAttendanceRecordStatus(attendanceException, validatedDTO.getReviewResult());
        warningService.markProcessedByExceptionId(validatedDTO.getExceptionId());
        warningService.notifyReviewResult(validatedDTO.getExceptionId(), validatedDTO.getReviewResult(), validatedDTO.getReviewComment());
        operationLogService.save(reviewUserId, "REVIEW", currentAuthUser().getRealName() + "复核异常记录" + validatedDTO.getExceptionId());
        return toVO(reviewRecord);
    }

    @Override
    @Transactional
    public void feedback(ReviewFeedbackDTO dto) {
        ReviewFeedbackDTO validatedDTO = reviewValidationSupport.validateFeedback(dto);
        AuthUser authUser = currentAuthUser();
        ReviewRecord reviewRecord = reviewRecordMapper.selectById(validatedDTO.getReviewId());
        if (reviewRecord == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "复核记录不存在");
        }
        reviewRecord.setFeedbackTag(validatedDTO.getFeedbackTag());
        reviewRecord.setStrategyFeedback(validatedDTO.getStrategyFeedback());
        reviewRecordMapper.updateById(reviewRecord);
        decisionTraceService.save(
                ATTENDANCE_EXCEPTION_BUSINESS_TYPE,
                reviewRecord.getExceptionId(),
                buildFeedbackRuleResult(reviewRecord.getId(), validatedDTO.getFeedbackTag()),
                null,
                REVIEW_FEEDBACK,
                null,
                buildFeedbackDecisionReason(validatedDTO.getStrategyFeedback())
        );
        operationLogService.save(authUser.getUserId(), "REVIEW", authUser.getRealName() + "提交复核反馈" + validatedDTO.getReviewId());
    }

    private AttendanceException ensureAttendanceExceptionExists(Long exceptionId) {
        AttendanceException attendanceException = attendanceExceptionMapper.selectById(exceptionId);
        if (attendanceException == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "异常记录不存在");
        }
        return attendanceException;
    }

    private void syncAttendanceRecordStatus(AttendanceException attendanceException, String reviewResult) {
        if (attendanceException == null || attendanceException.getRecordId() == null) {
            return;
        }
        AttendanceRecord attendanceRecord = attendanceRecordMapper.selectById(attendanceException.getRecordId());
        if (attendanceRecord == null) {
            return;
        }
        if (REVIEW_RESULT_REJECTED.equals(reviewResult)) {
            attendanceRecord.setStatus(RECORD_STATUS_NORMAL);
        } else if (REVIEW_RESULT_CONFIRMED.equals(reviewResult)) {
            attendanceRecord.setStatus(RECORD_STATUS_ABNORMAL);
        }
        attendanceRecordMapper.updateById(attendanceRecord);
    }

    private String joinSuggestion(String reasonSummary, String actionSuggestion) {
        if (reasonSummary == null || reasonSummary.trim().isEmpty()) {
            return actionSuggestion;
        }
        if (actionSuggestion == null || actionSuggestion.trim().isEmpty()) {
            return reasonSummary;
        }
        return reasonSummary + "；" + actionSuggestion;
    }

    private String buildFeedbackRuleResult(Long reviewId, String feedbackTag) {
        return "reviewId=" + reviewId + ", feedbackTag=" + feedbackTag;
    }

    private String buildFeedbackDecisionReason(String strategyFeedback) {
        if (!StringUtils.hasText(strategyFeedback)) {
            return "strategyFeedback=未填写";
        }
        return "strategyFeedback=" + strategyFeedback.trim();
    }

    private DecisionTraceVO findLatestDecisionTrace(List<DecisionTraceVO> traces) {
        for (int index = traces.size() - 1; index >= 0; index--) {
            DecisionTraceVO trace = traces.get(index);
            if (!REVIEW_FEEDBACK.equals(trace.getFinalDecision())) {
                return trace;
            }
        }
        return null;
    }

    private ReviewRecordVO toVO(ReviewRecord entity) {
        ReviewRecordVO vo = new ReviewRecordVO();
        vo.setId(entity.getId());
        vo.setExceptionId(entity.getExceptionId());
        vo.setReviewUserId(entity.getReviewUserId());
        vo.setReviewResult(entity.getResult());
        vo.setReviewComment(entity.getComment());
        vo.setAiReviewSuggestion(entity.getAiReviewSuggestion());
        vo.setSimilarCaseSummary(entity.getSimilarCaseSummary());
        vo.setFeedbackTag(normalizeFeedbackTag(entity.getFeedbackTag()));
        vo.setStrategyFeedback(entity.getStrategyFeedback());
        vo.setReviewTime(entity.getReviewTime());
        return vo;
    }

    private String normalizeFeedbackTag(String feedbackTag) {
        if (LEGACY_CONFIRMED_EFFECTIVE.equals(feedbackTag)) {
            return TRUE_POSITIVE;
        }
        return feedbackTag;
    }

    private AuthUser currentAuthUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
        }
        return (AuthUser) authentication.getPrincipal();
    }
}
