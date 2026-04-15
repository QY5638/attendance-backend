package com.quyong.attendance.module.attendance.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.attendance.dto.AttendanceCheckinDTO;
import com.quyong.attendance.module.attendance.dto.AttendanceListQueryDTO;
import com.quyong.attendance.module.attendance.dto.AttendanceRecordQueryDTO;
import com.quyong.attendance.module.attendance.dto.AttendanceRepairQueryDTO;
import com.quyong.attendance.module.attendance.dto.AttendanceRepairDTO;
import com.quyong.attendance.module.attendance.dto.AttendanceRepairReviewDTO;
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
import com.quyong.attendance.module.device.mapper.DeviceMapper;
import com.quyong.attendance.module.device.support.DeviceValidationSupport;
import com.quyong.attendance.module.exceptiondetect.dto.RiskFeaturesDTO;
import com.quyong.attendance.module.exceptiondetect.dto.RuleCheckDTO;
import com.quyong.attendance.module.exceptiondetect.entity.AttendanceException;
import com.quyong.attendance.module.exceptiondetect.mapper.AttendanceExceptionMapper;
import com.quyong.attendance.module.exceptiondetect.service.ExceptionAnalysisOrchestrator;
import com.quyong.attendance.module.exceptiondetect.vo.ExceptionDecisionVO;
import com.quyong.attendance.module.face.dto.FaceVerifyDTO;
import com.quyong.attendance.module.face.service.FaceService;
import com.quyong.attendance.module.face.vo.FaceVerifyVO;
import com.quyong.attendance.module.map.service.MapDistanceService;
import com.quyong.attendance.module.notification.dto.NotificationCreateCommand;
import com.quyong.attendance.module.notification.service.NotificationService;
import com.quyong.attendance.module.statistics.service.OperationLogService;
import com.quyong.attendance.module.user.entity.User;
import com.quyong.attendance.module.user.support.UserValidationSupport;
import com.quyong.attendance.module.warning.service.WarningService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Service
public class AttendanceServiceImpl implements AttendanceService {

    private static final String STATUS_NORMAL = "NORMAL";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_REPAIR_APPROVED = "APPROVED";
    private static final String STATUS_REPAIR_REJECTED = "REJECTED";
    private static final String STATUS_REPAIRED = "REPAIRED";
    private static final String EXCEPTION_STATUS_REVIEWED = "REVIEWED";
    private static final String BUSINESS_TYPE_ATTENDANCE_REPAIR = "ATTENDANCE_REPAIR";
    private static final String CATEGORY_REPAIR_RESULT = "REPAIR_RESULT";
    private static final String ACTION_VIEW = "VIEW";
    private static final String TYPE_ABSENT = "ABSENT";
    private static final String TYPE_MISSING_CHECKOUT = "MISSING_CHECKOUT";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AttendanceRecordMapper attendanceRecordMapper;
    private final AttendanceRepairMapper attendanceRepairMapper;
    private final DeviceMapper deviceMapper;
    private final AttendanceValidationSupport attendanceValidationSupport;
    private final UserValidationSupport userValidationSupport;
    private final DeviceValidationSupport deviceValidationSupport;
    private final FaceService faceService;
    private final AttendanceClosedLoopAsyncService attendanceClosedLoopAsyncService;
    private final OperationLogService operationLogService;
    private final AttendanceExceptionMapper attendanceExceptionMapper;
    private final ExceptionAnalysisOrchestrator exceptionAnalysisOrchestrator;
    private final WarningService warningService;
    private final NotificationService notificationService;
    private final MapDistanceService mapDistanceService;
    private final Clock clock;

