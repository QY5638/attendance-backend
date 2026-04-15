package com.quyong.attendance.module.attendance.service.impl;

import com.quyong.attendance.module.attendance.entity.AttendanceRecord;
import com.quyong.attendance.module.device.entity.Device;
import com.quyong.attendance.module.device.mapper.DeviceMapper;
import com.quyong.attendance.module.exceptiondetect.dto.ComplexCheckDTO;
import com.quyong.attendance.module.exceptiondetect.dto.RiskFeaturesDTO;
import com.quyong.attendance.module.exceptiondetect.service.ExceptionAnalysisOrchestrator;
import com.quyong.attendance.module.exceptiondetect.vo.ExceptionDecisionVO;
import com.quyong.attendance.module.face.config.FaceEngineProperties;
import com.quyong.attendance.module.warning.service.WarningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AttendanceClosedLoopAsyncService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceClosedLoopAsyncService.class);
    private static final BigDecimal COMPLEX_CHECK_FACE_SCORE_THRESHOLD = new BigDecimal("90.00");

    private final ExceptionAnalysisOrchestrator exceptionAnalysisOrchestrator;
    private final WarningService warningService;
    private final DeviceMapper deviceMapper;
    private final FaceEngineProperties faceEngineProperties;

    public AttendanceClosedLoopAsyncService(ExceptionAnalysisOrchestrator exceptionAnalysisOrchestrator,
                                            WarningService warningService,
                                            DeviceMapper deviceMapper,
                                            FaceEngineProperties faceEngineProperties) {
        this.exceptionAnalysisOrchestrator = exceptionAnalysisOrchestrator;
        this.warningService = warningService;
        this.deviceMapper = deviceMapper;
        this.faceEngineProperties = faceEngineProperties;
    }

    @Async("attendanceClosedLoopExecutor")
    public void runClosedLoop(AttendanceRecord attendanceRecord,
                              RiskFeaturesDTO riskFeatures,
                              ExceptionDecisionVO ruleDecision) {
        if (attendanceRecord == null || attendanceRecord.getId() == null) {
            return;
        }
        try {
            if (shouldSkipComplexCheck(ruleDecision)) {
                return;
            }

            if (!shouldTriggerComplexCheck(attendanceRecord, ruleDecision, riskFeatures)) {
                return;
            }

            ComplexCheckDTO complexCheckDTO = new ComplexCheckDTO();
            complexCheckDTO.setRecordId(attendanceRecord.getId());
            complexCheckDTO.setUserId(attendanceRecord.getUserId());
            complexCheckDTO.setRiskFeatures(riskFeatures);
            syncWarning(exceptionAnalysisOrchestrator.complexCheck(complexCheckDTO));
        } catch (RuntimeException exception) {
            log.warn("attendance closed-loop async failed for record {}: {}",
                    attendanceRecord.getId(),
                    exception.getMessage(),
                    exception);
        }
    }

    private boolean shouldSkipComplexCheck(ExceptionDecisionVO ruleDecision) {
        return ruleDecision != null && "MULTI_LOCATION_CONFLICT".equals(ruleDecision.getType());
    }

    private void syncWarning(ExceptionDecisionVO decisionVO) {
        if (decisionVO == null || decisionVO.getExceptionId() == null) {
            return;
        }
        warningService.syncWarningByExceptionId(decisionVO.getExceptionId());
    }

    private boolean shouldTriggerComplexCheck(AttendanceRecord attendanceRecord,
                                              ExceptionDecisionVO ruleDecision,
                                              RiskFeaturesDTO riskFeatures) {
        if (riskFeatures == null) {
            return false;
        }
        if (shouldBypassComplexCheckForConfiguredPunch(attendanceRecord, ruleDecision, riskFeatures)) {
            return false;
        }
        if (Boolean.TRUE.equals(riskFeatures.getDeviceChanged()) || Boolean.TRUE.equals(riskFeatures.getLocationChanged())) {
            return true;
        }
        if (riskFeatures.getHistoryAbnormalCount() != null && riskFeatures.getHistoryAbnormalCount().intValue() > 0) {
            return true;
        }
        return riskFeatures.getFaceScore() != null
                && riskFeatures.getFaceScore().compareTo(COMPLEX_CHECK_FACE_SCORE_THRESHOLD) < 0;
    }

    private boolean shouldBypassComplexCheckForConfiguredPunch(AttendanceRecord attendanceRecord,
                                                               ExceptionDecisionVO ruleDecision,
                                                               RiskFeaturesDTO riskFeatures) {
        if (!isEnabledConfiguredDevice(attendanceRecord)) {
            return false;
        }
        BigDecimal faceScore = riskFeatures.getFaceScore();
        BigDecimal matchThreshold = faceEngineProperties == null ? null : faceEngineProperties.getMatchScoreThreshold();
        if (faceScore == null || matchThreshold == null || faceScore.compareTo(matchThreshold) < 0) {
            return false;
        }
        return true;
    }

    private boolean isEnabledConfiguredDevice(AttendanceRecord attendanceRecord) {
        if (attendanceRecord == null || attendanceRecord.getDeviceId() == null) {
            return false;
        }
        Device device = deviceMapper.selectById(attendanceRecord.getDeviceId());
        return device != null && Integer.valueOf(1).equals(device.getStatus());
    }
}
