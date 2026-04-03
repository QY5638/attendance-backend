package com.quyong.attendance.module.exceptiondetect.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.attendance.entity.AttendanceRecord;
import com.quyong.attendance.module.attendance.mapper.AttendanceRecordMapper;
import com.quyong.attendance.module.exceptiondetect.dto.ComplexCheckDTO;
import com.quyong.attendance.module.exceptiondetect.dto.RiskFeaturesDTO;
import com.quyong.attendance.module.exceptiondetect.dto.RuleCheckDTO;
import com.quyong.attendance.module.exceptiondetect.entity.AttendanceException;
import com.quyong.attendance.module.exceptiondetect.entity.ExceptionAnalysis;
import com.quyong.attendance.module.exceptiondetect.entity.Rule;
import com.quyong.attendance.module.exceptiondetect.mapper.AttendanceExceptionMapper;
import com.quyong.attendance.module.exceptiondetect.mapper.ExceptionAnalysisMapper;
import com.quyong.attendance.module.exceptiondetect.service.ExceptionAnalysisOrchestrator;
import com.quyong.attendance.module.exceptiondetect.service.RuleService;
import com.quyong.attendance.module.exceptiondetect.support.ExceptionValidationSupport;
import com.quyong.attendance.module.exceptiondetect.vo.ExceptionDecisionVO;
import com.quyong.attendance.module.model.gateway.dto.ModelInvokeRequest;
import com.quyong.attendance.module.model.gateway.dto.ModelInvokeResponse;
import com.quyong.attendance.module.model.gateway.service.ModelGateway;
import com.quyong.attendance.module.model.log.service.ModelCallLogService;
import com.quyong.attendance.module.model.prompt.entity.PromptTemplate;
import com.quyong.attendance.module.model.prompt.mapper.PromptTemplateMapper;
import com.quyong.attendance.module.model.trace.service.DecisionTraceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

@Service
public class ExceptionAnalysisOrchestratorImpl implements ExceptionAnalysisOrchestrator {

    private static final String ATTENDANCE_EXCEPTION_BUSINESS_TYPE = "ATTENDANCE_EXCEPTION";
    private static final String RULE_SOURCE = "RULE";
    private static final String PENDING_STATUS = "PENDING";
    private static final String ABNORMAL_STATUS = "ABNORMAL";

    private final RuleService ruleService;
    private final AttendanceRecordMapper attendanceRecordMapper;
    private final AttendanceExceptionMapper attendanceExceptionMapper;
    private final ExceptionAnalysisMapper exceptionAnalysisMapper;
    private final PromptTemplateMapper promptTemplateMapper;
    private final ModelGateway modelGateway;
    private final ModelCallLogService modelCallLogService;
    private final DecisionTraceService decisionTraceService;
    private final ExceptionValidationSupport exceptionValidationSupport;
    private final Object[] recordLocks;

    public ExceptionAnalysisOrchestratorImpl(RuleService ruleService,
                                             AttendanceRecordMapper attendanceRecordMapper,
                                             AttendanceExceptionMapper attendanceExceptionMapper,
                                             ExceptionAnalysisMapper exceptionAnalysisMapper,
                                             PromptTemplateMapper promptTemplateMapper,
                                             ModelGateway modelGateway,
                                             ModelCallLogService modelCallLogService,
                                             DecisionTraceService decisionTraceService,
                                             ExceptionValidationSupport exceptionValidationSupport) {
        this.ruleService = ruleService;
        this.attendanceRecordMapper = attendanceRecordMapper;
        this.attendanceExceptionMapper = attendanceExceptionMapper;
        this.exceptionAnalysisMapper = exceptionAnalysisMapper;
        this.promptTemplateMapper = promptTemplateMapper;
        this.modelGateway = modelGateway;
        this.modelCallLogService = modelCallLogService;
        this.decisionTraceService = decisionTraceService;
        this.exceptionValidationSupport = exceptionValidationSupport;
        this.recordLocks = initRecordLocks();
    }

