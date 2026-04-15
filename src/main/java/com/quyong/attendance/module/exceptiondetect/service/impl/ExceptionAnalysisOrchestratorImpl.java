package com.quyong.attendance.module.exceptiondetect.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.attendance.entity.AttendanceRecord;
import com.quyong.attendance.module.attendance.mapper.AttendanceRecordMapper;
import com.quyong.attendance.module.device.entity.Device;
import com.quyong.attendance.module.device.mapper.DeviceMapper;
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
import com.quyong.attendance.module.face.config.FaceEngineProperties;
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
import com.quyong.attendance.module.warning.service.WarningService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class ExceptionAnalysisOrchestratorImpl implements ExceptionAnalysisOrchestrator {

    private static final String ATTENDANCE_EXCEPTION_BUSINESS_TYPE = "ATTENDANCE_EXCEPTION";
    private static final String RULE_SOURCE = "RULE";
    private static final String MODEL_SOURCE = "MODEL";
    private static final String MODEL_FALLBACK_SOURCE = "MODEL_FALLBACK";
    private static final String PENDING_STATUS = "PENDING";
    private static final String ABNORMAL_STATUS = "ABNORMAL";
    private static final String ABSENT = "ABSENT";
    private static final String MISSING_CHECKOUT = "MISSING_CHECKOUT";
    private static final String NORMAL_RESULT = "NORMAL";
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
    private final DeviceMapper deviceMapper;
    private final MapProperties mapProperties;
    private final ModelGatewayProperties modelGatewayProperties;
    private final FaceEngineProperties faceEngineProperties;
    private final WarningService warningService;
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
                                              DeviceMapper deviceMapper,
                                              MapProperties mapProperties,
                                              ModelGatewayProperties modelGatewayProperties,
                                              FaceEngineProperties faceEngineProperties,
                                              WarningService warningService) {
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
        this.deviceMapper = deviceMapper;
        this.mapProperties = mapProperties;
        this.modelGatewayProperties = modelGatewayProperties;
        this.faceEngineProperties = faceEngineProperties;
        this.warningService = warningService;
        this.recordLocks = initRecordLocks();
    }

    @Override
    @Transactional
    public ExceptionDecisionVO ruleCheck(RuleCheckDTO dto) {
        RuleCheckDTO validatedDTO = exceptionValidationSupport.validateRuleCheck(dto);
        if (validatedDTO.getRecordId() == null) {
            return ruleCheckWithoutRecord(validatedDTO);
        }
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
                ensureRuleExplanation(record, rule, existing, null);
                return toDecisionVO(existing, findLatestAnalysis(existing.getId()));
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
            ensureRuleExplanation(record, rule, attendanceException, ruleDecisionEvidence);
            return toDecisionVO(attendanceException, findLatestAnalysis(attendanceException.getId()));
        }
    }

    @Override
    @Transactional
    public ExceptionDecisionVO complexCheck(ComplexCheckDTO dto) {
        ComplexCheckDTO validatedDTO = exceptionValidationSupport.validateComplexCheck(dto);
        if (validatedDTO.getRecordId() == null) {
            return complexCheckWithoutRecord(validatedDTO);
        }
        synchronized (getRecordLock(validatedDTO.getRecordId())) {
            AttendanceRecord record = attendanceRecordMapper.selectById(validatedDTO.getRecordId());
            if (record == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "考勤记录不存在");
            }

            boolean forceRefresh = shouldForceRefreshComplexCheck(validatedDTO);
            AttendanceException refreshTarget = resolveRefreshTarget(validatedDTO, record.getId());
            ExceptionDecisionVO ruleDecision = resolveRuleDecisionForComplexCheck(record);

            if (shouldBypassComplexCheckForConfiguredPunch(record, ruleDecision, validatedDTO.getRiskFeatures())) {
                if (refreshTarget != null) {
                    closeModelExceptionAsFalsePositive(refreshTarget);
                }
                return ruleDecision == null ? buildNormalRuleDecision(refreshTarget) : ruleDecision;
            }

            AttendanceException existingModel = forceRefresh ? null : findLatestException(record.getId(), "MODEL");
            if (existingModel != null) {
                return toDecisionVO(existingModel, findLatestAnalysis(existingModel.getId()));
            }
            AttendanceException existingFallback = forceRefresh ? null : findLatestException(record.getId(), "MODEL_FALLBACK");
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
                response = applyLowConfidenceReviewEscalation(response);
            } catch (Exception exception) {
                AttendanceException fallbackException = prepareModelException(refreshTarget, record, COMPLEX_ATTENDANCE_RISK, "MEDIUM", "MODEL_FALLBACK", "模型调用失败，已转人工处理");
                saveOrUpdateAttendanceException(fallbackException);

                ExceptionAnalysis fallbackAnalysis = prepareFallbackAnalysis(refreshTarget, fallbackException.getId(), promptTemplate, inputSummary, exception.getMessage());
                saveOrUpdateAnalysis(fallbackAnalysis, refreshTarget == null ? null : findLatestAnalysis(refreshTarget.getId()));

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

            String normalizedExceptionType = normalizeModelExceptionType(response.getConclusion());
            AttendanceException attendanceException = prepareModelException(
                    refreshTarget,
                    record,
                    normalizedExceptionType,
                    limitText(response.getRiskLevel(), 20),
                    "MODEL",
                    limitText(response.getDecisionReason(), 255)
            );
            saveOrUpdateAttendanceException(attendanceException);

            ExceptionAnalysis analysis = prepareModelAnalysis(refreshTarget, attendanceException.getId(), promptTemplate, inputSummary, response);
            saveOrUpdateAnalysis(analysis, refreshTarget == null ? null : findLatestAnalysis(refreshTarget.getId()));

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

    private boolean shouldForceRefreshComplexCheck(ComplexCheckDTO dto) {
        return dto != null && dto.getExceptionId() != null;
    }

    private ExceptionDecisionVO resolveRuleDecisionForComplexCheck(AttendanceRecord record) {
        if (record == null || record.getId() == null) {
            return null;
        }
        RuleCheckDTO ruleCheckDTO = new RuleCheckDTO();
        ruleCheckDTO.setRecordId(record.getId());
        return ruleCheck(ruleCheckDTO);
    }

    private AttendanceException resolveRefreshTarget(ComplexCheckDTO dto, Long recordId) {
        if (!shouldForceRefreshComplexCheck(dto)) {
            return null;
        }
        AttendanceException target = requireExistingException(dto.getExceptionId());
        if (recordId != null && !recordId.equals(target.getRecordId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "异常记录与考勤记录不匹配");
        }
        return target;
    }

    private boolean shouldBypassComplexCheckForConfiguredPunch(AttendanceRecord record,
                                                               ExceptionDecisionVO ruleDecision,
                                                               RiskFeaturesDTO riskFeatures) {
        if (!isEnabledConfiguredDevice(record)) {
            return false;
        }
        BigDecimal faceScore = resolveFaceScore(record, riskFeatures);
        BigDecimal matchThreshold = faceEngineProperties == null ? null : faceEngineProperties.getMatchScoreThreshold();
        return faceScore != null && matchThreshold != null && faceScore.compareTo(matchThreshold) >= 0;
    }

    private ExceptionDecisionVO buildNormalRuleDecision(AttendanceException refreshTarget) {
        ExceptionDecisionVO vo = new ExceptionDecisionVO();
        if (refreshTarget != null) {
            vo.setExceptionId(refreshTarget.getId());
        }
        vo.setType("NORMAL");
        vo.setRiskLevel("LOW");
        vo.setSourceType("RULE");
        vo.setProcessStatus("REVIEWED");
        vo.setModelConclusion("当前记录符合已配置打卡规则");
        vo.setReasonSummary("当前记录位于系统已启用打卡地点，且人脸校验通过，应按已配置考勤规则就事论事处理。");
        vo.setActionSuggestion("无需按综合异常处理，请以当前有效规则结果为准。");
        vo.setConfidenceScore(null);
        return vo;
    }

    private void closeModelExceptionAsFalsePositive(AttendanceException attendanceException) {
        if (attendanceException == null || attendanceException.getId() == null) {
            return;
        }
        attendanceException.setProcessStatus("REVIEWED");
        attendanceException.setDescription("经重新判断，该记录符合已配置打卡规则，原综合异常已关闭");
        attendanceExceptionMapper.updateById(attendanceException);

        ExceptionAnalysis latestAnalysis = findLatestAnalysis(attendanceException.getId());
        if (latestAnalysis == null) {
            return;
        }
        latestAnalysis.setModelConclusion("已关闭误报");
        latestAnalysis.setConfidenceScore(null);
        latestAnalysis.setDecisionReason("经重新判断，该记录符合已配置打卡规则，原综合异常已关闭");
        latestAnalysis.setSuggestion("无需按综合异常处理，可按当前有效规则结果继续跟进。");
        latestAnalysis.setReasonSummary("经重新判断，该记录符合已配置打卡规则，原综合异常已关闭。");
        latestAnalysis.setActionSuggestion("无需继续按综合异常升级处理，请以当前有效规则结果为准。");
        latestAnalysis.setSimilarCaseSummary("历史综合识别结果已失效，本条记录不再作为综合异常案例参考。");
        exceptionAnalysisMapper.updateById(latestAnalysis);
        warningService.syncWarningByExceptionId(attendanceException.getId());
    }

    private BigDecimal resolveFaceScore(AttendanceRecord record, RiskFeaturesDTO riskFeatures) {
        if (riskFeatures != null && riskFeatures.getFaceScore() != null) {
            return riskFeatures.getFaceScore();
        }
        return record == null ? null : record.getFaceScore();
    }

    private boolean isEnabledConfiguredDevice(AttendanceRecord record) {
        if (record == null || !StringUtils.hasText(record.getDeviceId())) {
            return false;
        }
        Device device = deviceMapper.selectById(record.getDeviceId().trim());
        return device != null && Integer.valueOf(1).equals(device.getStatus());
    }

    private AttendanceException prepareModelException(AttendanceException existing,
                                                      AttendanceRecord record,
                                                      String type,
                                                      String riskLevel,
                                                      String sourceType,
                                                      String description) {
        AttendanceException attendanceException = existing == null ? new AttendanceException() : existing;
        attendanceException.setRecordId(record.getId());
        attendanceException.setUserId(record.getUserId());
        attendanceException.setType(type);
        attendanceException.setRiskLevel(riskLevel);
        attendanceException.setSourceType(sourceType);
        attendanceException.setDescription(description);
        attendanceException.setProcessStatus(PENDING_STATUS);
        return attendanceException;
    }

    private void saveOrUpdateAttendanceException(AttendanceException attendanceException) {
        if (attendanceException.getId() == null) {
            attendanceExceptionMapper.insert(attendanceException);
            return;
        }
        attendanceExceptionMapper.updateById(attendanceException);
    }

    private ExceptionAnalysis prepareFallbackAnalysis(AttendanceException existing,
                                                     Long exceptionId,
                                                     PromptTemplate promptTemplate,
                                                     String inputSummary,
                                                     String decisionReason) {
        ExceptionAnalysis fallbackAnalysis = existing == null ? new ExceptionAnalysis() : findLatestAnalysis(existing.getId());
        if (fallbackAnalysis == null) {
            fallbackAnalysis = new ExceptionAnalysis();
        }
        fallbackAnalysis.setExceptionId(exceptionId);
        fallbackAnalysis.setPromptTemplateId(promptTemplate.getId());
        fallbackAnalysis.setInputSummary(inputSummary);
        fallbackAnalysis.setModelResult(null);
        fallbackAnalysis.setModelConclusion(COMPLEX_ATTENDANCE_RISK);
        fallbackAnalysis.setDecisionReason(decisionReason);
        fallbackAnalysis.setSuggestion("建议管理员查看原始记录并人工确认");
        fallbackAnalysis.setReasonSummary("模型调用失败，已转人工复核");
        fallbackAnalysis.setActionSuggestion("建议管理员查看原始记录并人工确认");
        fallbackAnalysis.setSimilarCaseSummary(null);
        fallbackAnalysis.setPromptVersion(promptTemplate.getVersion());
        fallbackAnalysis.setConfidenceScore(null);
        return fallbackAnalysis;
    }

    private ExceptionAnalysis prepareModelAnalysis(AttendanceException existing,
                                                   Long exceptionId,
                                                   PromptTemplate promptTemplate,
                                                   String inputSummary,
                                                   ModelInvokeResponse response) {
        ExceptionAnalysis analysis = existing == null ? null : findLatestAnalysis(existing.getId());
        if (analysis == null) {
            analysis = new ExceptionAnalysis();
        }
        analysis.setExceptionId(exceptionId);
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
        return analysis;
    }

    private void saveOrUpdateAnalysis(ExceptionAnalysis analysis, ExceptionAnalysis existingAnalysis) {
        if (existingAnalysis == null || existingAnalysis.getId() == null) {
            exceptionAnalysisMapper.insert(analysis);
            return;
        }
        analysis.setId(existingAnalysis.getId());
        exceptionAnalysisMapper.updateById(analysis);
    }

    @Override
    @Transactional
    public int backfillAbsenceContext() {
        java.util.List<AttendanceException> candidates = attendanceExceptionMapper.selectList(Wrappers.<AttendanceException>lambdaQuery()
                .eq(AttendanceException::getType, ABSENT)
                .isNull(AttendanceException::getRecordId)
                .orderByAsc(AttendanceException::getId));
        int updatedCount = 0;
        for (AttendanceException attendanceException : candidates) {
            boolean updated = false;
            String ruleEvidence = buildAbsenceRuleDecisionEvidence(attendanceException);
            java.util.List<?> decisionTraces = decisionTraceService.list(ATTENDANCE_EXCEPTION_BUSINESS_TYPE, attendanceException.getId());
            if (!hasDecisionTraceValue(decisionTraces, ruleEvidence)) {
                saveRuleDecisionTrace(attendanceException, ruleEvidence);
                updated = true;
                decisionTraces = decisionTraceService.list(ATTENDANCE_EXCEPTION_BUSINESS_TYPE, attendanceException.getId());
            }

            ExceptionAnalysis analysis = findLatestAnalysis(attendanceException.getId());
            if (analysis == null) {
                analysis = buildAbsenceFallbackAnalysis(attendanceException);
                exceptionAnalysisMapper.insert(analysis);
                updated = true;
            }
            String modelTraceMarker = analysis == null ? null : analysis.getModelResult();
            if (StringUtils.hasText(modelTraceMarker) && !hasDecisionTraceValue(decisionTraces, modelTraceMarker)) {
                saveAbsenceFallbackTrace(attendanceException, analysis);
                updated = true;
            }

            if (updated) {
                updatedCount++;
            }
        }
        return updatedCount;
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

    private ModelInvokeResponse applyLowConfidenceReviewEscalation(ModelInvokeResponse response) {
        if (response == null || response.getConfidenceScore() == null) {
            return response;
        }
        BigDecimal threshold = modelGatewayProperties.getLowConfidenceThreshold();
        if (threshold == null || response.getConfidenceScore().compareTo(threshold) >= 0) {
            return response;
        }

        String riskLevel = limitText(response.getRiskLevel(), 20);
        if (!"HIGH".equals(riskLevel) && !"MEDIUM".equals(riskLevel)) {
            response.setRiskLevel("MEDIUM");
        }
        response.setDecisionReason(appendLowConfidenceNote(response.getDecisionReason()));
        response.setReasonSummary(appendLowConfidenceNote(response.getReasonSummary()));
        if (!StringUtils.hasText(response.getActionSuggestion())) {
            response.setActionSuggestion("模型置信度偏低，建议管理员尽快人工复核");
        } else {
            response.setActionSuggestion(response.getActionSuggestion() + "；模型置信度偏低，建议优先人工复核");
        }
        return response;
    }

    private ExceptionDecisionVO ruleCheckWithoutRecord(RuleCheckDTO dto) {
        AttendanceException attendanceException = requireExistingException(dto == null ? null : dto.getExceptionId());
        ensureSupportedExceptionWithoutRecord(attendanceException, "规则检查");
        String evidence = buildContextRuleDecisionEvidence(attendanceException);
        java.util.List<?> decisionTraces = decisionTraceService.list(ATTENDANCE_EXCEPTION_BUSINESS_TYPE, attendanceException.getId());
        if (!hasDecisionTraceValue(decisionTraces, evidence)) {
            saveRuleDecisionTrace(attendanceException, evidence);
        }

        ExceptionDecisionVO vo = toDecisionVO(attendanceException);
        vo.setType(resolveContextRuleConclusion(attendanceException));
        return vo;
    }

    private ExceptionDecisionVO complexCheckWithoutRecord(ComplexCheckDTO dto) {
        AttendanceException attendanceException = requireExistingException(dto == null ? null : dto.getExceptionId());
        ensureSupportedExceptionWithoutRecord(attendanceException, "系统判断");

        ExceptionAnalysis analysis = findLatestAnalysis(attendanceException.getId());
        if (analysis == null) {
            analysis = buildContextFallbackAnalysis(attendanceException);
            exceptionAnalysisMapper.insert(analysis);
            saveContextFallbackTrace(attendanceException, analysis);
        } else {
            java.util.List<?> decisionTraces = decisionTraceService.list(ATTENDANCE_EXCEPTION_BUSINESS_TYPE, attendanceException.getId());
            if (!hasDecisionTraceValue(decisionTraces, analysis.getModelResult())) {
                saveContextFallbackTrace(attendanceException, analysis);
            }
        }
        return toDecisionVO(attendanceException, analysis, MODEL_FALLBACK_SOURCE);
    }

    private void ensureSupportedExceptionWithoutRecord(AttendanceException attendanceException, String actionName) {
        if (attendanceException == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "异常记录不存在");
        }
        if (attendanceException.getRecordId() != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前异常已存在打卡记录，请基于记录重新执行" + actionName);
        }
        if (!ABSENT.equals(attendanceException.getType()) && !MISSING_CHECKOUT.equals(attendanceException.getType())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前异常缺少可用于" + actionName + "的记录上下文");
        }
    }

    private String resolveContextRuleConclusion(AttendanceException attendanceException) {
        if (attendanceException != null && MISSING_CHECKOUT.equals(attendanceException.getType())) {
            return resolveMissingCheckoutRuleConclusion(attendanceException);
        }
        return resolveAbsenceRuleConclusion(attendanceException);
    }

    private String buildContextRuleDecisionEvidence(AttendanceException attendanceException) {
        if (attendanceException != null && MISSING_CHECKOUT.equals(attendanceException.getType())) {
            return buildMissingCheckoutRuleDecisionEvidence(attendanceException);
        }
        return buildAbsenceRuleDecisionEvidence(attendanceException);
    }

    private ExceptionAnalysis buildContextFallbackAnalysis(AttendanceException attendanceException) {
        if (attendanceException != null && MISSING_CHECKOUT.equals(attendanceException.getType())) {
            return buildMissingCheckoutFallbackAnalysis(attendanceException);
        }
        return buildAbsenceFallbackAnalysis(attendanceException);
    }

    private ExceptionAnalysis buildAbsenceFallbackAnalysis(AttendanceException attendanceException) {
        AttendanceRecord earliestCheckIn = findEarliestSameDayCheckIn(attendanceException);
        String currentConclusion = resolveAbsenceRuleConclusion(attendanceException);
        String inputSummary = buildAbsenceInputSummary(attendanceException, earliestCheckIn);
        String reasonSummary = buildAbsenceReasonSummary(attendanceException, earliestCheckIn, currentConclusion);
        String actionSuggestion = buildAbsenceActionSuggestion(currentConclusion);

        ExceptionAnalysis analysis = new ExceptionAnalysis();
        analysis.setExceptionId(attendanceException.getId());
        analysis.setInputSummary(inputSummary);
        analysis.setModelResult(reasonSummary);
        analysis.setModelConclusion(currentConclusion);
        analysis.setConfidenceScore(resolveAbsenceConfidenceScore(currentConclusion));
        analysis.setDecisionReason(buildAbsenceRuleDecisionEvidence(attendanceException));
        analysis.setSuggestion(actionSuggestion);
        analysis.setReasonSummary(reasonSummary);
        analysis.setActionSuggestion(actionSuggestion);
        analysis.setSimilarCaseSummary(buildAbsenceCaseSummary(currentConclusion));
        analysis.setPromptVersion("absence-context-fallback-v1");
        return analysis;
    }

    private ExceptionAnalysis buildMissingCheckoutFallbackAnalysis(AttendanceException attendanceException) {
        AttendanceRecord latestCheckOut = findLatestSameDayCheckOut(attendanceException);
        String currentConclusion = resolveMissingCheckoutRuleConclusion(attendanceException);
        String inputSummary = buildMissingCheckoutInputSummary(attendanceException, latestCheckOut);
        String reasonSummary = buildMissingCheckoutReasonSummary(attendanceException, latestCheckOut, currentConclusion);
        String actionSuggestion = buildMissingCheckoutActionSuggestion(currentConclusion);

        ExceptionAnalysis analysis = new ExceptionAnalysis();
        analysis.setExceptionId(attendanceException.getId());
        analysis.setInputSummary(inputSummary);
        analysis.setModelResult(reasonSummary);
        analysis.setModelConclusion(currentConclusion);
        analysis.setConfidenceScore(resolveMissingCheckoutConfidenceScore(currentConclusion));
        analysis.setDecisionReason(buildMissingCheckoutRuleDecisionEvidence(attendanceException));
        analysis.setSuggestion(actionSuggestion);
        analysis.setReasonSummary(reasonSummary);
        analysis.setActionSuggestion(actionSuggestion);
        analysis.setSimilarCaseSummary(buildMissingCheckoutCaseSummary(currentConclusion));
        analysis.setPromptVersion("missing-checkout-context-fallback-v1");
        return analysis;
    }

    private AttendanceRecord findEarliestSameDayCheckIn(AttendanceException attendanceException) {
        if (attendanceException == null || attendanceException.getUserId() == null || attendanceException.getCreateTime() == null) {
            return null;
        }
        LocalDate targetDay = attendanceException.getCreateTime().toLocalDate();
        java.util.List<AttendanceRecord> records = attendanceRecordMapper.selectList(Wrappers.<AttendanceRecord>lambdaQuery()
                .eq(AttendanceRecord::getUserId, attendanceException.getUserId())
                .eq(AttendanceRecord::getCheckType, "IN")
                .ge(AttendanceRecord::getCheckTime, targetDay.atStartOfDay())
                .lt(AttendanceRecord::getCheckTime, targetDay.plusDays(1L).atStartOfDay())
                .orderByAsc(AttendanceRecord::getCheckTime)
                .orderByAsc(AttendanceRecord::getId)
                .last("LIMIT 1"));
        return records == null || records.isEmpty() ? null : records.get(0);
    }

    private AttendanceRecord findLatestSameDayCheckOut(AttendanceException attendanceException) {
        if (attendanceException == null || attendanceException.getUserId() == null || attendanceException.getCreateTime() == null) {
            return null;
        }
        LocalDate targetDay = attendanceException.getCreateTime().toLocalDate();
        java.util.List<AttendanceRecord> records = attendanceRecordMapper.selectList(Wrappers.<AttendanceRecord>lambdaQuery()
                .eq(AttendanceRecord::getUserId, attendanceException.getUserId())
                .eq(AttendanceRecord::getCheckType, "OUT")
                .ge(AttendanceRecord::getCheckTime, targetDay.atStartOfDay())
                .lt(AttendanceRecord::getCheckTime, targetDay.plusDays(1L).atStartOfDay())
                .orderByDesc(AttendanceRecord::getCheckTime)
                .orderByDesc(AttendanceRecord::getId)
                .last("LIMIT 1"));
        return records == null || records.isEmpty() ? null : records.get(0);
    }

    private String resolveAbsenceRuleConclusion(AttendanceException attendanceException) {
        AttendanceRecord earliestCheckIn = findEarliestSameDayCheckIn(attendanceException);
        if (earliestCheckIn == null) {
            return ABSENT;
        }
        Rule rule = safeGetEnabledRule();
        if (rule != null && isLate(earliestCheckIn, rule)) {
            return "LATE";
        }
        return NORMAL_RESULT;
    }

    private String resolveMissingCheckoutRuleConclusion(AttendanceException attendanceException) {
        AttendanceRecord latestCheckOut = findLatestSameDayCheckOut(attendanceException);
        if (latestCheckOut == null) {
            return MISSING_CHECKOUT;
        }
        Rule rule = safeGetEnabledRule();
        if (rule != null && isEarlyLeave(latestCheckOut, rule)) {
            return "EARLY_LEAVE";
        }
        return NORMAL_RESULT;
    }

    private Rule safeGetEnabledRule() {
        try {
            return ruleService.getEnabledRule();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private String buildAbsenceRuleDecisionEvidence(AttendanceException attendanceException) {
        AttendanceRecord earliestCheckIn = findEarliestSameDayCheckIn(attendanceException);
        StringBuilder builder = new StringBuilder();
        builder.append(StringUtils.hasText(attendanceException.getDescription()) ? attendanceException.getDescription() : "系统按规则生成缺勤异常");
        if (earliestCheckIn == null) {
            builder.append("；当前复核未发现当天上班打卡记录");
        } else {
            builder.append("；当前复核发现当天最早上班打卡时间=").append(earliestCheckIn.getCheckTime());
            Rule rule = safeGetEnabledRule();
            if (rule != null) {
                builder.append("；规则截止时间=").append(rule.getStartTime().plusMinutes(rule.getLateThreshold().longValue()));
            }
        }
        return builder.toString();
    }

    private String buildMissingCheckoutRuleDecisionEvidence(AttendanceException attendanceException) {
        AttendanceRecord latestCheckOut = findLatestSameDayCheckOut(attendanceException);
        StringBuilder builder = new StringBuilder();
        builder.append(StringUtils.hasText(attendanceException.getDescription()) ? attendanceException.getDescription() : "系统按规则生成下班缺卡异常");
        if (latestCheckOut == null) {
            builder.append("；当前复核未发现当天下班打卡记录");
        } else {
            builder.append("；当前复核发现当天最晚下班打卡时间=").append(latestCheckOut.getCheckTime());
            Rule rule = safeGetEnabledRule();
            if (rule != null) {
                builder.append("；规则下班时间=").append(rule.getEndTime());
                builder.append("；早退阈值截止=").append(rule.getEndTime().minusMinutes(rule.getEarlyThreshold().longValue()));
            }
        }
        return builder.toString();
    }

    private String buildAbsenceInputSummary(AttendanceException attendanceException, AttendanceRecord earliestCheckIn) {
        long historyAbnormalCount = attendanceExceptionMapper.selectCount(Wrappers.<AttendanceException>lambdaQuery()
                .eq(AttendanceException::getUserId, attendanceException.getUserId()));
        return "异常编号：" + attendanceException.getId()
                + "；员工编号：" + attendanceException.getUserId()
                + "；异常类型：缺勤"
                + "；异常生成时间：" + attendanceException.getCreateTime()
                + "；当前最早上班打卡时间：" + (earliestCheckIn == null ? "未发现" : earliestCheckIn.getCheckTime())
                + "；当前最早上班打卡地点：" + (earliestCheckIn == null ? "未发现" : safeText(earliestCheckIn.getLocation()))
                + "；历史异常次数：" + historyAbnormalCount
                + "；规则说明：" + safeText(attendanceException.getDescription())
                + "。请所有说明使用自然中文，不要输出英文键名、技术字段名或程序变量名。";
    }

    private String buildMissingCheckoutInputSummary(AttendanceException attendanceException, AttendanceRecord latestCheckOut) {
        long historyAbnormalCount = attendanceExceptionMapper.selectCount(Wrappers.<AttendanceException>lambdaQuery()
                .eq(AttendanceException::getUserId, attendanceException.getUserId()));
        return "异常编号：" + attendanceException.getId()
                + "；员工编号：" + attendanceException.getUserId()
                + "；异常类型：下班缺卡"
                + "；异常生成时间：" + attendanceException.getCreateTime()
                + "；当前最晚下班打卡时间：" + (latestCheckOut == null ? "未发现" : latestCheckOut.getCheckTime())
                + "；当前最晚下班打卡地点：" + (latestCheckOut == null ? "未发现" : safeText(latestCheckOut.getLocation()))
                + "；历史异常次数：" + historyAbnormalCount
                + "；规则说明：" + safeText(attendanceException.getDescription())
                + "。请所有说明使用自然中文，不要输出英文键名、技术字段名或程序变量名。";
    }

    private String buildAbsenceReasonSummary(AttendanceException attendanceException,
                                             AttendanceRecord earliestCheckIn,
                                             String currentConclusion) {
        if (earliestCheckIn == null) {
            return "系统复核未发现该员工在异常生成当天的上班打卡记录，当前缺勤结论与规则检测结果一致。";
        }
        if ("LATE".equals(currentConclusion)) {
            return "系统复核发现该员工当天已存在上班打卡，但最早签到时间晚于规则阈值，当前缺勤异常更接近迟到或补录后待修正场景。";
        }
        return "系统复核发现该员工当天已存在有效上班打卡，当前缺勤异常可能受定时检测生成时点或后续补录影响，建议人工确认后修正。";
    }

    private String buildMissingCheckoutReasonSummary(AttendanceException attendanceException,
                                                     AttendanceRecord latestCheckOut,
                                                     String currentConclusion) {
        if (latestCheckOut == null) {
            return "系统复核未发现该员工在异常生成当天的下班打卡记录，当前下班缺卡结论与规则检测结果一致。";
        }
        if ("EARLY_LEAVE".equals(currentConclusion)) {
            return "系统复核发现该员工当天已存在下班打卡，但最晚签退时间早于规则阈值，当前下班缺卡异常更接近早退或补录后待修正场景。";
        }
        return "系统复核发现该员工当天已存在有效下班打卡，当前下班缺卡异常可能受定时检测生成时点或后续补录影响，建议人工确认后修正。";
    }

    private String buildAbsenceActionSuggestion(String currentConclusion) {
        if (ABSENT.equals(currentConclusion)) {
            return "建议管理员结合请假、外出和补卡情况继续复核，必要时向员工发起说明请求。";
        }
        if ("LATE".equals(currentConclusion)) {
            return "建议管理员重点核对当天最早上班打卡时间，并按迟到或补卡修正流程处理当前异常。";
        }
        return "建议管理员核对当天实际签到证据，确认是否关闭该缺勤异常并同步修正相关记录。";
    }

    private String buildMissingCheckoutActionSuggestion(String currentConclusion) {
        if (MISSING_CHECKOUT.equals(currentConclusion)) {
            return "建议管理员结合加班、外出和补卡情况继续复核，必要时向员工发起说明请求。";
        }
        if ("EARLY_LEAVE".equals(currentConclusion)) {
            return "建议管理员重点核对当天最晚下班打卡时间，并按早退或补卡修正流程处理当前异常。";
        }
        return "建议管理员核对当天实际签退证据，确认是否关闭该下班缺卡异常并同步修正相关记录。";
    }

    private String buildAbsenceCaseSummary(String currentConclusion) {
        if (ABSENT.equals(currentConclusion)) {
            return "同类场景通常由未签到、漏打卡或请假未同步引起。";
        }
        if ("LATE".equals(currentConclusion)) {
            return "同类场景通常由迟到后补登、人工补卡通过或缺勤任务生成早于实际签到引起。";
        }
        return "同类场景通常由事后补登、补卡审批通过或异常生成时点与最终签到结果不一致引起。";
    }

    private String buildMissingCheckoutCaseSummary(String currentConclusion) {
        if (MISSING_CHECKOUT.equals(currentConclusion)) {
            return "同类场景通常由漏签退、加班后忘记打卡或外出未补签引起。";
        }
        if ("EARLY_LEAVE".equals(currentConclusion)) {
            return "同类场景通常由提前离岗、补卡后回填或异常生成早于最终签退结果引起。";
        }
        return "同类场景通常由事后补登、补卡审批通过或异常生成时点与最终签退结果不一致引起。";
    }

    private BigDecimal resolveAbsenceConfidenceScore(String currentConclusion) {
        if (ABSENT.equals(currentConclusion)) {
            return new BigDecimal("0.93");
        }
        if ("LATE".equals(currentConclusion)) {
            return new BigDecimal("0.68");
        }
        return new BigDecimal("0.58");
    }

    private BigDecimal resolveMissingCheckoutConfidenceScore(String currentConclusion) {
        if (MISSING_CHECKOUT.equals(currentConclusion)) {
            return new BigDecimal("0.91");
        }
        if ("EARLY_LEAVE".equals(currentConclusion)) {
            return new BigDecimal("0.68");
        }
        return new BigDecimal("0.58");
    }

    private void saveContextFallbackTrace(AttendanceException attendanceException, ExceptionAnalysis analysis) {
        decisionTraceService.save(
                ATTENDANCE_EXCEPTION_BUSINESS_TYPE,
                attendanceException.getId(),
                buildContextRuleDecisionEvidence(attendanceException),
                analysis == null ? null : analysis.getModelResult(),
                analysis == null ? attendanceException.getType() : analysis.getModelConclusion(),
                analysis == null ? null : analysis.getConfidenceScore(),
                analysis == null ? attendanceException.getDescription() : analysis.getDecisionReason()
        );
    }

    private void saveAbsenceFallbackTrace(AttendanceException attendanceException, ExceptionAnalysis analysis) {
        decisionTraceService.save(
                ATTENDANCE_EXCEPTION_BUSINESS_TYPE,
                attendanceException.getId(),
                buildAbsenceRuleDecisionEvidence(attendanceException),
                analysis == null ? null : analysis.getModelResult(),
                analysis == null ? attendanceException.getType() : analysis.getModelConclusion(),
                analysis == null ? null : analysis.getConfidenceScore(),
                analysis == null ? attendanceException.getDescription() : analysis.getDecisionReason()
        );
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

    private String appendLowConfidenceNote(String value) {
        if (!StringUtils.hasText(value)) {
            return "模型置信度低于阈值，系统已自动提升为人工复核优先事项";
        }
        return value + "；模型置信度低于阈值，系统已自动提升为人工复核优先事项";
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

    private ExceptionDecisionVO toDecisionVO(AttendanceException attendanceException,
                                             ExceptionAnalysis analysis,
                                             String sourceTypeOverride) {
        ExceptionDecisionVO vo = toDecisionVO(attendanceException, analysis);
        if (StringUtils.hasText(sourceTypeOverride)) {
            vo.setSourceType(sourceTypeOverride);
        }
        return vo;
    }

    private String detectRuleType(AttendanceRecord record, Rule rule) {
        if (isContinuousIllegalTimePattern(record, rule)) {
            return CONTINUOUS_ILLEGAL_TIME;
        }
        if (isIllegalTime(record, rule)) {
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
        if (isLate(record, rule)) {
            return "LATE";
        }
        if (isEarlyLeave(record, rule)) {
            return "EARLY_LEAVE";
        }
        if (isRepeatCheck(record, rule)) {
            return "REPEAT_CHECK";
        }
        if (isContinuousAttendanceRiskPattern(record, rule)) {
            return CONTINUOUS_ATTENDANCE_RISK;
        }
        return null;
    }

    private boolean isIllegalTime(AttendanceRecord record, Rule rule) {
        if (record == null || record.getCheckTime() == null) {
            return false;
        }
        LocalTime time = record.getCheckTime().toLocalTime();
        if (rule == null || rule.getStartTime() == null || rule.getEndTime() == null) {
            return time.isBefore(LocalTime.of(5, 0)) || time.isAfter(LocalTime.of(23, 0));
        }

        if ("IN".equals(record.getCheckType())) {
            return time.isAfter(rule.getEndTime());
        }
        if ("OUT".equals(record.getCheckType())) {
            return time.isBefore(rule.getStartTime());
        }
        return time.isBefore(LocalTime.of(5, 0)) || time.isAfter(LocalTime.of(23, 0));
    }

    private boolean isContinuousIllegalTimePattern(AttendanceRecord record, Rule rule) {
        if (!isIllegalTime(record, rule) || record.getUserId() == null || record.getCheckTime() == null) {
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
            if (!isIllegalTime(previousRecord, rule)) {
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
        if (isIllegalTime(record, rule)) {
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

    private void ensureRuleExplanation(AttendanceRecord record,
                                       Rule rule,
                                       AttendanceException attendanceException,
                                       String ruleDecisionEvidence) {
        if (!shouldUseModelAssistedRuleExplanation(attendanceException)) {
            return;
        }
        ExceptionAnalysis existingAnalysis = findLatestAnalysis(attendanceException.getId());
        if (existingAnalysis != null && StringUtils.hasText(existingAnalysis.getReasonSummary())) {
            return;
        }

        String resolvedEvidence = StringUtils.hasText(ruleDecisionEvidence)
                ? ruleDecisionEvidence
                : resolveRuleDecisionEvidence(record, rule, attendanceException.getType(), attendanceException.getDescription());
        PromptTemplate promptTemplate = promptTemplateMapper.selectEnabledBySceneType("EXCEPTION_ANALYSIS");
        if (promptTemplate == null) {
            saveRuleFallbackAnalysis(attendanceException, existingAnalysis, resolvedEvidence, null);
            return;
        }

        String inputSummary = buildRuleExplanationInputSummary(record, rule, attendanceException, resolvedEvidence);
        long startAt = System.currentTimeMillis();
        try {
            ModelInvokeResponse response = modelGateway.invoke(buildModelRequest(record, promptTemplate, inputSummary));
            saveRuleModelAnalysis(attendanceException, existingAnalysis, promptTemplate, inputSummary, resolvedEvidence, response);
            modelCallLogService.logSuccess(
                    "RULE_EXCEPTION_EXPLANATION",
                    attendanceException.getId(),
                    promptTemplate.getId(),
                    enrichLogInputSummary(inputSummary),
                    response.getRawResponse(),
                    Integer.valueOf((int) (System.currentTimeMillis() - startAt))
            );
        } catch (Exception exception) {
            saveRuleFallbackAnalysis(attendanceException, existingAnalysis, resolvedEvidence, promptTemplate);
            modelCallLogService.logFailure(
                    "RULE_EXCEPTION_EXPLANATION",
                    attendanceException.getId(),
                    promptTemplate.getId(),
                    enrichLogInputSummary(inputSummary),
                    exception.getMessage(),
                    Integer.valueOf((int) (System.currentTimeMillis() - startAt))
            );
        }
    }

    private boolean shouldUseModelAssistedRuleExplanation(AttendanceException attendanceException) {
        if (attendanceException == null || attendanceException.getId() == null) {
            return false;
        }
        if (!RULE_SOURCE.equals(attendanceException.getSourceType())) {
            return false;
        }
        return isQwenProviderEnabled();
    }

    private boolean isQwenProviderEnabled() {
        String provider = modelGatewayProperties == null ? null : modelGatewayProperties.getProvider();
        return "qwen".equalsIgnoreCase(provider == null ? "" : provider.trim());
    }

    private String buildRuleExplanationInputSummary(AttendanceRecord record,
                                                    Rule rule,
                                                    AttendanceException attendanceException,
                                                    String ruleDecisionEvidence) {
        return "当前规则结果已经确认，禁止改写异常类型和风险等级。"
                + "请仅基于已确认规则结果，为员工和管理员生成通俗易懂的中文说明摘要、处理建议和相似场景提示。"
                + "不要因为地点名称像商铺、餐饮店，或打卡时间不符合常见习惯而主观升级异常。"
                + "当前异常类型：" + safeText(attendanceException == null ? null : attendanceException.getType())
                + "；当前风险等级：" + safeText(attendanceException == null ? null : attendanceException.getRiskLevel())
                + "；规则说明：" + safeText(ruleDecisionEvidence)
                + "；打卡类型：" + formatCheckType(record == null ? null : record.getCheckType())
                + "；打卡时间：" + safeText(record == null ? null : record.getCheckTime())
                + "；规则上班时间：" + safeText(rule == null ? null : rule.getStartTime())
                + "；规则下班时间：" + safeText(rule == null ? null : rule.getEndTime())
                + "；迟到阈值分钟：" + safeText(rule == null ? null : rule.getLateThreshold())
                + "；早退阈值分钟：" + safeText(rule == null ? null : rule.getEarlyThreshold())
                + "；打卡地点：" + safeText(record == null ? null : record.getLocation())
                + "；打卡地点编号：" + safeText(record == null ? null : record.getDeviceId())
                + "；服务端人脸分数：" + safeText(record == null ? null : record.getFaceScore())
                + "。输出必须使用自然中文，适合直接展示给客户和管理员阅读。";
    }

    private void saveRuleModelAnalysis(AttendanceException attendanceException,
                                       ExceptionAnalysis existingAnalysis,
                                       PromptTemplate promptTemplate,
                                       String inputSummary,
                                       String ruleDecisionEvidence,
                                       ModelInvokeResponse response) {
        ExceptionAnalysis analysis = existingAnalysis == null ? new ExceptionAnalysis() : existingAnalysis;
        analysis.setExceptionId(attendanceException.getId());
        analysis.setPromptTemplateId(promptTemplate.getId());
        analysis.setInputSummary(inputSummary);
        analysis.setModelResult(response.getRawResponse());
        analysis.setModelConclusion(attendanceException.getType());
        analysis.setConfidenceScore(response.getConfidenceScore());
        analysis.setDecisionReason(preferNonEmpty(response.getDecisionReason(), ruleDecisionEvidence));
        analysis.setSuggestion(limitText(preferNonEmpty(response.getActionSuggestion(), buildRuleFallbackActionSuggestion(attendanceException)), 255));
        analysis.setReasonSummary(preferNonEmpty(response.getReasonSummary(), buildRuleFallbackReasonSummary(attendanceException)));
        analysis.setActionSuggestion(limitText(preferNonEmpty(response.getActionSuggestion(), buildRuleFallbackActionSuggestion(attendanceException)), 255));
        analysis.setSimilarCaseSummary(preferNonEmpty(response.getSimilarCaseSummary(), buildRuleFallbackCaseSummary(attendanceException)));
        analysis.setPromptVersion(limitText(promptTemplate.getVersion(), 50));
        persistRuleAnalysis(analysis, existingAnalysis);
    }

    private void saveRuleFallbackAnalysis(AttendanceException attendanceException,
                                          ExceptionAnalysis existingAnalysis,
                                          String ruleDecisionEvidence,
                                          PromptTemplate promptTemplate) {
        ExceptionAnalysis analysis = existingAnalysis == null ? new ExceptionAnalysis() : existingAnalysis;
        analysis.setExceptionId(attendanceException.getId());
        analysis.setPromptTemplateId(promptTemplate == null ? null : promptTemplate.getId());
        analysis.setInputSummary(ruleDecisionEvidence);
        analysis.setModelResult(null);
        analysis.setModelConclusion(attendanceException.getType());
        analysis.setConfidenceScore(null);
        analysis.setDecisionReason(ruleDecisionEvidence);
        analysis.setSuggestion(buildRuleFallbackActionSuggestion(attendanceException));
        analysis.setReasonSummary(buildRuleFallbackReasonSummary(attendanceException));
        analysis.setActionSuggestion(buildRuleFallbackActionSuggestion(attendanceException));
        analysis.setSimilarCaseSummary(buildRuleFallbackCaseSummary(attendanceException));
        analysis.setPromptVersion(promptTemplate == null ? null : limitText(promptTemplate.getVersion(), 50));
        persistRuleAnalysis(analysis, existingAnalysis);
    }

    private void persistRuleAnalysis(ExceptionAnalysis analysis, ExceptionAnalysis existingAnalysis) {
        if (existingAnalysis == null || existingAnalysis.getId() == null) {
            exceptionAnalysisMapper.insert(analysis);
            return;
        }
        analysis.setId(existingAnalysis.getId());
        exceptionAnalysisMapper.updateById(analysis);
    }

    private String buildRuleFallbackReasonSummary(AttendanceException attendanceException) {
        if (attendanceException == null) {
            return "系统已根据当前规则完成判断，请按规则结果处理。";
        }
        if ("LATE".equals(attendanceException.getType())) {
            return "本次上班打卡晚于规则时间，系统已按迟到处理。";
        }
        if ("EARLY_LEAVE".equals(attendanceException.getType())) {
            return "本次下班打卡早于规则时间，系统已按早退处理。";
        }
        if (ABSENT.equals(attendanceException.getType())) {
            return "本次在规定时段内未完成上班打卡，系统已按缺勤处理。";
        }
        if (MISSING_CHECKOUT.equals(attendanceException.getType())) {
            return "本次已上班打卡但未完成下班打卡，系统已按未打下班卡处理。";
        }
        if (MULTI_LOCATION_CONFLICT.equals(attendanceException.getType())) {
            return "本次打卡地点与前后记录差异较大，系统已按异地打卡处理。";
        }
        if ("REPEAT_CHECK".equals(attendanceException.getType())) {
            return "本次短时间内重复提交了同类打卡，系统已按重复打卡处理。";
        }
        return "系统已根据当前规则完成判断，请按规则结果处理。";
    }

    private String buildRuleFallbackActionSuggestion(AttendanceException attendanceException) {
        if (attendanceException == null) {
            return "请按当前规则结果继续处理。";
        }
        if ("LATE".equals(attendanceException.getType()) || "EARLY_LEAVE".equals(attendanceException.getType())) {
            return "建议按考勤制度记录本次情况，必要时提醒员工补充说明。";
        }
        if (ABSENT.equals(attendanceException.getType()) || MISSING_CHECKOUT.equals(attendanceException.getType())) {
            return "建议联系员工确认情况，必要时通过补卡或人工复核完成处理。";
        }
        if (MULTI_LOCATION_CONFLICT.equals(attendanceException.getType())) {
            return "建议优先核对地点、设备和当日出勤安排，再决定是否升级处理。";
        }
        return "建议按当前规则结果继续处理。";
    }

    private String buildRuleFallbackCaseSummary(AttendanceException attendanceException) {
        if (attendanceException == null) {
            return "同类规则异常通常可结合制度和出勤说明直接处理。";
        }
        if ("LATE".equals(attendanceException.getType()) || "EARLY_LEAVE".equals(attendanceException.getType())) {
            return "迟到和早退属于常见规则异常，通常结合制度记录和员工说明处理即可。";
        }
        if (ABSENT.equals(attendanceException.getType()) || MISSING_CHECKOUT.equals(attendanceException.getType())) {
            return "缺勤和未打下班卡场景通常先核实是否漏打卡，再决定是否走补卡流程。";
        }
        if (MULTI_LOCATION_CONFLICT.equals(attendanceException.getType())) {
            return "异地打卡场景通常需要结合地点配置、设备信息和时间间隔综合核实。";
        }
        return "同类规则异常通常可结合制度和出勤说明直接处理。";
    }

    private String preferNonEmpty(String preferredValue, String fallbackValue) {
        return StringUtils.hasText(preferredValue) ? preferredValue.trim() : fallbackValue;
    }

    private String buildInputSummary(AttendanceRecord record, RiskFeaturesDTO riskFeatures) {
        long historyAbnormalCount = attendanceExceptionMapper.selectCount(Wrappers.<AttendanceException>lambdaQuery()
                .eq(AttendanceException::getUserId, record.getUserId())
                .in(AttendanceException::getSourceType, "MODEL", "MODEL_FALLBACK"));
        return "记录编号：" + record.getId()
                + "；员工编号：" + record.getUserId()
                + "；打卡类型：" + formatCheckType(record.getCheckType())
                + "；打卡时间：" + record.getCheckTime()
                + "；打卡地点编号：" + safeText(record.getDeviceId())
                + "；打卡地点：" + safeText(record.getLocation())
                + "；地点经度：" + safeText(record.getLongitude())
                + "；地点纬度：" + safeText(record.getLatitude())
                + "；当前打卡地点来自系统已启用的打卡地点配置，不能仅依据地名、商户名或周边业态直接判定异常"
                + "；服务端人脸分数：" + safeText(record.getFaceScore())
                + "；客户端人脸分数：" + safeText(riskFeatures == null ? null : riskFeatures.getFaceScore())
                + "；客户端电脑设备是否变化：" + formatBoolean(riskFeatures == null ? null : riskFeatures.getDeviceChanged())
                + "；客户端打卡地点是否变化：" + formatBoolean(riskFeatures == null ? null : riskFeatures.getLocationChanged())
                + "；客户端历史复杂异常次数：" + safeText(riskFeatures == null ? null : riskFeatures.getHistoryAbnormalCount())
                + "；数据库历史复杂异常次数：" + historyAbnormalCount
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

    private AttendanceException requireExistingException(Long exceptionId) {
        AttendanceException attendanceException = exceptionId == null ? null : attendanceExceptionMapper.selectById(exceptionId);
        if (attendanceException == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "异常记录不存在");
        }
        return attendanceException;
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
