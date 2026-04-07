package com.quyong.attendance.module.warning.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.auth.model.AuthUser;
import com.quyong.attendance.module.exceptiondetect.entity.AttendanceException;
import com.quyong.attendance.module.exceptiondetect.entity.ExceptionAnalysis;
import com.quyong.attendance.module.exceptiondetect.mapper.AttendanceExceptionMapper;
import com.quyong.attendance.module.exceptiondetect.mapper.ExceptionAnalysisMapper;
import com.quyong.attendance.module.statistics.service.OperationLogService;
import com.quyong.attendance.module.warning.dto.RiskLevelQueryDTO;
import com.quyong.attendance.module.warning.dto.RiskLevelUpdateDTO;
import com.quyong.attendance.module.warning.dto.WarningQueryDTO;
import com.quyong.attendance.module.warning.dto.WarningReevaluateDTO;
import com.quyong.attendance.module.warning.entity.WarningRecord;
import com.quyong.attendance.module.warning.mapper.WarningRecordMapper;
import com.quyong.attendance.module.warning.service.WarningService;
import com.quyong.attendance.module.warning.support.RiskLevelRegistry;
import com.quyong.attendance.module.warning.support.WarningValidationSupport;
import com.quyong.attendance.module.warning.vo.RiskLevelConfigVO;
import com.quyong.attendance.module.warning.vo.WarningAdviceVO;
import com.quyong.attendance.module.warning.vo.WarningVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class WarningServiceImpl implements WarningService {

    private static final String TYPE_RISK_WARNING = "RISK_WARNING";
    private static final String TYPE_ATTENDANCE_WARNING = "ATTENDANCE_WARNING";
    private static final String STATUS_UNPROCESSED = "UNPROCESSED";
    private static final String STATUS_PROCESSED = "PROCESSED";
    private static final String SOURCE_RULE = "RULE";
    private static final String SOURCE_MODEL = "MODEL";
    private static final String SOURCE_MODEL_FALLBACK = "MODEL_FALLBACK";
    private static final String DECISION_SOURCE_RULE = "RULE";
    private static final String DECISION_SOURCE_MODEL_FUSION = "MODEL_FUSION";

    private final WarningRecordMapper warningRecordMapper;
    private final AttendanceExceptionMapper attendanceExceptionMapper;
    private final ExceptionAnalysisMapper exceptionAnalysisMapper;
    private final WarningValidationSupport warningValidationSupport;
    private final RiskLevelRegistry riskLevelRegistry;
    private final OperationLogService operationLogService;

    public WarningServiceImpl(WarningRecordMapper warningRecordMapper,
                              AttendanceExceptionMapper attendanceExceptionMapper,
                              ExceptionAnalysisMapper exceptionAnalysisMapper,
                              WarningValidationSupport warningValidationSupport,
                              RiskLevelRegistry riskLevelRegistry,
                              OperationLogService operationLogService) {
        this.warningRecordMapper = warningRecordMapper;
        this.attendanceExceptionMapper = attendanceExceptionMapper;
        this.exceptionAnalysisMapper = exceptionAnalysisMapper;
        this.warningValidationSupport = warningValidationSupport;
        this.riskLevelRegistry = riskLevelRegistry;
        this.operationLogService = operationLogService;
    }

    @Override
    @Transactional
    public PageResult<WarningVO> list(WarningQueryDTO queryDTO) {
        WarningQueryDTO validatedDTO = warningValidationSupport.validateQuery(queryDTO);
        ensureWarningsGenerated();
        int offset = (validatedDTO.getPageNum().intValue() - 1) * validatedDTO.getPageSize().intValue();
        long total = warningRecordMapper.countByQuery(validatedDTO.getLevel(), validatedDTO.getStatus(), validatedDTO.getType());
        List<WarningRecord> entities = warningRecordMapper.selectPageByQuery(
                validatedDTO.getLevel(),
                validatedDTO.getStatus(),
                validatedDTO.getType(),
                validatedDTO.getPageSize().intValue(),
                offset
        );
        List<WarningVO> records = new ArrayList<WarningVO>();
        for (WarningRecord entity : entities) {
            records.add(toVO(entity));
        }
        return new PageResult<WarningVO>(Long.valueOf(total), records);
    }

    @Override
    public WarningAdviceVO getAdvice(Long id) {
        WarningRecord warningRecord = requireExistingWarning(id);
        WarningAdviceVO vo = new WarningAdviceVO();
        vo.setId(warningRecord.getId());
        vo.setExceptionId(warningRecord.getExceptionId());
        vo.setPriorityScore(warningRecord.getPriorityScore());
        vo.setAiSummary(warningRecord.getAiSummary());
        vo.setDisposeSuggestion(warningRecord.getDisposeSuggestion());
        vo.setDecisionSource(warningRecord.getDecisionSource());
        return vo;
    }

    @Override
    @Transactional
    public WarningVO reEvaluate(WarningReevaluateDTO dto) {
        WarningReevaluateDTO validatedDTO = warningValidationSupport.validateReevaluate(dto);
        AuthUser authUser = currentAuthUser();
        WarningRecord warningRecord = requireExistingWarning(validatedDTO.getWarningId());
        AttendanceException attendanceException = requireExistingException(warningRecord.getExceptionId());
        ExceptionAnalysis analysis = findLatestAnalysis(attendanceException.getId());
        applySnapshot(warningRecord, attendanceException, analysis, warningRecord.getStatus());
        warningRecord.setSendTime(LocalDateTime.now());
        warningRecordMapper.updateById(warningRecord);
        operationLogService.save(authUser.getUserId(), "WARNING", authUser.getRealName() + "重新评估预警" + warningRecord.getId());
        return toVO(warningRecord);
    }

    @Override
    @Transactional
    public void syncWarningByExceptionId(Long exceptionId) {
        AttendanceException attendanceException = requireExistingException(exceptionId);
        if (!shouldCreateWarning(attendanceException)) {
            return;
        }

        ExceptionAnalysis analysis = findLatestAnalysis(exceptionId);
        WarningRecord warningRecord = warningRecordMapper.selectByExceptionId(exceptionId);
        if (warningRecord == null) {
            WarningRecord newWarningRecord = new WarningRecord();
            newWarningRecord.setExceptionId(exceptionId);
            applySnapshot(newWarningRecord, attendanceException, analysis, STATUS_UNPROCESSED);
            newWarningRecord.setSendTime(LocalDateTime.now());
            try {
                warningRecordMapper.insert(newWarningRecord);
                return;
            } catch (DuplicateKeyException exception) {
                warningRecord = warningRecordMapper.selectByExceptionId(exceptionId);
                if (warningRecord == null) {
                    return;
                }
            }
        }

        applySnapshot(warningRecord, attendanceException, analysis, resolveWarningStatus(warningRecord));
        warningRecordMapper.updateById(warningRecord);
    }

    @Override
    @Transactional
    public void markProcessedByExceptionId(Long exceptionId) {
        WarningRecord warningRecord = warningRecordMapper.selectByExceptionId(exceptionId);
        if (warningRecord == null || STATUS_PROCESSED.equals(warningRecord.getStatus())) {
            return;
        }
        warningRecord.setStatus(STATUS_PROCESSED);
        warningRecordMapper.updateById(warningRecord);
    }

    @Override
    public PageResult<RiskLevelConfigVO> listRiskLevels(RiskLevelQueryDTO queryDTO) {
        return riskLevelRegistry.list(warningValidationSupport.validateRiskLevelQuery(queryDTO));
    }

    @Override
    public void updateRiskLevel(RiskLevelUpdateDTO dto) {
        riskLevelRegistry.update(warningValidationSupport.validateRiskLevelUpdate(dto));
    }

    private void ensureWarningsGenerated() {
        // BE-06 只消费异常结果；首次查询时懒生成缺失的预警记录。
        List<AttendanceException> candidates = attendanceExceptionMapper.selectList(Wrappers.<AttendanceException>lambdaQuery()
                .in(AttendanceException::getRiskLevel, "HIGH", "MEDIUM")
                .orderByDesc(AttendanceException::getCreateTime)
                .orderByDesc(AttendanceException::getId));
        for (AttendanceException attendanceException : candidates) {
            syncWarningByExceptionId(attendanceException.getId());
        }
    }

    private boolean shouldCreateWarning(AttendanceException attendanceException) {
        return "HIGH".equals(attendanceException.getRiskLevel()) || "MEDIUM".equals(attendanceException.getRiskLevel());
    }

    private String resolveWarningStatus(WarningRecord warningRecord) {
        if (!StringUtils.hasText(warningRecord.getStatus())) {
            return STATUS_UNPROCESSED;
        }
        return warningRecord.getStatus();
    }

    private void applySnapshot(WarningRecord warningRecord,
                               AttendanceException attendanceException,
                               ExceptionAnalysis analysis,
                               String status) {
        warningRecord.setType(resolveWarningType(attendanceException));
        warningRecord.setLevel(attendanceException.getRiskLevel());
        warningRecord.setStatus(status);
        warningRecord.setPriorityScore(resolvePriorityScore(attendanceException, analysis));
        warningRecord.setAiSummary(resolveAiSummary(attendanceException, analysis));
        warningRecord.setDisposeSuggestion(resolveDisposeSuggestion(attendanceException, analysis));
        warningRecord.setDecisionSource(resolveDecisionSource(attendanceException, analysis));
    }

    private WarningRecord requireExistingWarning(Long id) {
        WarningRecord warningRecord = warningRecordMapper.selectById(id);
        if (warningRecord == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "预警记录不存在");
        }
        return warningRecord;
    }

    private AttendanceException requireExistingException(Long exceptionId) {
        AttendanceException attendanceException = attendanceExceptionMapper.selectById(exceptionId);
        if (attendanceException == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "关联异常不存在");
        }
        return attendanceException;
    }

    private ExceptionAnalysis findLatestAnalysis(Long exceptionId) {
        return exceptionAnalysisMapper.selectOne(Wrappers.<ExceptionAnalysis>lambdaQuery()
                .eq(ExceptionAnalysis::getExceptionId, exceptionId)
                .orderByDesc(ExceptionAnalysis::getCreateTime)
                .orderByDesc(ExceptionAnalysis::getId)
                .last("LIMIT 1"));
    }

    private String resolveWarningType(AttendanceException attendanceException) {
        if (SOURCE_MODEL.equals(attendanceException.getSourceType()) || SOURCE_MODEL_FALLBACK.equals(attendanceException.getSourceType())) {
            return TYPE_RISK_WARNING;
        }
        return TYPE_ATTENDANCE_WARNING;
    }

    private String resolveDecisionSource(AttendanceException attendanceException, ExceptionAnalysis analysis) {
        if (analysis != null || !SOURCE_RULE.equals(attendanceException.getSourceType())) {
            return DECISION_SOURCE_MODEL_FUSION;
        }
        return DECISION_SOURCE_RULE;
    }

    private String resolveAiSummary(AttendanceException attendanceException, ExceptionAnalysis analysis) {
        if (analysis != null && StringUtils.hasText(analysis.getReasonSummary())) {
            return analysis.getReasonSummary();
        }
        RiskLevelConfigVO riskLevelConfig = riskLevelRegistry.get(attendanceException.getRiskLevel());
        String riskLevelName = riskLevelConfig == null ? attendanceException.getRiskLevel() : riskLevelConfig.getName();
        return attendanceException.getDescription() + "（" + riskLevelName + "）";
    }

    private String resolveDisposeSuggestion(AttendanceException attendanceException, ExceptionAnalysis analysis) {
        if (analysis != null && StringUtils.hasText(analysis.getActionSuggestion())) {
            return analysis.getActionSuggestion();
        }
        if (SOURCE_MODEL.equals(attendanceException.getSourceType()) || SOURCE_MODEL_FALLBACK.equals(attendanceException.getSourceType())) {
            return "建议管理员尽快查看异常详情并确认风险";
        }
        return "建议记录本次异常并结合历史记录继续观察";
    }

    private BigDecimal resolvePriorityScore(AttendanceException attendanceException, ExceptionAnalysis analysis) {
        int score = "HIGH".equals(attendanceException.getRiskLevel()) ? 90 : 70;
        if (SOURCE_MODEL.equals(attendanceException.getSourceType()) || SOURCE_MODEL_FALLBACK.equals(attendanceException.getSourceType())) {
            score += 5;
        }
        if (analysis != null && analysis.getConfidenceScore() != null
                && analysis.getConfidenceScore().compareTo(new BigDecimal("90")) >= 0) {
            score += 1;
        }
        return new BigDecimal(Math.min(score, 99)).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private WarningVO toVO(WarningRecord warningRecord) {
        WarningVO vo = new WarningVO();
        AttendanceException attendanceException = attendanceExceptionMapper.selectById(warningRecord.getExceptionId());
        vo.setId(warningRecord.getId());
        vo.setExceptionId(warningRecord.getExceptionId());
        vo.setExceptionType(attendanceException == null ? null : attendanceException.getType());
        vo.setType(warningRecord.getType());
        vo.setLevel(warningRecord.getLevel());
        vo.setStatus(warningRecord.getStatus());
        vo.setPriorityScore(warningRecord.getPriorityScore());
        vo.setAiSummary(warningRecord.getAiSummary());
        vo.setDisposeSuggestion(warningRecord.getDisposeSuggestion());
        vo.setDecisionSource(warningRecord.getDecisionSource());
        vo.setSendTime(warningRecord.getSendTime());
        return vo;
    }

    private AuthUser currentAuthUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
        }
        return (AuthUser) authentication.getPrincipal();
    }
}
