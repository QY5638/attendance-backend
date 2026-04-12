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
import com.quyong.attendance.module.map.config.MapProperties;
import com.quyong.attendance.module.map.service.MapDistanceService;
import com.quyong.attendance.module.model.gateway.config.ModelGatewayProperties;
import com.quyong.attendance.module.model.gateway.dto.ModelInvokeRequest;
import com.quyong.attendance.module.model.gateway.dto.ModelInvokeResponse;
import com.quyong.attendance.module.model.gateway.service.ModelGateway;
import com.quyong.attendance.module.model.log.service.ModelCallLogService;
import com.quyong.attendance.module.model.prompt.entity.PromptTemplate;
import com.quyong.attendance.module.model.prompt.mapper.PromptTemplateMapper;
import com.quyong.attendance.module.model.trace.service.DecisionTraceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalTime;

@Service
public class ExceptionAnalysisOrchestratorImpl implements ExceptionAnalysisOrchestrator {

    private static final String ATTENDANCE_EXCEPTION_BUSINESS_TYPE = "ATTENDANCE_EXCEPTION";
    private static final String RULE_SOURCE = "RULE";
    private static final String PENDING_STATUS = "PENDING";
    private static final String ABNORMAL_STATUS = "ABNORMAL";
    private static final String MULTI_LOCATION_CONFLICT = "MULTI_LOCATION_CONFLICT";
    private static final String CONTINUOUS_MULTI_LOCATION_CONFLICT = "CONTINUOUS_MULTI_LOCATION_CONFLICT";
    private static final String CONTINUOUS_ILLEGAL_TIME = "CONTINUOUS_ILLEGAL_TIME";
    private static final String CONTINUOUS_REPEAT_CHECK = "CONTINUOUS_REPEAT_CHECK";
    private static final String CONTINUOUS_PROXY_CHECKIN = "CONTINUOUS_PROXY_CHECKIN";
    private static final String CONTINUOUS_ATTENDANCE_RISK = "CONTINUOUS_ATTENDANCE_RISK";
    private static final String CONTINUOUS_MODEL_RISK = "CONTINUOUS_MODEL_RISK";
    private static final String COMPLEX_ATTENDANCE_RISK = "COMPLEX_ATTENDANCE_RISK";
    private static final String CONTINUOUS_LATE = "CONTINUOUS_LATE";
    private static final String CONTINUOUS_EARLY_LEAVE = "CONTINUOUS_EARLY_LEAVE";
    private static final String HISTORICAL_MULTI_LOCATION_TRACE_NOTE = "历史版本未持久化完整空间证据，当前为避免基于变更后的记录/阈值重建失真证据，不做明细回填";
    private static final int CONTINUOUS_LATE_RECORD_COUNT = 3;
    private static final int CONTINUOUS_LATE_WINDOW_DAYS = 7;
    private static final int CONTINUOUS_MULTI_LOCATION_RECORD_COUNT = 3;

    private final RuleService ruleService;
    private final AttendanceRecordMapper attendanceRecordMapper;
    private final AttendanceExceptionMapper attendanceExceptionMapper;
    private final ExceptionAnalysisMapper exceptionAnalysisMapper;
    private final PromptTemplateMapper promptTemplateMapper;
    private final ModelGateway modelGateway;
    private final ModelCallLogService modelCallLogService;
    private final DecisionTraceService decisionTraceService;
    private final ExceptionValidationSupport exceptionValidationSupport;
    private final MapDistanceService mapDistanceService;
    private final MapProperties mapProperties;
    private final ModelGatewayProperties modelGatewayProperties;
    private final Object[] recordLocks;