    public AttendanceServiceImpl(AttendanceRecordMapper attendanceRecordMapper,
                                 AttendanceRepairMapper attendanceRepairMapper,
                                 DeviceMapper deviceMapper,
                                 AttendanceValidationSupport attendanceValidationSupport,
                                 UserValidationSupport userValidationSupport,
                                 DeviceValidationSupport deviceValidationSupport,
                                 FaceService faceService,
                                 AttendanceClosedLoopAsyncService attendanceClosedLoopAsyncService,
                                 OperationLogService operationLogService,
                                 AttendanceExceptionMapper attendanceExceptionMapper,
                                 ExceptionAnalysisOrchestrator exceptionAnalysisOrchestrator,
                                 WarningService warningService,
                                 NotificationService notificationService,
                                 MapDistanceService mapDistanceService,
                                 Clock clock) {
        this.attendanceRecordMapper = attendanceRecordMapper;
        this.attendanceRepairMapper = attendanceRepairMapper;
        this.deviceMapper = deviceMapper;
        this.attendanceValidationSupport = attendanceValidationSupport;
        this.userValidationSupport = userValidationSupport;
        this.deviceValidationSupport = deviceValidationSupport;
        this.faceService = faceService;
        this.attendanceClosedLoopAsyncService = attendanceClosedLoopAsyncService;
        this.operationLogService = operationLogService;
        this.attendanceExceptionMapper = attendanceExceptionMapper;
        this.exceptionAnalysisOrchestrator = exceptionAnalysisOrchestrator;
        this.warningService = warningService;
        this.notificationService = notificationService;
        this.mapDistanceService = mapDistanceService;
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
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "打卡地点已停用，不能打卡");
        }
        validatePunchRange(device, validatedDTO);

        FaceVerifyDTO faceVerifyDTO = new FaceVerifyDTO();
        faceVerifyDTO.setUserId(validatedDTO.getUserId());
        faceVerifyDTO.setImageData(validatedDTO.getImageData());
        faceVerifyDTO.setLivenessToken(validatedDTO.getLivenessToken());
        faceVerifyDTO.setConsumeLiveness(Boolean.TRUE);
        FaceVerifyVO faceVerifyVO = faceService.verify(faceVerifyDTO);
        if (!Boolean.TRUE.equals(faceVerifyVO.getRegistered()) || !Boolean.TRUE.equals(faceVerifyVO.getMatched())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), faceVerifyVO.getMessage());
        }

        AttendanceRecord attendanceRecord = new AttendanceRecord();
        attendanceRecord.setUserId(validatedDTO.getUserId());
        attendanceRecord.setCheckTime(LocalDateTime.now(clock));
        attendanceRecord.setCheckType(validatedDTO.getCheckType());
        attendanceRecord.setDeviceId(validatedDTO.getDeviceId());
        attendanceRecord.setDeviceInfo(resolveComputerDeviceInfo(validatedDTO));
        attendanceRecord.setTerminalId(resolveTerminalId(validatedDTO));
        attendanceRecord.setIpAddr(validatedDTO.getIpAddr());
        attendanceRecord.setLocation(resolvePunchLocation(device));
        attendanceRecord.setClientLongitude(validatedDTO.getClientLongitude());
        attendanceRecord.setClientLatitude(validatedDTO.getClientLatitude());
        attendanceRecord.setLongitude(device.getLongitude());
        attendanceRecord.setLatitude(device.getLatitude());
        attendanceRecord.setFaceScore(faceVerifyVO.getFaceScore());
        attendanceRecord.setStatus(STATUS_NORMAL);
        attendanceRecordMapper.insert(attendanceRecord);

        AttendanceRecord savedRecord = attendanceRecordMapper.selectById(attendanceRecord.getId());
        RiskFeaturesDTO riskFeatures = buildRiskFeatures(savedRecord);
        RuleCheckDTO ruleCheckDTO = new RuleCheckDTO();
        ruleCheckDTO.setRecordId(savedRecord.getId());
        ExceptionDecisionVO ruleDecision = exceptionAnalysisOrchestrator.ruleCheck(ruleCheckDTO);
        if (ruleDecision != null && ruleDecision.getExceptionId() != null) {
            warningService.syncWarningByExceptionId(ruleDecision.getExceptionId());
        }
        attendanceClosedLoopAsyncService.runClosedLoop(savedRecord, riskFeatures, ruleDecision);
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
        Long sourceRecordId = resolveRepairSourceRecordId(validatedDTO, user);
        Long sameRecordId = attendanceRecordMapper.selectSameRecordId(validatedDTO.getUserId(), validatedDTO.getCheckType(), checkTime);
        if (sameRecordId != null && (sourceRecordId == null || !sameRecordId.equals(sourceRecordId))) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "该时间点已存在打卡记录，无需补卡");
        }
        if (sourceRecordId != null && attendanceRepairMapper.countPendingByRecordId(sourceRecordId) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "该异常记录已有待处理补卡申请，请勿重复提交");
        }
        if (attendanceRepairMapper.countPendingRepair(validatedDTO.getUserId(), validatedDTO.getCheckType(), checkTime) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "补卡申请已存在，请勿重复提交");
        }

        AttendanceRepair attendanceRepair = new AttendanceRepair();
        attendanceRepair.setUserId(validatedDTO.getUserId());
        attendanceRepair.setCheckType(validatedDTO.getCheckType());
        attendanceRepair.setCheckTime(checkTime);
        attendanceRepair.setRepairReason(validatedDTO.getRepairReason());
        attendanceRepair.setRecordId(sourceRecordId);
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
        vo.setRecordId(savedRepair.getRecordId());
        vo.setCreateTime(savedRepair.getCreateTime());
        operationLogService.save(authUser.getUserId(), "REPAIR", user.getRealName() + "提交补卡申请");
        return vo;
    }

    @Override
    public PageResult<AttendanceRepairVO> repairList(AttendanceRepairQueryDTO dto) {
        AttendanceRepairQueryDTO validatedDTO = attendanceValidationSupport.validateRepairQuery(dto);
        LocalDateTime startTime = attendanceValidationSupport.parseQueryStart(validatedDTO.getStartDate());
        LocalDateTime endTime = attendanceValidationSupport.parseQueryEnd(validatedDTO.getEndDate());
        int offset = (validatedDTO.getPageNum().intValue() - 1) * validatedDTO.getPageSize().intValue();
        long total = attendanceRepairMapper.countByQuery(
                validatedDTO.getKeyword(),
                validatedDTO.getUserId(),
                validatedDTO.getDeptId(),
                validatedDTO.getCheckType(),
                validatedDTO.getStatus(),
                startTime,
                endTime
        );
        List<AttendanceRepairVO> records = attendanceRepairMapper.selectPageByQuery(
                validatedDTO.getKeyword(),
                validatedDTO.getUserId(),
                validatedDTO.getDeptId(),
                validatedDTO.getCheckType(),
                validatedDTO.getStatus(),
                startTime,
                endTime,
                validatedDTO.getPageSize().intValue(),
                offset
        );
        return new PageResult<AttendanceRepairVO>(Long.valueOf(total), records);
    }

    @Override
    @Transactional
    public AttendanceRepairVO reviewRepair(Long repairId, AttendanceRepairReviewDTO dto) {
        if (repairId == null || repairId.longValue() <= 0L) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "补卡申请不存在");
        }
        AuthUser authUser = currentAuthUser();
        if (!isAdmin(authUser)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), ResultCode.FORBIDDEN.getMessage());
        }
        AttendanceRepairReviewDTO validatedDTO = attendanceValidationSupport.validateRepairReview(dto);
        AttendanceRepair attendanceRepair = attendanceRepairMapper.selectByIdForUpdate(repairId);
        if (attendanceRepair == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "补卡申请不存在");
        }
        if (!STATUS_PENDING.equals(attendanceRepair.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "该补卡申请已处理，请勿重复操作");
        }

        if (STATUS_REPAIR_APPROVED.equals(validatedDTO.getStatus())) {
            approveRepair(attendanceRepair);
        } else {
            attendanceRepair.setStatus(STATUS_REPAIR_REJECTED);
        }
        attendanceRepairMapper.updateById(attendanceRepair);

        User employee = userValidationSupport.requireExistingUser(attendanceRepair.getUserId());
        notificationService.push(buildRepairResultNotification(
                employee.getId(),
                authUser.getUserId(),
                attendanceRepair,
                validatedDTO.getReviewComment()
        ));
        operationLogService.save(
                authUser.getUserId(),
                "REPAIR_REVIEW",
                authUser.getRealName() + (STATUS_REPAIR_APPROVED.equals(validatedDTO.getStatus()) ? "通过" : "驳回") + employee.getRealName() + "的补卡申请"
        );

        AttendanceRepairVO vo = new AttendanceRepairVO();
        vo.setId(attendanceRepair.getId());
        vo.setUserId(attendanceRepair.getUserId());
        vo.setRealName(employee.getRealName());
        vo.setCheckType(attendanceRepair.getCheckType());
        vo.setCheckTime(attendanceRepair.getCheckTime());
        vo.setRepairReason(attendanceRepair.getRepairReason());
        vo.setStatus(attendanceRepair.getStatus());
        vo.setRecordId(attendanceRepair.getRecordId());
        vo.setCreateTime(attendanceRepair.getCreateTime());
        return vo;
    }

    private RiskFeaturesDTO buildRiskFeatures(AttendanceRecord attendanceRecord) {
        RiskFeaturesDTO riskFeatures = new RiskFeaturesDTO();
        riskFeatures.setFaceScore(attendanceRecord.getFaceScore());

        AttendanceRecord previousRecord = attendanceRecordMapper.selectLatestBefore(
                attendanceRecord.getUserId(),
                attendanceRecord.getCheckTime(),
                attendanceRecord.getId()
        );
        riskFeatures.setDeviceChanged(previousRecord != null && !Objects.equals(previousRecord.getTerminalId(), attendanceRecord.getTerminalId()));
        riskFeatures.setLocationChanged(previousRecord != null && !Objects.equals(previousRecord.getLocation(), attendanceRecord.getLocation()));

        Long historyAbnormalCount = attendanceExceptionMapper.selectCount(Wrappers.<AttendanceException>lambdaQuery()
                .eq(AttendanceException::getUserId, attendanceRecord.getUserId())
                .in(AttendanceException::getSourceType, "MODEL", "MODEL_FALLBACK"));
        riskFeatures.setHistoryAbnormalCount(historyAbnormalCount == null ? 0 : historyAbnormalCount.intValue());
        return riskFeatures;
    }

    private Long resolveRepairSourceRecordId(AttendanceRepairDTO validatedDTO, User user) {
        Long sourceRecordId = validatedDTO == null ? null : validatedDTO.getRecordId();
        if (sourceRecordId == null) {
            return null;
        }
        if (sourceRecordId.longValue() <= 0L) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "补卡申请关联记录不合法");
        }

        AttendanceRecord sourceRecord = attendanceRecordMapper.selectById(sourceRecordId);
        if (sourceRecord == null) {
            AttendanceException contextException = attendanceExceptionMapper.selectById(sourceRecordId);
            if (isRepairableContextException(contextException, validatedDTO)) {
                return null;
            }
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "补卡申请关联记录不存在");
        }
        if (!sourceRecord.getUserId().equals(validatedDTO.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权关联其他员工的考勤记录");
        }
        if (!isRepairableStatus(sourceRecord.getStatus())) {
            String realName = user == null ? "当前员工" : user.getRealName();
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), realName + "的该条记录当前无需补卡");
        }
        return sourceRecordId;
    }

    private boolean isRepairableContextException(AttendanceException attendanceException, AttendanceRepairDTO validatedDTO) {
        if (attendanceException == null || validatedDTO == null) {
            return false;
        }
        if (attendanceException.getRecordId() != null || !Objects.equals(attendanceException.getUserId(), validatedDTO.getUserId())) {
            return false;
        }
        if ("IN".equals(validatedDTO.getCheckType())) {
            return TYPE_ABSENT.equals(attendanceException.getType());
        }
        if ("OUT".equals(validatedDTO.getCheckType())) {
            return TYPE_MISSING_CHECKOUT.equals(attendanceException.getType());
        }
        return false;
    }

    private boolean isRepairableStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        return "ABNORMAL".equals(normalized)
                || "ABSENT".equals(normalized)
                || TYPE_MISSING_CHECKOUT.equals(normalized)
                || "LATE".equals(normalized)
                || "EARLY_LEAVE".equals(normalized);
    }

    private void approveRepair(AttendanceRepair attendanceRepair) {
        if (attendanceRepair.getRecordId() != null) {
            approveLinkedRepair(attendanceRepair);
            return;
        }

        Long sameRecordId = attendanceRecordMapper.selectSameRecordId(
                attendanceRepair.getUserId(),
                attendanceRepair.getCheckType(),
                attendanceRepair.getCheckTime()
        );
        if (sameRecordId != null) {
            attendanceRepair.setRecordId(sameRecordId);
            attendanceRepair.setStatus(STATUS_REPAIR_APPROVED);
            return;
        }

        AttendanceRecord latestRecord = attendanceRecordMapper.selectLatestByUser(attendanceRepair.getUserId());
        Device latestDevice = null;
        if (latestRecord != null && StringUtils.hasText(latestRecord.getDeviceId())) {
            latestDevice = deviceMapper.selectById(latestRecord.getDeviceId().trim());
        }
        Device fallbackDevice = deviceMapper.selectOne(Wrappers.<Device>lambdaQuery()
                .eq(Device::getStatus, Integer.valueOf(1))
                .orderByAsc(Device::getId)
                .last("LIMIT 1"));
        Device sourceDevice = latestDevice == null ? fallbackDevice : latestDevice;
        if (sourceDevice == null || !StringUtils.hasText(sourceDevice.getId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "系统未配置可用打卡地点，当前无法通过该补卡申请");
        }

        AttendanceRecord attendanceRecord = new AttendanceRecord();
        attendanceRecord.setUserId(attendanceRepair.getUserId());
        attendanceRecord.setCheckTime(attendanceRepair.getCheckTime());
        attendanceRecord.setCheckType(attendanceRepair.getCheckType());
        attendanceRecord.setDeviceId(resolveRepairDeviceId(sourceDevice));
        attendanceRecord.setDeviceInfo(resolveRepairDeviceInfo(latestRecord));
        attendanceRecord.setTerminalId(latestRecord == null ? null : latestRecord.getTerminalId());
        attendanceRecord.setIpAddr(latestRecord == null ? null : latestRecord.getIpAddr());
        attendanceRecord.setLocation(resolveRepairLocation(latestRecord, sourceDevice));
        attendanceRecord.setClientLongitude(latestRecord == null ? null : latestRecord.getClientLongitude());
        attendanceRecord.setClientLatitude(latestRecord == null ? null : latestRecord.getClientLatitude());
        attendanceRecord.setLongitude(resolveRepairLongitude(latestRecord, sourceDevice));
        attendanceRecord.setLatitude(resolveRepairLatitude(latestRecord, sourceDevice));
        attendanceRecord.setFaceScore(null);
        attendanceRecord.setStatus(STATUS_REPAIRED);
        attendanceRecordMapper.insert(attendanceRecord);

        attendanceRepair.setRecordId(attendanceRecord.getId());
        attendanceRepair.setStatus(STATUS_REPAIR_APPROVED);
        markSameDayContextExceptionsReviewed(attendanceRepair);
    }

    private void approveLinkedRepair(AttendanceRepair attendanceRepair) {
        AttendanceRecord sourceRecord = attendanceRecordMapper.selectById(attendanceRepair.getRecordId());
        if (sourceRecord == null || !Objects.equals(sourceRecord.getUserId(), attendanceRepair.getUserId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "补卡申请关联记录不存在，当前无法通过");
        }

        Long sameRecordId = attendanceRecordMapper.selectSameRecordId(
                attendanceRepair.getUserId(),
                attendanceRepair.getCheckType(),
                attendanceRepair.getCheckTime()
        );
        if (sameRecordId != null && !sameRecordId.equals(sourceRecord.getId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "该时间点已有其他打卡记录，当前无法通过该补卡申请");
        }

        sourceRecord.setCheckType(attendanceRepair.getCheckType());
        sourceRecord.setCheckTime(attendanceRepair.getCheckTime());
        sourceRecord.setStatus(STATUS_NORMAL);
        attendanceRecordMapper.updateById(sourceRecord);
        markRecordExceptionsReviewed(sourceRecord.getId());
        attendanceRepair.setRecordId(sourceRecord.getId());
        attendanceRepair.setStatus(STATUS_REPAIR_APPROVED);
        markSameDayContextExceptionsReviewed(attendanceRepair);
    }

    private void markRecordExceptionsReviewed(Long recordId) {
        if (recordId == null) {
            return;
        }
        List<AttendanceException> exceptions = attendanceExceptionMapper.selectList(Wrappers.<AttendanceException>lambdaQuery()
                .eq(AttendanceException::getRecordId, recordId)
                .ne(AttendanceException::getProcessStatus, EXCEPTION_STATUS_REVIEWED));
        markExceptionsReviewed(exceptions);
    }

    private void markSameDayContextExceptionsReviewed(AttendanceRepair attendanceRepair) {
        if (attendanceRepair == null
                || attendanceRepair.getUserId() == null
                || attendanceRepair.getCheckTime() == null) {
            return;
        }
        String contextExceptionType = resolveContextExceptionType(attendanceRepair.getCheckType());
        if (!StringUtils.hasText(contextExceptionType)) {
            return;
        }
        LocalDate targetDay = attendanceRepair.getCheckTime().toLocalDate();
        LocalDateTime dayStart = targetDay.atStartOfDay();
        LocalDateTime nextDayStart = targetDay.plusDays(1L).atStartOfDay();
        List<AttendanceException> exceptions = attendanceExceptionMapper.selectList(Wrappers.<AttendanceException>lambdaQuery()
                .eq(AttendanceException::getUserId, attendanceRepair.getUserId())
                .eq(AttendanceException::getType, contextExceptionType)
                .isNull(AttendanceException::getRecordId)
                .ge(AttendanceException::getCreateTime, dayStart)
                .lt(AttendanceException::getCreateTime, nextDayStart)
                .ne(AttendanceException::getProcessStatus, EXCEPTION_STATUS_REVIEWED));
        markExceptionsReviewed(exceptions);
    }

    private String resolveContextExceptionType(String checkType) {
        if ("IN".equals(checkType)) {
            return TYPE_ABSENT;
        }
        if ("OUT".equals(checkType)) {
            return TYPE_MISSING_CHECKOUT;
        }
        return null;
    }

    private void markExceptionsReviewed(List<AttendanceException> exceptions) {
        if (exceptions == null || exceptions.isEmpty()) {
            return;
        }
        for (AttendanceException attendanceException : exceptions) {
            attendanceException.setProcessStatus(EXCEPTION_STATUS_REVIEWED);
            attendanceExceptionMapper.updateById(attendanceException);
            warningService.markProcessedByExceptionId(attendanceException.getId());
        }
    }

    private NotificationCreateCommand buildRepairResultNotification(Long recipientUserId,
                                                                    Long senderUserId,
                                                                    AttendanceRepair attendanceRepair,
                                                                    String reviewComment) {
        String checkTypeLabel = "OUT".equals(attendanceRepair.getCheckType()) ? "下班打卡" : "上班打卡";
        String checkTimeText = attendanceRepair.getCheckTime() == null ? "--" : DATE_TIME_FORMATTER.format(attendanceRepair.getCheckTime());
        StringBuilder builder = new StringBuilder();
        if (STATUS_REPAIR_APPROVED.equals(attendanceRepair.getStatus())) {
            builder.append("你的补卡申请已通过：").append(checkTypeLabel).append(" ").append(checkTimeText);
        } else {
            builder.append("你的补卡申请未通过：").append(checkTypeLabel).append(" ").append(checkTimeText);
        }
        if (StringUtils.hasText(reviewComment)) {
            builder.append("；处理说明：").append(reviewComment.trim());
        }

        NotificationCreateCommand command = new NotificationCreateCommand();
        command.setRecipientUserId(recipientUserId);
        command.setSenderUserId(senderUserId);
        command.setBusinessType(BUSINESS_TYPE_ATTENDANCE_REPAIR);
        command.setBusinessId(attendanceRepair.getId());
        command.setCategory(CATEGORY_REPAIR_RESULT);
        command.setTitle("补卡申请处理结果");
        command.setContent(limitText(builder.toString(), 1000));
        command.setLevel(STATUS_REPAIR_APPROVED.equals(attendanceRepair.getStatus()) ? "INFO" : "MEDIUM");
        command.setActionCode(ACTION_VIEW);
        return command;
    }

    private String resolveRepairDeviceId(Device sourceDevice) {
        return sourceDevice == null ? null : sourceDevice.getId();
    }

    private String resolveRepairDeviceInfo(AttendanceRecord latestRecord) {
        if (latestRecord != null && StringUtils.hasText(latestRecord.getDeviceInfo())) {
            return latestRecord.getDeviceInfo().trim();
        }
        return "补卡审批录入";
    }

    private String resolveRepairLocation(AttendanceRecord latestRecord, Device sourceDevice) {
        if (latestRecord != null && StringUtils.hasText(latestRecord.getLocation())) {
            return latestRecord.getLocation().trim();
        }
        if (sourceDevice == null) {
            return "未配置地点";
        }
        if (StringUtils.hasText(sourceDevice.getLocation())) {
            return sourceDevice.getLocation().trim();
        }
        if (StringUtils.hasText(sourceDevice.getName())) {
            return sourceDevice.getName().trim();
        }
        return sourceDevice.getId();
    }

    private BigDecimal resolveRepairLongitude(AttendanceRecord latestRecord, Device sourceDevice) {
        if (latestRecord != null && latestRecord.getLongitude() != null) {
            return latestRecord.getLongitude();
        }
        return sourceDevice == null ? null : sourceDevice.getLongitude();
    }

    private BigDecimal resolveRepairLatitude(AttendanceRecord latestRecord, Device sourceDevice) {
        if (latestRecord != null && latestRecord.getLatitude() != null) {
            return latestRecord.getLatitude();
        }
        return sourceDevice == null ? null : sourceDevice.getLatitude();
    }

    private String resolveCheckinText(String checkType) {
        if ("OUT".equals(checkType)) {
            return "完成下班打卡";
        }
        return "完成上班打卡";
    }

    private String resolveComputerDeviceInfo(AttendanceCheckinDTO dto) {
        if (dto != null && StringUtils.hasText(dto.getDeviceInfo())) {
            return limitText(dto.getDeviceInfo().trim(), 120);
        }
        if (dto != null && StringUtils.hasText(dto.getIpAddr())) {
            return "网页端电脑（" + dto.getIpAddr().trim() + "）";
        }
        return "网页端电脑";
    }

    private String resolvePunchLocation(Device device) {
        if (device == null) {
            return "未配置地点";
        }
        if (StringUtils.hasText(device.getLocation())) {
            return device.getLocation().trim();
        }
        if (StringUtils.hasText(device.getName())) {
            return device.getName().trim();
        }
        return device.getId();
    }

    private String resolveTerminalId(AttendanceCheckinDTO dto) {
        if (dto != null && StringUtils.hasText(dto.getTerminalId())) {
            return limitText(dto.getTerminalId().trim(), 64);
        }
        return null;
    }

    private void validatePunchRange(Device device, AttendanceCheckinDTO dto) {
        if (device.getLongitude() == null || device.getLatitude() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前打卡地点未完成位置设置，请联系管理员");
        }

        BigDecimal distanceMeters = mapDistanceService.calculateDistanceMeters(
                dto.getClientLongitude(),
                dto.getClientLatitude(),
                device.getLongitude(),
                device.getLatitude()
        );

        int radiusMeters = device.getRadiusMeters() == null ? 30 : device.getRadiusMeters().intValue();
        if (distanceMeters.compareTo(BigDecimal.valueOf(radiusMeters)) > 0) {
            throw new BusinessException(
                    ResultCode.BAD_REQUEST.getCode(),
                    "当前位置不在打卡地点允许范围内，请进入“" + resolvePunchLocation(device) + "”附近 " + radiusMeters + " 米范围后再打卡"
            );
        }
    }

    private String limitText(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
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