    @Override
    @Transactional
    public ExceptionDecisionVO ruleCheck(RuleCheckDTO dto) {
        RuleCheckDTO validatedDTO = exceptionValidationSupport.validateRuleCheck(dto);
        synchronized (getRecordLock(validatedDTO.getRecordId())) {
            AttendanceRecord record = attendanceRecordMapper.selectById(validatedDTO.getRecordId());
            if (record == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "考勤记录不存在");
            }

            Rule rule = ruleService.getEnabledRule();
            String type = detectRuleType(record, rule);
            if (type == null) {
                return null;
            }

            AttendanceException existing = findLatestException(record.getId(), RULE_SOURCE);
            if (existing != null) {
                ensureRuleDecisionTrace(existing);
                return toDecisionVO(existing);
            }

            AttendanceException attendanceException = new AttendanceException();
            attendanceException.setRecordId(record.getId());
            attendanceException.setUserId(record.getUserId());
            attendanceException.setType(type);
            attendanceException.setRiskLevel(resolveRuleRiskLevel(type));
            attendanceException.setSourceType(RULE_SOURCE);
            attendanceException.setDescription(resolveRuleDescription(type));
            attendanceException.setProcessStatus(PENDING_STATUS);
            attendanceExceptionMapper.insert(attendanceException);

            record.setStatus(ABNORMAL_STATUS);
            attendanceRecordMapper.updateById(record);

            decisionTraceService.save(
                    ATTENDANCE_EXCEPTION_BUSINESS_TYPE,
                    attendanceException.getId(),
                    attendanceException.getDescription(),
                    null,
                    attendanceException.getType(),
                    null,
                    attendanceException.getDescription()
            );
            return toDecisionVO(attendanceException);
        }
    }

    @Override
    @Transactional
    public ExceptionDecisionVO complexCheck(ComplexCheckDTO dto) {
        ComplexCheckDTO validatedDTO = exceptionValidationSupport.validateComplexCheck(dto);
        synchronized (getRecordLock(validatedDTO.getRecordId())) {
            AttendanceRecord record = attendanceRecordMapper.selectById(validatedDTO.getRecordId());
            if (record == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "考勤记录不存在");
            }
            if (!record.getUserId().equals(validatedDTO.getUserId())) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "考勤记录用户与请求用户不一致");
            }

            AttendanceException existingModel = findLatestException(record.getId(), "MODEL");
            if (existingModel != null) {
                return toDecisionVO(existingModel, findLatestAnalysis(existingModel.getId()));
            }
            AttendanceException existingFallback = findLatestException(record.getId(), "MODEL_FALLBACK");
            if (existingFallback != null) {
                return toFallbackDecisionVO(existingFallback, findLatestAnalysis(existingFallback.getId()));
            }

            PromptTemplate promptTemplate = promptTemplateMapper.selectEnabledBySceneType("EXCEPTION_ANALYSIS");
            if (promptTemplate == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "未找到启用中的异常分析模板");
            }

            String inputSummary = buildInputSummary(record, validatedDTO.getRiskFeatures());
            long startAt = System.currentTimeMillis();
            ModelInvokeResponse response;
            try {
                response = modelGateway.invoke(buildModelRequest(record, promptTemplate, inputSummary));
            } catch (Exception exception) {
                AttendanceException fallbackException = new AttendanceException();
                fallbackException.setRecordId(record.getId());
                fallbackException.setUserId(record.getUserId());
                fallbackException.setType("COMPLEX_ATTENDANCE_RISK");
                fallbackException.setRiskLevel("MEDIUM");
                fallbackException.setSourceType("MODEL_FALLBACK");
                fallbackException.setDescription("模型调用失败，已转人工处理");
                fallbackException.setProcessStatus(PENDING_STATUS);
                attendanceExceptionMapper.insert(fallbackException);

                ExceptionAnalysis fallbackAnalysis = new ExceptionAnalysis();
                fallbackAnalysis.setExceptionId(fallbackException.getId());
                fallbackAnalysis.setPromptTemplateId(promptTemplate.getId());
                fallbackAnalysis.setInputSummary(inputSummary);
                fallbackAnalysis.setModelConclusion(fallbackException.getType());
                fallbackAnalysis.setDecisionReason(exception.getMessage());
                fallbackAnalysis.setSuggestion("建议管理员查看原始记录并人工确认");
                fallbackAnalysis.setReasonSummary("模型调用失败，已转人工复核");
                fallbackAnalysis.setActionSuggestion("建议管理员查看原始记录并人工确认");
                fallbackAnalysis.setPromptVersion(promptTemplate.getVersion());
                exceptionAnalysisMapper.insert(fallbackAnalysis);

                modelCallLogService.logFailure(
                        "EXCEPTION_ANALYSIS",
                        fallbackException.getId(),
                        promptTemplate.getId(),
                        inputSummary,
                        exception.getMessage(),
                        Integer.valueOf((int) (System.currentTimeMillis() - startAt))
                );

                decisionTraceService.save(
                        ATTENDANCE_EXCEPTION_BUSINESS_TYPE,
                        fallbackException.getId(),
                        buildRuleFeatureSummary(record, validatedDTO.getRiskFeatures()),
                        null,
                        fallbackException.getType(),
                        null,
                        "模型调用失败，已转入保守降级结果"
                );

                record.setStatus(ABNORMAL_STATUS);
                attendanceRecordMapper.updateById(record);
                return toFallbackDecisionVO(fallbackException, fallbackAnalysis);
            }

            AttendanceException attendanceException = new AttendanceException();
            attendanceException.setRecordId(record.getId());
            attendanceException.setUserId(record.getUserId());
            attendanceException.setType(response.getConclusion());
            attendanceException.setRiskLevel(response.getRiskLevel());
            attendanceException.setSourceType("MODEL");
            attendanceException.setDescription(response.getDecisionReason());
            attendanceException.setProcessStatus(PENDING_STATUS);
            attendanceExceptionMapper.insert(attendanceException);

            ExceptionAnalysis analysis = new ExceptionAnalysis();
            analysis.setExceptionId(attendanceException.getId());
            analysis.setPromptTemplateId(promptTemplate.getId());
            analysis.setInputSummary(inputSummary);
            analysis.setModelResult(response.getRawResponse());
            analysis.setModelConclusion(response.getConclusion());
            analysis.setConfidenceScore(response.getConfidenceScore());
            analysis.setDecisionReason(response.getDecisionReason());
            analysis.setSuggestion(response.getActionSuggestion());
            analysis.setReasonSummary(response.getReasonSummary());
            analysis.setActionSuggestion(response.getActionSuggestion());
            analysis.setSimilarCaseSummary(response.getSimilarCaseSummary());
            analysis.setPromptVersion(promptTemplate.getVersion());
            exceptionAnalysisMapper.insert(analysis);

            modelCallLogService.logSuccess(
                    "EXCEPTION_ANALYSIS",
                    attendanceException.getId(),
                    promptTemplate.getId(),
                    inputSummary,
                    response.getRawResponse(),
                    Integer.valueOf((int) (System.currentTimeMillis() - startAt))
            );

            decisionTraceService.save(
                    ATTENDANCE_EXCEPTION_BUSINESS_TYPE,
                    attendanceException.getId(),
                    buildRuleFeatureSummary(record, validatedDTO.getRiskFeatures()),
                    response.getRawResponse(),
                    response.getConclusion(),
                    response.getConfidenceScore(),
                    response.getDecisionReason()
            );

            record.setStatus(ABNORMAL_STATUS);
            attendanceRecordMapper.updateById(record);

            return toDecisionVO(attendanceException, analysis);
        }
    }

    private ExceptionDecisionVO toDecisionVO(AttendanceException attendanceException) {
        ExceptionDecisionVO vo = new ExceptionDecisionVO();
        vo.setExceptionId(attendanceException.getId());
        vo.setType(attendanceException.getType());
        vo.setRiskLevel(attendanceException.getRiskLevel());
        vo.setSourceType(attendanceException.getSourceType());
        vo.setProcessStatus(attendanceException.getProcessStatus());
        return vo;
    }

    private ExceptionDecisionVO toDecisionVO(AttendanceException attendanceException, ModelInvokeResponse response) {
        ExceptionDecisionVO vo = toDecisionVO(attendanceException);
        vo.setModelConclusion(response.getConclusion());
        vo.setReasonSummary(response.getReasonSummary());
        vo.setActionSuggestion(response.getActionSuggestion());
        vo.setConfidenceScore(response.getConfidenceScore());
        return vo;
    }

    private ExceptionDecisionVO toDecisionVO(AttendanceException attendanceException, ExceptionAnalysis analysis) {
        ExceptionDecisionVO vo = toDecisionVO(attendanceException);
        if (analysis != null) {
            vo.setModelConclusion(analysis.getModelConclusion());
            vo.setReasonSummary(analysis.getReasonSummary());
            vo.setActionSuggestion(analysis.getActionSuggestion());
            vo.setConfidenceScore(analysis.getConfidenceScore());
        }
        return vo;
    }

    private String detectRuleType(AttendanceRecord record, Rule rule) {
        if (isIllegalTime(record)) {
            return "ILLEGAL_TIME";
        }
        if (isLate(record, rule)) {
            return "LATE";
        }
        if (isEarlyLeave(record, rule)) {
            return "EARLY_LEAVE";
        }
        if (isRepeatCheck(record, rule)) {
            return "REPEAT_CHECK";
        }
        return null;
    }

    private boolean isIllegalTime(AttendanceRecord record) {
        if (record.getCheckTime() == null) {
            return false;
        }
        LocalTime time = record.getCheckTime().toLocalTime();
        return time.isBefore(LocalTime.of(5, 0)) || time.isAfter(LocalTime.of(23, 0));
    }

    private boolean isLate(AttendanceRecord record, Rule rule) {
        return record.getCheckTime() != null
                && "IN".equals(record.getCheckType())
                && record.getCheckTime().toLocalTime().isAfter(rule.getStartTime().plusMinutes(rule.getLateThreshold().longValue()));
    }

    private boolean isEarlyLeave(AttendanceRecord record, Rule rule) {
        return record.getCheckTime() != null
                && "OUT".equals(record.getCheckType())
                && record.getCheckTime().toLocalTime().isBefore(rule.getEndTime().minusMinutes(rule.getEarlyThreshold().longValue()));
    }

    private boolean isRepeatCheck(AttendanceRecord record, Rule rule) {
        if (record.getCheckTime() == null || !"IN".equals(record.getCheckType())) {
            return false;
        }
        Long count = attendanceRecordMapper.selectCount(Wrappers.<AttendanceRecord>lambdaQuery()
                .eq(AttendanceRecord::getUserId, record.getUserId())
                .eq(AttendanceRecord::getCheckType, record.getCheckType())
                .ge(AttendanceRecord::getCheckTime, record.getCheckTime().minusMinutes(rule.getRepeatLimit().longValue()))
                .lt(AttendanceRecord::getCheckTime, record.getCheckTime()));
        return count != null && count.longValue() > 0L;
    }

    private String resolveRuleRiskLevel(String type) {
        if ("ILLEGAL_TIME".equals(type)) {
            return "HIGH";
        }
        if ("LATE".equals(type) || "EARLY_LEAVE".equals(type)) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String resolveRuleDescription(String type) {
        if ("LATE".equals(type)) {
            return "超过上班时间阈值，判定为迟到";
        }
        if ("EARLY_LEAVE".equals(type)) {
            return "早于下班时间阈值，判定为早退";
        }
        if ("ILLEGAL_TIME".equals(type)) {
            return "非法时间段打卡";
        }
        return "短时间内重复打卡";
    }

    private void ensureRuleDecisionTrace(AttendanceException attendanceException) {
        if (!decisionTraceService.list(ATTENDANCE_EXCEPTION_BUSINESS_TYPE, attendanceException.getId()).isEmpty()) {
            return;
        }
        decisionTraceService.save(
                ATTENDANCE_EXCEPTION_BUSINESS_TYPE,
                attendanceException.getId(),
                attendanceException.getDescription(),
                null,
                attendanceException.getType(),
                null,
            attendanceException.getDescription()
        );
    }

    private String buildInputSummary(AttendanceRecord record, RiskFeaturesDTO riskFeatures) {
        long historyAbnormalCount = attendanceExceptionMapper.selectCount(Wrappers.<AttendanceException>lambdaQuery()
                .eq(AttendanceException::getUserId, record.getUserId()));
        return "recordId=" + record.getId()
                + ", userId=" + record.getUserId()
                + ", checkType=" + record.getCheckType()
                + ", checkTime=" + record.getCheckTime()
                + ", deviceId=" + record.getDeviceId()
                + ", location=" + record.getLocation()
                + ", faceScore=" + record.getFaceScore()
                + ", clientFaceScore=" + (riskFeatures == null ? null : riskFeatures.getFaceScore())
                + ", clientDeviceChanged=" + (riskFeatures == null ? null : riskFeatures.getDeviceChanged())
                + ", clientLocationChanged=" + (riskFeatures == null ? null : riskFeatures.getLocationChanged())
                + ", clientHistoryAbnormalCount=" + (riskFeatures == null ? null : riskFeatures.getHistoryAbnormalCount())
                + ", actualHistoryAbnormalCount=" + historyAbnormalCount;
    }

    private ModelInvokeRequest buildModelRequest(AttendanceRecord record, PromptTemplate promptTemplate, String inputSummary) {
        ModelInvokeRequest request = new ModelInvokeRequest();
        request.setSceneType("EXCEPTION_ANALYSIS");
        request.setBusinessId(record.getId());
        request.setPromptTemplateId(promptTemplate.getId());
        request.setPromptVersion(promptTemplate.getVersion());
        request.setPromptContent(promptTemplate.getContent());
        request.setInputSummary(inputSummary);
        return request;
    }

    private String buildRuleFeatureSummary(AttendanceRecord record, RiskFeaturesDTO riskFeatures) {
        return "deviceId=" + record.getDeviceId()
                + ", location=" + record.getLocation()
                + ", faceScore=" + record.getFaceScore()
                + ", clientDeviceChanged=" + (riskFeatures == null ? null : riskFeatures.getDeviceChanged())
                + ", clientLocationChanged=" + (riskFeatures == null ? null : riskFeatures.getLocationChanged());
    }

    private ExceptionDecisionVO toFallbackDecisionVO(AttendanceException attendanceException, ExceptionAnalysis analysis) {
        ExceptionDecisionVO vo = toDecisionVO(attendanceException);
        if (analysis != null) {
            vo.setModelConclusion(analysis.getModelConclusion());
            vo.setReasonSummary(analysis.getReasonSummary());
            vo.setActionSuggestion(analysis.getActionSuggestion());
            vo.setConfidenceScore(analysis.getConfidenceScore());
        } else {
            vo.setReasonSummary("模型调用失败，已转人工复核");
            vo.setActionSuggestion("建议管理员查看原始记录并人工确认");
        }
        return vo;
    }

    private AttendanceException findLatestException(Long recordId, String sourceType) {
        return attendanceExceptionMapper.selectOne(Wrappers.<AttendanceException>lambdaQuery()
                .eq(AttendanceException::getRecordId, recordId)
                .eq(AttendanceException::getSourceType, sourceType)
                .orderByDesc(AttendanceException::getId)
                .last("LIMIT 1"));
    }

    private ExceptionAnalysis findLatestAnalysis(Long exceptionId) {
        return exceptionAnalysisMapper.selectOne(Wrappers.<ExceptionAnalysis>lambdaQuery()
                .eq(ExceptionAnalysis::getExceptionId, exceptionId)
                .orderByDesc(ExceptionAnalysis::getCreateTime)
                .last("LIMIT 1"));
    }

    private Object[] initRecordLocks() {
        Object[] locks = new Object[64];
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new Object();
        }
        return locks;
    }

    private Object getRecordLock(Long recordId) {
        int index = (recordId.hashCode() & Integer.MAX_VALUE) % recordLocks.length;
        return recordLocks[index];
    }
}