    public ExceptionAnalysisOrchestratorImpl(RuleService ruleService,
                                             AttendanceRecordMapper attendanceRecordMapper,
                                             AttendanceExceptionMapper attendanceExceptionMapper,
                                             ExceptionAnalysisMapper exceptionAnalysisMapper,
                                             PromptTemplateMapper promptTemplateMapper,
                                             ModelGateway modelGateway,
                                             ModelCallLogService modelCallLogService,
                                             DecisionTraceService decisionTraceService,
                                             ExceptionValidationSupport exceptionValidationSupport,
                                             MapDistanceService mapDistanceService,
                                             MapProperties mapProperties,
                                             ModelGatewayProperties modelGatewayProperties) {
        this.ruleService = ruleService;
        this.attendanceRecordMapper = attendanceRecordMapper;
        this.attendanceExceptionMapper = attendanceExceptionMapper;
        this.exceptionAnalysisMapper = exceptionAnalysisMapper;
        this.promptTemplateMapper = promptTemplateMapper;
        this.modelGateway = modelGateway;
        this.modelCallLogService = modelCallLogService;
        this.decisionTraceService = decisionTraceService;
        this.exceptionValidationSupport = exceptionValidationSupport;
        this.mapDistanceService = mapDistanceService;
        this.mapProperties = mapProperties;
        this.modelGatewayProperties = modelGatewayProperties;
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
            String description = resolveRuleDescription(type);
            String ruleDecisionEvidence = resolveRuleDecisionEvidence(record, rule, type, description);
            attendanceException.setDescription(description);
            attendanceException.setProcessStatus(PENDING_STATUS);
            attendanceExceptionMapper.insert(attendanceException);

            record.setStatus(ABNORMAL_STATUS);
            attendanceRecordMapper.updateById(record);

            decisionTraceService.save(
                    ATTENDANCE_EXCEPTION_BUSINESS_TYPE,
                    attendanceException.getId(),
                    ruleDecisionEvidence,
                    null,
                    attendanceException.getType(),
                    null,
                    ruleDecisionEvidence
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
                return null;
            }

            String inputSummary = enrichPromptContext(
                    buildInputSummary(record, validatedDTO.getRiskFeatures()),
                    promptTemplate
            );
            long startAt = System.currentTimeMillis();
            ModelInvokeResponse response;
            try {
                response = modelGateway.invoke(buildModelRequest(record, promptTemplate, inputSummary));
                response = applyContinuousProxyEscalation(record, response);
                response = applyContinuousModelRiskEscalation(record, response);
            } catch (Exception exception) {
                AttendanceException fallbackException = new AttendanceException();
                fallbackException.setRecordId(record.getId());
                fallbackException.setUserId(record.getUserId());
                fallbackException.setType(COMPLEX_ATTENDANCE_RISK);
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
                        enrichLogInputSummary(inputSummary),
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
            String normalizedExceptionType = normalizeModelExceptionType(response.getConclusion());
            attendanceException.setRecordId(record.getId());
            attendanceException.setUserId(record.getUserId());
            attendanceException.setType(normalizedExceptionType);
            attendanceException.setRiskLevel(limitText(response.getRiskLevel(), 20));
            attendanceException.setSourceType("MODEL");
            attendanceException.setDescription(limitText(response.getDecisionReason(), 255));
            attendanceException.setProcessStatus(PENDING_STATUS);
            attendanceExceptionMapper.insert(attendanceException);

            ExceptionAnalysis analysis = new ExceptionAnalysis();
            analysis.setExceptionId(attendanceException.getId());
            analysis.setPromptTemplateId(promptTemplate.getId());
            analysis.setInputSummary(inputSummary);
            analysis.setModelResult(response.getRawResponse());
            analysis.setModelConclusion(limitText(response.getConclusion(), 100));
            analysis.setConfidenceScore(response.getConfidenceScore());
            analysis.setDecisionReason(response.getDecisionReason());
            analysis.setSuggestion(limitText(response.getActionSuggestion(), 255));
            analysis.setReasonSummary(response.getReasonSummary());
            analysis.setActionSuggestion(limitText(response.getActionSuggestion(), 255));
            analysis.setSimilarCaseSummary(response.getSimilarCaseSummary());
            analysis.setPromptVersion(limitText(promptTemplate.getVersion(), 50));
            exceptionAnalysisMapper.insert(analysis);

            modelCallLogService.logSuccess(
                    "EXCEPTION_ANALYSIS",
                    attendanceException.getId(),
                    promptTemplate.getId(),
                    enrichLogInputSummary(inputSummary),
                    response.getRawResponse(),
                    Integer.valueOf((int) (System.currentTimeMillis() - startAt))
            );

            decisionTraceService.save(
                    ATTENDANCE_EXCEPTION_BUSINESS_TYPE,
                    attendanceException.getId(),
                    buildRuleFeatureSummary(record, validatedDTO.getRiskFeatures()),
                    response.getRawResponse(),
                    normalizedExceptionType,
                    response.getConfidenceScore(),
                    response.getDecisionReason()
            );

            record.setStatus(ABNORMAL_STATUS);
            attendanceRecordMapper.updateById(record);

            return toDecisionVO(attendanceException, analysis);
        }
    }

    private ModelInvokeResponse applyContinuousProxyEscalation(AttendanceRecord record, ModelInvokeResponse response) {
        if (record == null || response == null || record.getUserId() == null || record.getCheckTime() == null) {
            return response;
        }
        if (!"PROXY_CHECKIN".equals(limitText(response.getConclusion(), 50))) {
            return response;
        }

        Long count = attendanceExceptionMapper.selectCount(Wrappers.<AttendanceException>lambdaQuery()
                .eq(AttendanceException::getUserId, record.getUserId())
                .in(AttendanceException::getSourceType, "MODEL", "MODEL_FALLBACK")
                .in(AttendanceException::getType, "PROXY_CHECKIN", CONTINUOUS_PROXY_CHECKIN)
                .ge(AttendanceException::getCreateTime, record.getCheckTime().minusDays(CONTINUOUS_LATE_WINDOW_DAYS))
                .lt(AttendanceException::getCreateTime, record.getCheckTime()));
        if (count == null || count.longValue() < CONTINUOUS_LATE_RECORD_COUNT - 1L) {
            return response;
        }

        response.setConclusion(CONTINUOUS_PROXY_CHECKIN);
        response.setRiskLevel("HIGH");
        response.setReasonSummary("最近7天内连续3次疑似代打卡，已升级为持续性高风险行为");
        response.setDecisionReason("模型识别本次疑似代打卡，且历史同类模型异常在最近7天内已连续出现");
        response.setActionSuggestion("建议优先核查人脸、设备、地点和终端信息，并尽快人工复核");
        return response;
    }

    private ModelInvokeResponse applyContinuousModelRiskEscalation(AttendanceRecord record, ModelInvokeResponse response) {
        if (record == null || response == null || record.getUserId() == null || record.getCheckTime() == null) {
            return response;
        }
        String conclusion = limitText(response.getConclusion(), 50);
        if (!StringUtils.hasText(conclusion) || CONTINUOUS_PROXY_CHECKIN.equals(conclusion) || CONTINUOUS_MODEL_RISK.equals(conclusion)) {
            return response;
        }

        Long count = attendanceExceptionMapper.selectCount(Wrappers.<AttendanceException>lambdaQuery()
                .eq(AttendanceException::getUserId, record.getUserId())
                .in(AttendanceException::getSourceType, "MODEL", "MODEL_FALLBACK")
                .ge(AttendanceException::getCreateTime, record.getCheckTime().minusDays(CONTINUOUS_LATE_WINDOW_DAYS))
                .lt(AttendanceException::getCreateTime, record.getCheckTime()));
        if (count == null || count.longValue() < CONTINUOUS_LATE_RECORD_COUNT - 1L) {
            return response;
        }

        response.setConclusion(CONTINUOUS_MODEL_RISK);
        response.setRiskLevel("HIGH");
        response.setReasonSummary("最近7天内连续3次出现模型侧异常判断，已升级为持续性模型风险行为");
        response.setDecisionReason("模型识别本次异常，且历史模型异常在最近7天内已连续出现");
        response.setActionSuggestion("建议优先查看模型证据链、风险人员档案并安排人工复核");
        return response;
    }

