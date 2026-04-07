package com.quyong.attendance.module.attendance.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.attendance.dto.AttendanceCheckinDTO;
import com.quyong.attendance.module.attendance.dto.AttendanceListQueryDTO;
import com.quyong.attendance.module.attendance.dto.AttendanceRecordQueryDTO;
import com.quyong.attendance.module.attendance.dto.AttendanceRepairDTO;
import com.quyong.attendance.module.attendance.entity.AttendanceRecord;
import com.quyong.attendance.module.attendance.entity.AttendanceRepair;
import com.quyong.attendance.module.attendance.mapper.AttendanceRecordMapper;
import com.quyong.attendance.module.attendance.mapper.AttendanceRepairMapper;
import com.quyong.attendance.module.attendance.service.AttendanceService;
import com.quyong.attendance.module.attendance.support.AttendanceValidationSupport;
import com.quyong.attendance.module.attendance.vo.AttendanceCheckinVO;
import com.quyong.attendance.module.attendance.vo.AttendanceRecordVO;
import com.quyong.attendance.module.attendance.vo.AttendanceRepairVO;
import com.quyong.attendance.module.auth.model.AuthUser;
import com.quyong.attendance.module.device.entity.Device;
import com.quyong.attendance.module.device.support.DeviceValidationSupport;
import com.quyong.attendance.module.exceptiondetect.dto.ComplexCheckDTO;
import com.quyong.attendance.module.exceptiondetect.dto.RiskFeaturesDTO;
import com.quyong.attendance.module.exceptiondetect.dto.RuleCheckDTO;
import com.quyong.attendance.module.exceptiondetect.entity.AttendanceException;
import com.quyong.attendance.module.exceptiondetect.mapper.AttendanceExceptionMapper;
import com.quyong.attendance.module.exceptiondetect.service.ExceptionAnalysisOrchestrator;
import com.quyong.attendance.module.exceptiondetect.vo.ExceptionDecisionVO;
import com.quyong.attendance.module.face.dto.FaceVerifyDTO;
import com.quyong.attendance.module.face.service.FaceService;
import com.quyong.attendance.module.face.vo.FaceVerifyVO;
import com.quyong.attendance.module.statistics.service.OperationLogService;
import com.quyong.attendance.module.user.entity.User;
import com.quyong.attendance.module.user.support.UserValidationSupport;
import com.quyong.attendance.module.warning.service.WarningService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class AttendanceServiceImpl implements AttendanceService {

    private static final String STATUS_NORMAL = "NORMAL";
    private static final String STATUS_PENDING = "PENDING";
    private static final BigDecimal COMPLEX_CHECK_FACE_SCORE_THRESHOLD = new BigDecimal("90");

    private final AttendanceRecordMapper attendanceRecordMapper;
    private final AttendanceRepairMapper attendanceRepairMapper;
    private final AttendanceValidationSupport attendanceValidationSupport;
    private final UserValidationSupport userValidationSupport;
    private final DeviceValidationSupport deviceValidationSupport;
    private final FaceService faceService;
    private final OperationLogService operationLogService;
    private final AttendanceExceptionMapper attendanceExceptionMapper;
    private final ExceptionAnalysisOrchestrator exceptionAnalysisOrchestrator;
    private final WarningService warningService;
    private final Clock clock;

    public AttendanceServiceImpl(AttendanceRecordMapper attendanceRecordMapper,
                                 AttendanceRepairMapper attendanceRepairMapper,
                                 AttendanceValidationSupport attendanceValidationSupport,
                                 UserValidationSupport userValidationSupport,
                                 DeviceValidationSupport deviceValidationSupport,
                                 FaceService faceService,
                                 OperationLogService operationLogService,
                                 AttendanceExceptionMapper attendanceExceptionMapper,
                                 ExceptionAnalysisOrchestrator exceptionAnalysisOrchestrator,
                                 WarningService warningService,
                                 Clock clock) {
        this.attendanceRecordMapper = attendanceRecordMapper;
        this.attendanceRepairMapper = attendanceRepairMapper;
        this.attendanceValidationSupport = attendanceValidationSupport;
        this.userValidationSupport = userValidationSupport;
        this.deviceValidationSupport = deviceValidationSupport;
        this.faceService = faceService;
        this.operationLogService = operationLogService;
        this.attendanceExceptionMapper = attendanceExceptionMapper;
        this.exceptionAnalysisOrchestrator = exceptionAnalysisOrchestrator;
        this.warningService = warningService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AttendanceCheckinVO checkin(AttendanceCheckinDTO dto) {
        AttendanceCheckinDTO validatedDTO = attendanceValidationSupport.validateCheckin(dto);
        User user = userValidationSupport.requireExistingUser(validatedDTO.getUserId());
        AuthUser authUser = currentAuthUser();
        if (!isAdmin(authUser) && !authUser.getUserId().equals(validatedDTO.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权为其他用户提交打卡");
        }
        Device device = deviceValidationSupport.requireExistingDevice(validatedDTO.getDeviceId());
        if (device.getStatus() == null || device.getStatus() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "设备已停用，不能打卡");
        }

        FaceVerifyDTO faceVerifyDTO = new FaceVerifyDTO();
        faceVerifyDTO.setUserId(validatedDTO.getUserId());
        faceVerifyDTO.setImageData(validatedDTO.getImageData());
        FaceVerifyVO faceVerifyVO = faceService.verify(faceVerifyDTO);
        if (!Boolean.TRUE.equals(faceVerifyVO.getRegistered()) || !Boolean.TRUE.equals(faceVerifyVO.getMatched())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), faceVerifyVO.getMessage());
        }

        AttendanceRecord attendanceRecord = new AttendanceRecord();
        attendanceRecord.setUserId(validatedDTO.getUserId());
        attendanceRecord.setCheckTime(LocalDateTime.now(clock));
        attendanceRecord.setCheckType(validatedDTO.getCheckType());
        attendanceRecord.setDeviceId(validatedDTO.getDeviceId());
        attendanceRecord.setIpAddr(validatedDTO.getIpAddr());
        attendanceRecord.setLocation(device.getLocation());
        attendanceRecord.setLongitude(device.getLongitude());
        attendanceRecord.setLatitude(device.getLatitude());
        attendanceRecord.setFaceScore(faceVerifyVO.getFaceScore());
        attendanceRecord.setStatus(STATUS_NORMAL);
        attendanceRecordMapper.insert(attendanceRecord);

        AttendanceRecord savedRecord = attendanceRecordMapper.selectById(attendanceRecord.getId());
        RiskFeaturesDTO riskFeatures = buildRiskFeatures(savedRecord);
        runClosedLoop(savedRecord, riskFeatures);
        savedRecord = attendanceRecordMapper.selectById(attendanceRecord.getId());
        AttendanceCheckinVO vo = new AttendanceCheckinVO();
        vo.setRecordId(savedRecord.getId());
        vo.setUserId(savedRecord.getUserId());
        vo.setCheckTime(savedRecord.getCheckTime());
        vo.setCheckType(savedRecord.getCheckType());
        vo.setDeviceId(savedRecord.getDeviceId());
        vo.setLocation(savedRecord.getLocation());
        vo.setFaceScore(savedRecord.getFaceScore());
        vo.setThreshold(faceVerifyVO.getThreshold());
        vo.setStatus(savedRecord.getStatus());
        vo.setMessage("打卡成功");
        operationLogService.save(authUser.getUserId(), "CHECKIN", user.getRealName() + resolveCheckinText(savedRecord.getCheckType()));
        return vo;
    }

    @Override
    public PageResult<AttendanceRecordVO> record(Long userId, AttendanceRecordQueryDTO dto) {
        userValidationSupport.requireExistingUser(userId);
        AuthUser authUser = currentAuthUser();
        if (!isAdmin(authUser) && !authUser.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权查看其他用户考勤记录");
        }

        AttendanceRecordQueryDTO validatedDTO = attendanceValidationSupport.validateRecordQuery(dto);
        LocalDateTime startTime = attendanceValidationSupport.parseQueryStart(validatedDTO.getStartDate());
        LocalDateTime endTime = attendanceValidationSupport.parseQueryEnd(validatedDTO.getEndDate());
        int offset = (validatedDTO.getPageNum() - 1) * validatedDTO.getPageSize();
        long total = attendanceRecordMapper.countPersonalRecords(userId, startTime, endTime);
        List<AttendanceRecordVO> records = attendanceRecordMapper.selectPersonalRecords(
                userId,
                startTime,
                endTime,
                validatedDTO.getPageSize(),
                offset
        );
        return new PageResult<AttendanceRecordVO>(total, records);
    }

    @Override
    public PageResult<AttendanceRecordVO> list(AttendanceListQueryDTO dto) {
        AttendanceListQueryDTO validatedDTO = attendanceValidationSupport.validateListQuery(dto);
        LocalDateTime startTime = attendanceValidationSupport.parseQueryStart(validatedDTO.getStartDate());
        LocalDateTime endTime = attendanceValidationSupport.parseQueryEnd(validatedDTO.getEndDate());
        int offset = (validatedDTO.getPageNum() - 1) * validatedDTO.getPageSize();
        long total = attendanceRecordMapper.countAdminRecords(
                validatedDTO.getUserId(),
                validatedDTO.getDeptId(),
                validatedDTO.getCheckType(),
                validatedDTO.getStatus(),
                startTime,
                endTime
        );
        List<AttendanceRecordVO> records = attendanceRecordMapper.selectAdminRecords(
                validatedDTO.getUserId(),
                validatedDTO.getDeptId(),
                validatedDTO.getCheckType(),
                validatedDTO.getStatus(),
                startTime,
                endTime,
                validatedDTO.getPageSize(),
                offset
        );
        return new PageResult<AttendanceRecordVO>(total, records);
    }

    @Override
    public AttendanceRepairVO repair(AttendanceRepairDTO dto) {
        AttendanceRepairDTO validatedDTO = attendanceValidationSupport.validateRepair(dto);
        User user = userValidationSupport.requireExistingUser(validatedDTO.getUserId());
        AuthUser authUser = currentAuthUser();
        if (!isAdmin(authUser) && !authUser.getUserId().equals(validatedDTO.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权为其他用户提交补卡申请");
        }

        LocalDateTime checkTime = attendanceValidationSupport.parseDateTime(validatedDTO.getCheckTime(), "补卡时间");
        if (attendanceRecordMapper.countSameRecord(validatedDTO.getUserId(), validatedDTO.getCheckType(), checkTime) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "该时间点已存在打卡记录，无需补卡");
        }
        if (attendanceRepairMapper.countPendingRepair(validatedDTO.getUserId(), validatedDTO.getCheckType(), checkTime) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "补卡申请已存在，请勿重复提交");
        }

        AttendanceRepair attendanceRepair = new AttendanceRepair();
        attendanceRepair.setUserId(validatedDTO.getUserId());
        attendanceRepair.setCheckType(validatedDTO.getCheckType());
        attendanceRepair.setCheckTime(checkTime);
        attendanceRepair.setRepairReason(validatedDTO.getRepairReason());
        attendanceRepair.setStatus(STATUS_PENDING);
        try {
            attendanceRepairMapper.insert(attendanceRepair);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "补卡申请已存在，请勿重复提交");
        }

        AttendanceRepair savedRepair = attendanceRepairMapper.selectById(attendanceRepair.getId());
        AttendanceRepairVO vo = new AttendanceRepairVO();
        vo.setId(savedRepair.getId());
        vo.setUserId(savedRepair.getUserId());
        vo.setCheckType(savedRepair.getCheckType());
        vo.setCheckTime(savedRepair.getCheckTime());
        vo.setRepairReason(savedRepair.getRepairReason());
        vo.setStatus(savedRepair.getStatus());
        vo.setCreateTime(savedRepair.getCreateTime());
        operationLogService.save(authUser.getUserId(), "REPAIR", user.getRealName() + "提交补卡申请");
        return vo;
    }

    private void runClosedLoop(AttendanceRecord attendanceRecord, RiskFeaturesDTO riskFeatures) {
        RuleCheckDTO ruleCheckDTO = new RuleCheckDTO();
        ruleCheckDTO.setRecordId(attendanceRecord.getId());
        ExceptionDecisionVO ruleDecision = exceptionAnalysisOrchestrator.ruleCheck(ruleCheckDTO);
        syncWarning(ruleDecision);

        if (shouldSkipComplexCheck(ruleDecision)) {
            return;
        }

        if (!shouldTriggerComplexCheck(riskFeatures)) {
            return;
        }

        ComplexCheckDTO complexCheckDTO = new ComplexCheckDTO();
        complexCheckDTO.setRecordId(attendanceRecord.getId());
        complexCheckDTO.setUserId(attendanceRecord.getUserId());
        complexCheckDTO.setRiskFeatures(riskFeatures);
        syncWarning(exceptionAnalysisOrchestrator.complexCheck(complexCheckDTO));
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

    private RiskFeaturesDTO buildRiskFeatures(AttendanceRecord attendanceRecord) {
        RiskFeaturesDTO riskFeatures = new RiskFeaturesDTO();
        riskFeatures.setFaceScore(attendanceRecord.getFaceScore());

        AttendanceRecord previousRecord = attendanceRecordMapper.selectLatestBefore(
                attendanceRecord.getUserId(),
                attendanceRecord.getCheckTime(),
                attendanceRecord.getId()
        );
        riskFeatures.setDeviceChanged(previousRecord != null && !Objects.equals(previousRecord.getDeviceId(), attendanceRecord.getDeviceId()));
        riskFeatures.setLocationChanged(previousRecord != null && !Objects.equals(previousRecord.getLocation(), attendanceRecord.getLocation()));

        Long historyAbnormalCount = attendanceExceptionMapper.selectCount(Wrappers.<AttendanceException>lambdaQuery()
                .eq(AttendanceException::getUserId, attendanceRecord.getUserId()));
        riskFeatures.setHistoryAbnormalCount(historyAbnormalCount == null ? 0 : historyAbnormalCount.intValue());
        return riskFeatures;
    }

    private boolean shouldTriggerComplexCheck(RiskFeaturesDTO riskFeatures) {
        if (riskFeatures == null) {
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

    private String resolveCheckinText(String checkType) {
        if ("OUT".equals(checkType)) {
            return "完成下班打卡";
        }
        return "完成上班打卡";
    }

    private AuthUser currentAuthUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
        }
        return (AuthUser) authentication.getPrincipal();
    }

    private boolean isAdmin(AuthUser authUser) {
        return authUser != null && "ADMIN".equals(authUser.getRoleCode());
    }
}