    private String normalizeModelExceptionType(String conclusion) {
        String normalized = limitText(conclusion, 50);
        if (!StringUtils.hasText(normalized)) {
            return COMPLEX_ATTENDANCE_RISK;
        }
        if ("PROXY_CHECKIN".equals(normalized)
                || CONTINUOUS_PROXY_CHECKIN.equals(normalized)
                || CONTINUOUS_MODEL_RISK.equals(normalized)
                || CONTINUOUS_ATTENDANCE_RISK.equals(normalized)
                || CONTINUOUS_LATE.equals(normalized)
                || CONTINUOUS_EARLY_LEAVE.equals(normalized)
                || CONTINUOUS_MULTI_LOCATION_CONFLICT.equals(normalized)
                || CONTINUOUS_ILLEGAL_TIME.equals(normalized)
                || CONTINUOUS_REPEAT_CHECK.equals(normalized)
                || MULTI_LOCATION_CONFLICT.equals(normalized)
                || "LATE".equals(normalized)
                || "EARLY_LEAVE".equals(normalized)
                || "ILLEGAL_TIME".equals(normalized)
                || "REPEAT_CHECK".equals(normalized)) {
            return normalized;
        }
        return COMPLEX_ATTENDANCE_RISK;
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
        if (isContinuousIllegalTimePattern(record)) {
            return CONTINUOUS_ILLEGAL_TIME;
        }
        if (isIllegalTime(record)) {
            return "ILLEGAL_TIME";
        }
        if (isContinuousMultiLocationPattern(record)) {
            return CONTINUOUS_MULTI_LOCATION_CONFLICT;
        }
        if (isMultiLocationConflict(record)) {
            return MULTI_LOCATION_CONFLICT;
        }
        if (isContinuousLatePattern(record, rule)) {
            return CONTINUOUS_LATE;
        }
        if (isContinuousEarlyLeavePattern(record, rule)) {
            return CONTINUOUS_EARLY_LEAVE;
        }
        if (isContinuousRepeatCheckPattern(record, rule)) {
            return CONTINUOUS_REPEAT_CHECK;
        }
        if (isContinuousAttendanceRiskPattern(record, rule)) {
            return CONTINUOUS_ATTENDANCE_RISK;
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

    private boolean isContinuousIllegalTimePattern(AttendanceRecord record) {
        if (!isIllegalTime(record) || record.getUserId() == null || record.getCheckTime() == null) {
            return false;
        }

        java.util.List<AttendanceRecord> previousRecords = attendanceRecordMapper.selectList(Wrappers.<AttendanceRecord>lambdaQuery()
                .eq(AttendanceRecord::getUserId, record.getUserId())
                .ge(AttendanceRecord::getCheckTime, record.getCheckTime().minusDays(CONTINUOUS_LATE_WINDOW_DAYS))
                .lt(AttendanceRecord::getCheckTime, record.getCheckTime())
                .orderByDesc(AttendanceRecord::getCheckTime)
                .orderByDesc(AttendanceRecord::getId)
                .last("LIMIT " + (CONTINUOUS_LATE_RECORD_COUNT - 1)));

        if (previousRecords == null || previousRecords.size() < CONTINUOUS_LATE_RECORD_COUNT - 1) {
            return false;
        }

        for (AttendanceRecord previousRecord : previousRecords) {
            if (!isIllegalTime(previousRecord)) {
                return false;
            }
        }
        return true;
    }

    private boolean isLate(AttendanceRecord record, Rule rule) {
        return record.getCheckTime() != null
                && "IN".equals(record.getCheckType())
                && record.getCheckTime().toLocalTime().isAfter(rule.getStartTime().plusMinutes(rule.getLateThreshold().longValue()));
    }

    private boolean isContinuousLatePattern(AttendanceRecord record, Rule rule) {
        if (!isLate(record, rule) || record.getUserId() == null || record.getCheckTime() == null) {
            return false;
        }

        java.util.List<AttendanceRecord> previousInRecords = attendanceRecordMapper.selectList(Wrappers.<AttendanceRecord>lambdaQuery()
                .eq(AttendanceRecord::getUserId, record.getUserId())
                .eq(AttendanceRecord::getCheckType, "IN")
                .ge(AttendanceRecord::getCheckTime, record.getCheckTime().minusDays(CONTINUOUS_LATE_WINDOW_DAYS))
                .lt(AttendanceRecord::getCheckTime, record.getCheckTime())
                .orderByDesc(AttendanceRecord::getCheckTime)
                .orderByDesc(AttendanceRecord::getId)
                .last("LIMIT " + (CONTINUOUS_LATE_RECORD_COUNT - 1)));

        if (previousInRecords == null || previousInRecords.size() < CONTINUOUS_LATE_RECORD_COUNT - 1) {
            return false;
        }

        for (AttendanceRecord previousRecord : previousInRecords) {
            if (!isLate(previousRecord, rule)) {
                return false;
            }
        }
        return true;
    }

    private boolean isEarlyLeave(AttendanceRecord record, Rule rule) {
        return record.getCheckTime() != null
                && "OUT".equals(record.getCheckType())
                && record.getCheckTime().toLocalTime().isBefore(rule.getEndTime().minusMinutes(rule.getEarlyThreshold().longValue()));
    }

    private boolean isContinuousEarlyLeavePattern(AttendanceRecord record, Rule rule) {
        if (!isEarlyLeave(record, rule) || record.getUserId() == null || record.getCheckTime() == null) {
            return false;
        }

        java.util.List<AttendanceRecord> previousOutRecords = attendanceRecordMapper.selectList(Wrappers.<AttendanceRecord>lambdaQuery()
                .eq(AttendanceRecord::getUserId, record.getUserId())
                .eq(AttendanceRecord::getCheckType, "OUT")
                .ge(AttendanceRecord::getCheckTime, record.getCheckTime().minusDays(CONTINUOUS_LATE_WINDOW_DAYS))
                .lt(AttendanceRecord::getCheckTime, record.getCheckTime())
                .orderByDesc(AttendanceRecord::getCheckTime)
                .orderByDesc(AttendanceRecord::getId)
                .last("LIMIT " + (CONTINUOUS_LATE_RECORD_COUNT - 1)));

        if (previousOutRecords == null || previousOutRecords.size() < CONTINUOUS_LATE_RECORD_COUNT - 1) {
            return false;
        }

        for (AttendanceRecord previousRecord : previousOutRecords) {
            if (!isEarlyLeave(previousRecord, rule)) {
                return false;
            }
        }
        return true;
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

    private boolean isContinuousRepeatCheckPattern(AttendanceRecord record, Rule rule) {
        if (!isRepeatCheck(record, rule) || record.getUserId() == null || record.getCheckTime() == null) {
            return false;
        }

        Long count = attendanceExceptionMapper.selectCount(Wrappers.<AttendanceException>lambdaQuery()
                .eq(AttendanceException::getUserId, record.getUserId())
                .eq(AttendanceException::getSourceType, RULE_SOURCE)
                .in(AttendanceException::getType, "REPEAT_CHECK", CONTINUOUS_REPEAT_CHECK)
                .ge(AttendanceException::getCreateTime, record.getCheckTime().minusDays(CONTINUOUS_LATE_WINDOW_DAYS))
                .lt(AttendanceException::getCreateTime, record.getCheckTime()));
        return count != null && count.longValue() >= CONTINUOUS_LATE_RECORD_COUNT - 1L;
    }

    private boolean isContinuousAttendanceRiskPattern(AttendanceRecord record, Rule rule) {
        if (record == null || record.getUserId() == null || record.getCheckTime() == null) {
            return false;
        }
        String currentBaseType = resolveCurrentBaseRuleType(record, rule);
        if (!StringUtils.hasText(currentBaseType)) {
            return false;
        }

        Long count = attendanceExceptionMapper.selectCount(Wrappers.<AttendanceException>lambdaQuery()
                .eq(AttendanceException::getUserId, record.getUserId())
                .eq(AttendanceException::getSourceType, RULE_SOURCE)
                .ge(AttendanceException::getCreateTime, record.getCheckTime().minusDays(CONTINUOUS_LATE_WINDOW_DAYS))
                .lt(AttendanceException::getCreateTime, record.getCheckTime()));
        return count != null && count.longValue() >= CONTINUOUS_LATE_RECORD_COUNT - 1L;
    }

    private String resolveCurrentBaseRuleType(AttendanceRecord record, Rule rule) {
        if (isIllegalTime(record)) {
            return "ILLEGAL_TIME";
        }
        if (isMultiLocationConflict(record)) {
            return MULTI_LOCATION_CONFLICT;
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

    private boolean isMultiLocationConflict(AttendanceRecord record) {
        if (record == null || record.getUserId() == null || record.getCheckTime() == null) {
            return false;
        }
        if (record.getLongitude() == null || record.getLatitude() == null) {
            return false;
        }
        if (mapProperties.getMultiLocationWindowMinutes() == null || mapProperties.getMultiLocationWindowMinutes().intValue() <= 0) {
            return false;
        }
        if (mapProperties.getMultiLocationDistanceMeters() == null
                || mapProperties.getMultiLocationDistanceMeters().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        AttendanceRecord previousRecord = attendanceRecordMapper.selectLatestBefore(record.getUserId(), record.getCheckTime(), record.getId());
        if (previousRecord == null || previousRecord.getCheckTime() == null) {
            return false;
        }
        if (previousRecord.getLongitude() == null || previousRecord.getLatitude() == null) {
            return false;
        }

        long intervalMinutes = Duration.between(previousRecord.getCheckTime(), record.getCheckTime()).toMinutes();
        if (intervalMinutes < 0 || intervalMinutes > mapProperties.getMultiLocationWindowMinutes().intValue()) {
            return false;
        }

        BigDecimal distanceMeters = mapDistanceService.calculateDistanceMeters(
                previousRecord.getLongitude(),
                previousRecord.getLatitude(),
                record.getLongitude(),
                record.getLatitude()
        );
        return distanceMeters.compareTo(mapProperties.getMultiLocationDistanceMeters()) >= 0;
    }

    private boolean isContinuousMultiLocationPattern(AttendanceRecord record) {
        if (!isMultiLocationConflict(record) || record.getUserId() == null || record.getCheckTime() == null) {
            return false;
        }

        Long count = attendanceExceptionMapper.selectCount(Wrappers.<AttendanceException>lambdaQuery()
                .eq(AttendanceException::getUserId, record.getUserId())
                .eq(AttendanceException::getSourceType, RULE_SOURCE)
                .in(AttendanceException::getType, MULTI_LOCATION_CONFLICT, CONTINUOUS_MULTI_LOCATION_CONFLICT)
                .ge(AttendanceException::getCreateTime, record.getCheckTime().minusDays(CONTINUOUS_LATE_WINDOW_DAYS))
                .lt(AttendanceException::getCreateTime, record.getCheckTime()));
        return count != null && count.longValue() >= CONTINUOUS_MULTI_LOCATION_RECORD_COUNT - 1L;
    }

    private String resolveRuleRiskLevel(String type) {
        if (MULTI_LOCATION_CONFLICT.equals(type)) {
            return "HIGH";
        }
        if (CONTINUOUS_MULTI_LOCATION_CONFLICT.equals(type)) {
            return "HIGH";
        }
        if (CONTINUOUS_ILLEGAL_TIME.equals(type)) {
            return "HIGH";
        }
        if (CONTINUOUS_REPEAT_CHECK.equals(type)) {
            return "HIGH";
        }
        if (CONTINUOUS_ATTENDANCE_RISK.equals(type)) {
            return "HIGH";
        }
        if (CONTINUOUS_LATE.equals(type)) {
            return "HIGH";
        }
        if (CONTINUOUS_EARLY_LEAVE.equals(type)) {
            return "HIGH";
        }
        if ("ILLEGAL_TIME".equals(type)) {
            return "HIGH";
        }
        if ("LATE".equals(type) || "EARLY_LEAVE".equals(type)) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String resolveRuleDescription(String type) {
        if (MULTI_LOCATION_CONFLICT.equals(type)) {
            return "短时间内在多个地点完成打卡，判定为空间冲突";
        }
        if (CONTINUOUS_MULTI_LOCATION_CONFLICT.equals(type)) {
            return "最近7天内连续3次出现多地点冲突，判定为连续多地点冲突模式异常";
        }
        if (CONTINUOUS_ILLEGAL_TIME.equals(type)) {
            return "最近7天内连续3次发生非法时间打卡，判定为连续非法时间打卡模式异常";
        }
        if (CONTINUOUS_REPEAT_CHECK.equals(type)) {
            return "最近7天内连续3次出现短时间重复打卡，判定为连续重复打卡模式异常";
        }
        if (CONTINUOUS_ATTENDANCE_RISK.equals(type)) {
            return "最近7天内连续3次出现规则异常，判定为持续考勤风险模式异常";
        }
        if (CONTINUOUS_LATE.equals(type)) {
            return "最近7天内连续3次上班打卡迟到，判定为连续迟到模式异常";
        }
        if (CONTINUOUS_EARLY_LEAVE.equals(type)) {
            return "最近7天内连续3次下班打卡早退，判定为连续早退模式异常";
        }
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

    private String resolveRuleDecisionEvidence(AttendanceRecord record, Rule rule, String type, String description) {
        if (CONTINUOUS_LATE.equals(type)) {
            String evidence = buildContinuousLateEvidence(record);
            if (evidence != null) {
                return evidence;
            }
            return description;
        }
        if (CONTINUOUS_ILLEGAL_TIME.equals(type)) {
            String evidence = buildContinuousIllegalTimeEvidence(record);
            if (evidence != null) {
                return evidence;
            }
            return description;
        }
        if (CONTINUOUS_REPEAT_CHECK.equals(type)) {
            String evidence = buildContinuousRepeatCheckEvidence(record);
            if (evidence != null) {
                return evidence;
            }
            return description;
        }
        if (CONTINUOUS_ATTENDANCE_RISK.equals(type)) {
            String evidence = buildContinuousAttendanceRiskEvidence(record, rule);
            if (evidence != null) {
                return evidence;
            }
            return description;
        }
        if (CONTINUOUS_MULTI_LOCATION_CONFLICT.equals(type)) {
            String evidence = buildContinuousMultiLocationEvidence(record);
            if (evidence != null) {
                return evidence;
            }
            return description;
        }
        if (CONTINUOUS_EARLY_LEAVE.equals(type)) {
            String evidence = buildContinuousEarlyLeaveEvidence(record);
            if (evidence != null) {
                return evidence;
            }
            return description;
        }
        if (!MULTI_LOCATION_CONFLICT.equals(type)) {
            return description;
        }
        String evidence = buildMultiLocationConflictEvidence(record);
        if (evidence != null) {
            return evidence;
        }
        return description;
    }

    private String buildContinuousLateEvidence(AttendanceRecord record) {
        if (record == null || record.getUserId() == null || record.getCheckTime() == null) {
            return null;
        }

        java.util.List<AttendanceRecord> recentLateRecords = attendanceRecordMapper.selectList(Wrappers.<AttendanceRecord>lambdaQuery()
                .eq(AttendanceRecord::getUserId, record.getUserId())
                .eq(AttendanceRecord::getCheckType, "IN")
                .ge(AttendanceRecord::getCheckTime, record.getCheckTime().minusDays(CONTINUOUS_LATE_WINDOW_DAYS))
                .le(AttendanceRecord::getCheckTime, record.getCheckTime())
                .orderByDesc(AttendanceRecord::getCheckTime)
                .orderByDesc(AttendanceRecord::getId)
                .last("LIMIT " + CONTINUOUS_LATE_RECORD_COUNT));

        if (recentLateRecords == null || recentLateRecords.size() < CONTINUOUS_LATE_RECORD_COUNT) {
            return null;
        }

        StringBuilder builder = new StringBuilder("最近7天内连续3次上班打卡迟到，判定为连续迟到模式异常；迟到记录=");
        for (int index = recentLateRecords.size() - 1; index >= 0; index--) {
            AttendanceRecord lateRecord = recentLateRecords.get(index);
            builder.append('[')
                    .append(lateRecord.getCheckTime())
                    .append(" @ ")
                    .append(resolveLocationLabel(lateRecord))
                    .append(']');
            if (index > 0) {
                builder.append(" -> ");
            }
        }
        return builder.toString();
    }

    private String buildContinuousIllegalTimeEvidence(AttendanceRecord record) {
        if (record == null || record.getUserId() == null || record.getCheckTime() == null) {
            return null;
        }

        java.util.List<AttendanceRecord> recentIllegalTimeRecords = attendanceRecordMapper.selectList(Wrappers.<AttendanceRecord>lambdaQuery()
                .eq(AttendanceRecord::getUserId, record.getUserId())
                .ge(AttendanceRecord::getCheckTime, record.getCheckTime().minusDays(CONTINUOUS_LATE_WINDOW_DAYS))
                .le(AttendanceRecord::getCheckTime, record.getCheckTime())
                .orderByDesc(AttendanceRecord::getCheckTime)
                .orderByDesc(AttendanceRecord::getId)
                .last("LIMIT " + CONTINUOUS_LATE_RECORD_COUNT));

        if (recentIllegalTimeRecords == null || recentIllegalTimeRecords.size() < CONTINUOUS_LATE_RECORD_COUNT) {
            return null;
        }

        StringBuilder builder = new StringBuilder("最近7天内连续3次发生非法时间打卡，判定为连续非法时间打卡模式异常；记录=");
        for (int index = recentIllegalTimeRecords.size() - 1; index >= 0; index--) {
            AttendanceRecord illegalRecord = recentIllegalTimeRecords.get(index);
            builder.append('[')
                    .append(illegalRecord.getCheckTime())
                    .append(" @ ")
                    .append(resolveLocationLabel(illegalRecord))
                    .append(']');
            if (index > 0) {
                builder.append(" -> ");
            }
        }
        return builder.toString();
    }

    private String buildContinuousRepeatCheckEvidence(AttendanceRecord record) {
        if (record == null || record.getUserId() == null || record.getCheckTime() == null) {
            return null;
        }

        java.util.List<AttendanceException> recentRepeatChecks = attendanceExceptionMapper.selectList(Wrappers.<AttendanceException>lambdaQuery()
                .eq(AttendanceException::getUserId, record.getUserId())
                .eq(AttendanceException::getSourceType, RULE_SOURCE)
                .in(AttendanceException::getType, "REPEAT_CHECK", CONTINUOUS_REPEAT_CHECK)
                .ge(AttendanceException::getCreateTime, record.getCheckTime().minusDays(CONTINUOUS_LATE_WINDOW_DAYS))
                .lt(AttendanceException::getCreateTime, record.getCheckTime())
                .orderByDesc(AttendanceException::getCreateTime)
                .orderByDesc(AttendanceException::getId)
                .last("LIMIT " + (CONTINUOUS_LATE_RECORD_COUNT - 1)));

        if (recentRepeatChecks == null || recentRepeatChecks.size() < CONTINUOUS_LATE_RECORD_COUNT - 1) {
            return null;
        }

        StringBuilder builder = new StringBuilder("最近7天内连续3次出现短时间重复打卡，判定为连续重复打卡模式异常；历史重复打卡时间=");
        for (int index = recentRepeatChecks.size() - 1; index >= 0; index--) {
            AttendanceException repeatCheck = recentRepeatChecks.get(index);
            builder.append('[').append(repeatCheck.getCreateTime()).append(']');
            if (index > 0) {
                builder.append(" -> ");
            }
        }
        builder.append("；当前打卡时间=").append(record.getCheckTime());
        return builder.toString();
    }

    private String buildContinuousAttendanceRiskEvidence(AttendanceRecord record, Rule rule) {
        if (record == null || record.getUserId() == null || record.getCheckTime() == null) {
            return null;
        }
        String currentType = resolveCurrentBaseRuleType(record, rule);
        java.util.List<AttendanceException> recentRuleExceptions = attendanceExceptionMapper.selectList(Wrappers.<AttendanceException>lambdaQuery()
                .eq(AttendanceException::getUserId, record.getUserId())
                .eq(AttendanceException::getSourceType, RULE_SOURCE)
                .ge(AttendanceException::getCreateTime, record.getCheckTime().minusDays(CONTINUOUS_LATE_WINDOW_DAYS))
                .lt(AttendanceException::getCreateTime, record.getCheckTime())
                .orderByDesc(AttendanceException::getCreateTime)
                .orderByDesc(AttendanceException::getId)
                .last("LIMIT " + (CONTINUOUS_LATE_RECORD_COUNT - 1)));
        if (recentRuleExceptions == null || recentRuleExceptions.size() < CONTINUOUS_LATE_RECORD_COUNT - 1) {
            return null;
        }

        StringBuilder builder = new StringBuilder("最近7天内连续3次出现规则异常，判定为持续考勤风险模式异常；历史异常=");
        for (int index = recentRuleExceptions.size() - 1; index >= 0; index--) {
            AttendanceException item = recentRuleExceptions.get(index);
            builder.append('[')
                    .append(item.getType())
                    .append(" @ ")
                    .append(item.getCreateTime())
                    .append(']');
            if (index > 0) {
                builder.append(" -> ");
            }
        }
        builder.append("；当前异常=").append(currentType);
        return builder.toString();
    }

    private String buildContinuousMultiLocationEvidence(AttendanceRecord record) {
        if (record == null || record.getUserId() == null || record.getCheckTime() == null) {
            return null;
        }

        java.util.List<AttendanceException> recentConflicts = attendanceExceptionMapper.selectList(Wrappers.<AttendanceException>lambdaQuery()
                .eq(AttendanceException::getUserId, record.getUserId())
                .eq(AttendanceException::getSourceType, RULE_SOURCE)
                .in(AttendanceException::getType, MULTI_LOCATION_CONFLICT, CONTINUOUS_MULTI_LOCATION_CONFLICT)
                .ge(AttendanceException::getCreateTime, record.getCheckTime().minusDays(CONTINUOUS_LATE_WINDOW_DAYS))
                .lt(AttendanceException::getCreateTime, record.getCheckTime())
                .orderByDesc(AttendanceException::getCreateTime)
                .orderByDesc(AttendanceException::getId)
                .last("LIMIT " + (CONTINUOUS_MULTI_LOCATION_RECORD_COUNT - 1)));

        String currentEvidence = buildMultiLocationConflictEvidence(record);
        if (recentConflicts == null || recentConflicts.size() < CONTINUOUS_MULTI_LOCATION_RECORD_COUNT - 1) {
            return currentEvidence;
        }

        StringBuilder builder = new StringBuilder("最近7天内连续3次出现多地点冲突，判定为连续多地点冲突模式异常；历史冲突时间=");
        for (int index = recentConflicts.size() - 1; index >= 0; index--) {
            AttendanceException conflict = recentConflicts.get(index);
            builder.append('[').append(conflict.getCreateTime()).append(']');
            if (index > 0) {
                builder.append(" -> ");
            }
        }
        if (currentEvidence != null) {
            builder.append("；当前冲突证据=").append(currentEvidence);
        }
        return builder.toString();
    }

    private String buildContinuousEarlyLeaveEvidence(AttendanceRecord record) {
        if (record == null || record.getUserId() == null || record.getCheckTime() == null) {
            return null;
        }

        java.util.List<AttendanceRecord> recentEarlyLeaveRecords = attendanceRecordMapper.selectList(Wrappers.<AttendanceRecord>lambdaQuery()
                .eq(AttendanceRecord::getUserId, record.getUserId())
                .eq(AttendanceRecord::getCheckType, "OUT")
                .ge(AttendanceRecord::getCheckTime, record.getCheckTime().minusDays(CONTINUOUS_LATE_WINDOW_DAYS))
                .le(AttendanceRecord::getCheckTime, record.getCheckTime())
                .orderByDesc(AttendanceRecord::getCheckTime)
                .orderByDesc(AttendanceRecord::getId)
                .last("LIMIT " + CONTINUOUS_LATE_RECORD_COUNT));

        if (recentEarlyLeaveRecords == null || recentEarlyLeaveRecords.size() < CONTINUOUS_LATE_RECORD_COUNT) {
            return null;
        }

        StringBuilder builder = new StringBuilder("最近7天内连续3次下班打卡早退，判定为连续早退模式异常；早退记录=");
        for (int index = recentEarlyLeaveRecords.size() - 1; index >= 0; index--) {
            AttendanceRecord earlyLeaveRecord = recentEarlyLeaveRecords.get(index);
            builder.append('[')
                    .append(earlyLeaveRecord.getCheckTime())
                    .append(" @ ")
                    .append(resolveLocationLabel(earlyLeaveRecord))
                    .append(']');
            if (index > 0) {
                builder.append(" -> ");
            }
        }
        return builder.toString();
    }

    private String buildMultiLocationConflictEvidence(AttendanceRecord record) {
        if (record == null || record.getUserId() == null || record.getCheckTime() == null || record.getId() == null) {
            return null;
        }
        if (record.getLongitude() == null || record.getLatitude() == null) {
            return null;
        }
        if (mapProperties.getMultiLocationWindowMinutes() == null || mapProperties.getMultiLocationDistanceMeters() == null) {
            return null;
        }

        AttendanceRecord previousRecord = attendanceRecordMapper.selectLatestBefore(record.getUserId(), record.getCheckTime(), record.getId());
        if (previousRecord == null || previousRecord.getCheckTime() == null) {
            return null;
        }
        if (previousRecord.getLongitude() == null || previousRecord.getLatitude() == null) {
            return null;
        }

        long intervalMinutes = Duration.between(previousRecord.getCheckTime(), record.getCheckTime()).toMinutes();
        BigDecimal distanceMeters = mapDistanceService.calculateDistanceMeters(
                previousRecord.getLongitude(),
                previousRecord.getLatitude(),
                record.getLongitude(),
                record.getLatitude()
        );
        return "短时间内在多个地点完成打卡，判定为空间冲突；前一条地点=" + resolveLocationLabel(previousRecord)
                + "，当前地点=" + resolveLocationLabel(record)
                + "，间隔分钟=" + intervalMinutes
                + "，实际距离米=" + formatDecimal(distanceMeters)
                + "，阈值距离米=" + formatDecimal(mapProperties.getMultiLocationDistanceMeters())
                + "，窗口分钟=" + mapProperties.getMultiLocationWindowMinutes();
    }

    private String resolveLocationLabel(AttendanceRecord record) {
        if (record.getLocation() != null && !record.getLocation().trim().isEmpty()) {
            return record.getLocation();
        }
        return "经度=" + formatDecimal(record.getLongitude()) + ",纬度=" + formatDecimal(record.getLatitude());
    }

    private String formatDecimal(BigDecimal value) {
        if (value == null) {
            return "null";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private void ensureRuleDecisionTrace(AttendanceException attendanceException) {
        java.util.List<?> decisionTraces = decisionTraceService.list(ATTENDANCE_EXCEPTION_BUSINESS_TYPE, attendanceException.getId());
        String ruleDecisionEvidence = resolveHistoricalRuleDecisionEvidence(attendanceException, decisionTraces);
        if (ruleDecisionEvidence == null) {
            return;
        }
        saveRuleDecisionTrace(attendanceException, ruleDecisionEvidence);
    }

    private String resolveHistoricalRuleDecisionEvidence(AttendanceException attendanceException,
                                                         java.util.List<?> decisionTraces) {
        if (!MULTI_LOCATION_CONFLICT.equals(attendanceException.getType())) {
            if (!decisionTraces.isEmpty()) {
                return null;
            }
            return attendanceException.getDescription();
        }
        String historicalExplanation = buildHistoricalMultiLocationTraceExplanation(attendanceException.getDescription());
        if (hasCompleteMultiLocationEvidence(decisionTraces) || hasDecisionTraceValue(decisionTraces, historicalExplanation)) {
            return null;
        }
        return historicalExplanation;
    }

    private String buildHistoricalMultiLocationTraceExplanation(String description) {
        return description + "；" + HISTORICAL_MULTI_LOCATION_TRACE_NOTE;
    }

    private boolean hasCompleteMultiLocationEvidence(java.util.List<?> decisionTraces) {
        for (Object decisionTrace : decisionTraces) {
            if (isCompleteMultiLocationEvidence(readDecisionTraceValue(decisionTrace, "getRuleResult"))
                    || isCompleteMultiLocationEvidence(readDecisionTraceValue(decisionTrace, "getDecisionReason"))) {
                return true;
            }
        }
        return false;
    }

    private boolean isCompleteMultiLocationEvidence(String traceValue) {
        return traceValue != null
                && traceValue.contains("前一条地点=")
                && traceValue.contains("当前地点=")
                && traceValue.contains("间隔分钟=")
                && traceValue.contains("实际距离米=")
                && traceValue.contains("阈值距离米=")
                && traceValue.contains("窗口分钟=");
    }

    private boolean hasDecisionTraceValue(java.util.List<?> decisionTraces, String expectedValue) {
        for (Object decisionTrace : decisionTraces) {
            if (expectedValue.equals(readDecisionTraceValue(decisionTrace, "getRuleResult"))
                    || expectedValue.equals(readDecisionTraceValue(decisionTrace, "getDecisionReason"))) {
                return true;
            }
        }
        return false;
    }

    private String readDecisionTraceValue(Object decisionTrace, String getterName) {
        if (decisionTrace == null) {
            return null;
        }
        try {
            Object value = decisionTrace.getClass().getMethod(getterName).invoke(decisionTrace);
            return value == null ? null : value.toString();
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private void saveRuleDecisionTrace(AttendanceException attendanceException, String ruleDecisionEvidence) {
        decisionTraceService.save(
                ATTENDANCE_EXCEPTION_BUSINESS_TYPE,
                attendanceException.getId(),
                ruleDecisionEvidence,
                null,
                attendanceException.getType(),
                null,
                ruleDecisionEvidence
        );
    }

    private String buildInputSummary(AttendanceRecord record, RiskFeaturesDTO riskFeatures) {
        long historyAbnormalCount = attendanceExceptionMapper.selectCount(Wrappers.<AttendanceException>lambdaQuery()
                .eq(AttendanceException::getUserId, record.getUserId()));
        return "记录编号：" + record.getId()
                + "；员工编号：" + record.getUserId()
                + "；打卡类型：" + formatCheckType(record.getCheckType())
                + "；打卡时间：" + record.getCheckTime()
                + "；打卡地点编号：" + safeText(record.getDeviceId())
                + "；打卡地点：" + safeText(record.getLocation())
                + "；地点经度：" + safeText(record.getLongitude())
                + "；地点纬度：" + safeText(record.getLatitude())
                + "；服务端人脸分数：" + safeText(record.getFaceScore())
                + "；客户端人脸分数：" + safeText(riskFeatures == null ? null : riskFeatures.getFaceScore())
                + "；客户端电脑设备是否变化：" + formatBoolean(riskFeatures == null ? null : riskFeatures.getDeviceChanged())
                + "；客户端打卡地点是否变化：" + formatBoolean(riskFeatures == null ? null : riskFeatures.getLocationChanged())
                + "；客户端历史异常次数：" + safeText(riskFeatures == null ? null : riskFeatures.getHistoryAbnormalCount())
                + "；数据库历史异常次数：" + historyAbnormalCount
                + "。请所有说明使用自然中文，不要输出英文键名、技术字段名或程序变量名。";
    }

    private String enrichPromptContext(String inputSummary, PromptTemplate promptTemplate) {
        String promptVersion = promptTemplate == null || !StringUtils.hasText(promptTemplate.getVersion())
                ? "unknown"
                : promptTemplate.getVersion().trim();
        return inputSummary
                + "；提示词版本：" + promptVersion
                + "；提示词指纹：" + digestPromptContent(promptTemplate == null ? null : promptTemplate.getContent());
    }

    private String enrichLogInputSummary(String inputSummary) {
        String provider = modelGatewayProperties == null ? null : modelGatewayProperties.getProvider();
        if (!StringUtils.hasText(provider)) {
            return inputSummary;
        }
        return inputSummary + "；模型提供方：" + provider.trim();
    }

    private String digestPromptContent(String promptContent) {
        String normalizedContent = promptContent == null ? "" : promptContent;
        return DigestUtils.md5DigestAsHex(normalizedContent.getBytes(StandardCharsets.UTF_8));
    }

    private String limitText(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String normalizedValue = value.trim();
        if (normalizedValue.length() <= maxLength) {
            return normalizedValue;
        }
        return normalizedValue.substring(0, maxLength);
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
        return "打卡地点编号：" + safeText(record.getDeviceId())
                + "；打卡地点：" + safeText(record.getLocation())
                + "；地点经度：" + safeText(record.getLongitude())
                + "；地点纬度：" + safeText(record.getLatitude())
                + "；服务端人脸分数：" + safeText(record.getFaceScore())
                + "；客户端电脑设备是否变化：" + formatBoolean(riskFeatures == null ? null : riskFeatures.getDeviceChanged())
                + "；客户端打卡地点是否变化：" + formatBoolean(riskFeatures == null ? null : riskFeatures.getLocationChanged());
    }

    private String formatCheckType(String checkType) {
        if ("OUT".equals(checkType)) {
            return "下班打卡";
        }
        if ("IN".equals(checkType)) {
            return "上班打卡";
        }
        return safeText(checkType);
    }

    private String formatBoolean(Boolean value) {
        if (value == null) {
            return "未提供";
        }
        return Boolean.TRUE.equals(value) ? "是" : "否";
    }

    private String safeText(Object value) {
        return value == null ? "未提供" : String.valueOf(value);
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
