package com.quyong.attendance.module.face.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.department.entity.Department;
import com.quyong.attendance.module.department.mapper.DepartmentMapper;
import com.quyong.attendance.module.face.dto.FaceRegisterApplyDTO;
import com.quyong.attendance.module.face.dto.FaceRegisterApprovalQueryDTO;
import com.quyong.attendance.module.face.dto.FaceRegisterApprovalReviewDTO;
import com.quyong.attendance.module.face.entity.FaceFeature;
import com.quyong.attendance.module.face.entity.FaceRegisterApproval;
import com.quyong.attendance.module.face.mapper.FaceFeatureMapper;
import com.quyong.attendance.module.face.mapper.FaceRegisterApprovalMapper;
import com.quyong.attendance.module.face.service.FaceRegisterApprovalService;
import com.quyong.attendance.module.face.vo.FaceRegisterApprovalVO;
import com.quyong.attendance.module.face.vo.FaceRegisterStatusVO;
import com.quyong.attendance.module.statistics.service.OperationLogService;
import com.quyong.attendance.module.user.entity.User;
import com.quyong.attendance.module.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class FaceRegisterApprovalServiceImpl implements FaceRegisterApprovalService {

    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private static final String STATUS_NONE = "NONE";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_USED = "USED";

    private final FaceFeatureMapper faceFeatureMapper;
    private final FaceRegisterApprovalMapper faceRegisterApprovalMapper;
    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;
    private final OperationLogService operationLogService;

    public FaceRegisterApprovalServiceImpl(FaceFeatureMapper faceFeatureMapper,
                                           FaceRegisterApprovalMapper faceRegisterApprovalMapper,
                                           UserMapper userMapper,
                                           DepartmentMapper departmentMapper,
                                           OperationLogService operationLogService) {
        this.faceFeatureMapper = faceFeatureMapper;
        this.faceRegisterApprovalMapper = faceRegisterApprovalMapper;
        this.userMapper = userMapper;
        this.departmentMapper = departmentMapper;
        this.operationLogService = operationLogService;
    }

    @Override
    public FaceRegisterStatusVO getStatus(Long userId) {
        Long safeUserId = requireExistingUser(userId);
        FaceFeature latestFaceFeature = faceFeatureMapper.selectLatestByUserId(safeUserId);
        FaceRegisterApproval latestApproval = selectLatestApproval(safeUserId);

        FaceRegisterStatusVO vo = new FaceRegisterStatusVO();
        vo.setUserId(safeUserId);
        vo.setRegistered(Boolean.valueOf(latestFaceFeature != null));
        vo.setRequiresApproval(Boolean.valueOf(latestFaceFeature != null));

        if (latestApproval != null) {
            vo.setStatus(normalizeStatus(latestApproval.getStatus()));
            vo.setReason(latestApproval.getReason());
            vo.setReviewComment(latestApproval.getReviewComment());
            vo.setCreateTime(latestApproval.getCreateTime());
            vo.setReviewTime(latestApproval.getReviewTime());
        } else {
            vo.setStatus(STATUS_NONE);
        }

        if (latestFaceFeature == null) {
            vo.setCanRegister(Boolean.TRUE);
            vo.setCanApply(Boolean.FALSE);
            vo.setMessage("当前账号尚未录入人脸，可直接进行人脸采集");
            return vo;
        }

        String status = vo.getStatus();
        if (STATUS_APPROVED.equals(status)) {
            vo.setCanRegister(Boolean.TRUE);
            vo.setCanApply(Boolean.FALSE);
            vo.setMessage("管理员已通过本次人脸重录申请，可继续进行人脸采集");
            return vo;
        }

        if (STATUS_PENDING.equals(status)) {
            vo.setCanRegister(Boolean.FALSE);
            vo.setCanApply(Boolean.FALSE);
            vo.setMessage("已提交人脸重录申请，请等待管理员审批");
            return vo;
        }

        if (STATUS_REJECTED.equals(status)) {
            vo.setCanRegister(Boolean.FALSE);
            vo.setCanApply(Boolean.TRUE);
            vo.setMessage("最近一次人脸重录申请未通过，可修改说明后重新提交");
            return vo;
        }

        vo.setCanRegister(Boolean.FALSE);
        vo.setCanApply(Boolean.TRUE);
        vo.setMessage("当前账号已录入人脸，如需重新采集，请先提交申请并等待管理员审批");
        return vo;
    }

    @Override
    @Transactional
    public FaceRegisterStatusVO apply(Long userId, FaceRegisterApplyDTO dto) {
        Long safeUserId = requireExistingUser(userId);
        String reason = requireReason(dto == null ? null : dto.getReason());
        FaceFeature latestFaceFeature = faceFeatureMapper.selectLatestByUserId(safeUserId);
        if (latestFaceFeature == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前账号尚未录入人脸，无需提交重录申请");
        }

        FaceRegisterApproval latestApproval = selectLatestApproval(safeUserId);
        if (latestApproval != null) {
            String latestStatus = normalizeStatus(latestApproval.getStatus());
            if (STATUS_PENDING.equals(latestStatus)) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前已存在待审批的人脸重录申请，请勿重复提交");
            }
            if (STATUS_APPROVED.equals(latestStatus)) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前已存在审批通过的人脸重录申请，请直接进行人脸采集");
            }
        }

        FaceRegisterApproval approval = new FaceRegisterApproval();
        approval.setUserId(safeUserId);
        approval.setReason(reason);
        approval.setStatus(STATUS_PENDING);
        faceRegisterApprovalMapper.insert(approval);

        User user = userMapper.selectById(safeUserId);
        operationLogService.save(safeUserId, "FACE_REGISTER_APPLY", resolveUserDisplayName(user, safeUserId) + "提交人脸重录申请");
        return getStatus(safeUserId);
    }

    @Override
    public PageResult<FaceRegisterApprovalVO> list(FaceRegisterApprovalQueryDTO dto) {
        FaceRegisterApprovalQueryDTO safe = validateListQuery(dto);
        LambdaQueryWrapper<FaceRegisterApproval> totalQuery = buildListQuery(safe);
        long total = faceRegisterApprovalMapper.selectCount(totalQuery).longValue();
        if (total <= 0L) {
            return new PageResult<FaceRegisterApprovalVO>(0L, Collections.<FaceRegisterApprovalVO>emptyList());
        }

        int offset = (safe.getPageNum().intValue() - 1) * safe.getPageSize().intValue();
        LambdaQueryWrapper<FaceRegisterApproval> pageQuery = buildListQuery(safe)
                .orderByDesc(FaceRegisterApproval::getCreateTime, FaceRegisterApproval::getId)
                .last("LIMIT " + safe.getPageSize().intValue() + " OFFSET " + offset);
        List<FaceRegisterApprovalVO> records = faceRegisterApprovalMapper.selectList(pageQuery).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return new PageResult<FaceRegisterApprovalVO>(Long.valueOf(total), records);
    }

    @Override
    @Transactional
    public FaceRegisterApprovalVO review(Long reviewUserId, FaceRegisterApprovalReviewDTO dto) {
        Long safeReviewUserId = requireExistingUser(reviewUserId);
        FaceRegisterApprovalReviewDTO safe = validateReview(dto);
        FaceRegisterApproval approval = faceRegisterApprovalMapper.selectById(safe.getId());
        if (approval == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "人脸重录申请不存在");
        }
        if (!STATUS_PENDING.equals(normalizeStatus(approval.getStatus()))) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前申请已处理，请刷新后重试");
        }

        approval.setStatus(safe.getStatus());
        approval.setReviewUserId(safeReviewUserId);
        approval.setReviewComment(normalizeText(safe.getReviewComment()));
        approval.setReviewTime(LocalDateTime.now());
        faceRegisterApprovalMapper.updateById(approval);

        User applicant = userMapper.selectById(approval.getUserId());
        String logType = STATUS_APPROVED.equals(approval.getStatus()) ? "FACE_REGISTER_APPROVE" : "FACE_REGISTER_REJECT";
        String actionText = STATUS_APPROVED.equals(approval.getStatus()) ? "审批通过" : "审批驳回";
        operationLogService.save(safeReviewUserId, logType, resolveUserDisplayName(applicant, approval.getUserId()) + "的人脸重录申请" + actionText);
        return toVO(approval);
    }

    @Override
    public void requireApproved(Long userId) {
        Long safeUserId = requireExistingUser(userId);
        FaceRegisterApproval latestApproval = selectLatestApproval(safeUserId);
        String status = latestApproval == null ? STATUS_NONE : normalizeStatus(latestApproval.getStatus());
        if (STATUS_APPROVED.equals(status)) {
            return;
        }
        if (STATUS_PENDING.equals(status)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "已提交人脸重录申请，请等待管理员审批通过后再采集");
        }
        if (STATUS_REJECTED.equals(status)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "最近一次人脸重录申请未通过，请重新提交申请");
        }
        throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "当前账号已录入人脸，如需重新采集，请先提交申请并等待管理员审批");
    }

    @Override
    @Transactional
    public void consumeApproved(Long userId) {
        Long safeUserId = requireExistingUser(userId);
        FaceRegisterApproval approval = selectLatestApprovalByStatus(safeUserId, STATUS_APPROVED);
        if (approval == null) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "当前账号未找到有效的人脸重录审批记录");
        }
        approval.setStatus(STATUS_USED);
        faceRegisterApprovalMapper.updateById(approval);
    }

    private FaceRegisterApprovalVO toVO(FaceRegisterApproval approval) {
        FaceRegisterApprovalVO vo = new FaceRegisterApprovalVO();
        vo.setId(approval.getId());
        vo.setUserId(approval.getUserId());
        vo.setReason(approval.getReason());
        vo.setStatus(normalizeStatus(approval.getStatus()));
        vo.setReviewUserId(approval.getReviewUserId());
        vo.setReviewComment(approval.getReviewComment());
        vo.setCreateTime(approval.getCreateTime());
        vo.setReviewTime(approval.getReviewTime());

        User applicant = approval.getUserId() == null ? null : userMapper.selectById(approval.getUserId());
        if (applicant != null) {
            vo.setUserName(resolveUserDisplayName(applicant, approval.getUserId()));
            Department department = applicant.getDeptId() == null ? null : departmentMapper.selectById(applicant.getDeptId());
            vo.setDepartmentName(department == null ? "-" : department.getName());
        } else {
            vo.setUserName(approval.getUserId() == null ? "-" : "用户#" + approval.getUserId());
            vo.setDepartmentName("-");
        }

        User reviewUser = approval.getReviewUserId() == null ? null : userMapper.selectById(approval.getReviewUserId());
        vo.setReviewUserName(resolveUserDisplayName(reviewUser, approval.getReviewUserId()));
        return vo;
    }

    private LambdaQueryWrapper<FaceRegisterApproval> buildListQuery(FaceRegisterApprovalQueryDTO dto) {
        LambdaQueryWrapper<FaceRegisterApproval> queryWrapper = Wrappers.lambdaQuery(FaceRegisterApproval.class);
        if (StringUtils.hasText(dto.getStatus())) {
            queryWrapper.eq(FaceRegisterApproval::getStatus, dto.getStatus());
        }
        return queryWrapper;
    }

    private FaceRegisterApprovalQueryDTO validateListQuery(FaceRegisterApprovalQueryDTO dto) {
        FaceRegisterApprovalQueryDTO safe = dto == null ? new FaceRegisterApprovalQueryDTO() : dto;
        safe.setPageNum(resolvePageNum(safe.getPageNum()));
        safe.setPageSize(resolvePageSize(safe.getPageSize()));
        safe.setStatus(resolveStatusFilter(safe.getStatus()));
        return safe;
    }

    private FaceRegisterApprovalReviewDTO validateReview(FaceRegisterApprovalReviewDTO dto) {
        FaceRegisterApprovalReviewDTO safe = dto == null ? new FaceRegisterApprovalReviewDTO() : dto;
        if (safe.getId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "申请编号不能为空");
        }
        String status = normalizeStatus(safe.getStatus());
        if (!STATUS_APPROVED.equals(status) && !STATUS_REJECTED.equals(status)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "审批结果不合法");
        }
        safe.setStatus(status);
        safe.setReviewComment(limitText(normalizeText(safe.getReviewComment()), 255));
        return safe;
    }

    private Long requireExistingUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户编号不能为空");
        }
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户不存在");
        }
        return userId;
    }

    private String requireReason(String reason) {
        String normalized = normalizeText(reason);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "申请说明不能为空");
        }
        return limitText(normalized, 255);
    }

    private Integer resolvePageNum(Integer pageNum) {
        if (pageNum == null || pageNum.intValue() < 1) {
            return Integer.valueOf(DEFAULT_PAGE_NUM);
        }
        return pageNum;
    }

    private Integer resolvePageSize(Integer pageSize) {
        if (pageSize == null || pageSize.intValue() < 1) {
            return Integer.valueOf(DEFAULT_PAGE_SIZE);
        }
        return Integer.valueOf(Math.min(pageSize.intValue(), MAX_PAGE_SIZE));
    }

    private String resolveStatusFilter(String status) {
        String normalized = normalizeText(status);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        String upperStatus = normalized.toUpperCase(Locale.ROOT);
        if (!STATUS_PENDING.equals(upperStatus)
                && !STATUS_APPROVED.equals(upperStatus)
                && !STATUS_REJECTED.equals(upperStatus)
                && !STATUS_USED.equals(upperStatus)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "申请状态不合法");
        }
        return upperStatus;
    }

    private FaceRegisterApproval selectLatestApproval(Long userId) {
        return faceRegisterApprovalMapper.selectOne(Wrappers.<FaceRegisterApproval>lambdaQuery()
                .eq(FaceRegisterApproval::getUserId, userId)
                .orderByDesc(FaceRegisterApproval::getCreateTime, FaceRegisterApproval::getId)
                .last("LIMIT 1"));
    }

    private FaceRegisterApproval selectLatestApprovalByStatus(Long userId, String status) {
        return faceRegisterApprovalMapper.selectOne(Wrappers.<FaceRegisterApproval>lambdaQuery()
                .eq(FaceRegisterApproval::getUserId, userId)
                .eq(FaceRegisterApproval::getStatus, status)
                .orderByDesc(FaceRegisterApproval::getCreateTime, FaceRegisterApproval::getId)
                .last("LIMIT 1"));
    }

    private String normalizeStatus(String status) {
        String normalized = normalizeText(status);
        return StringUtils.hasText(normalized) ? normalized.toUpperCase(Locale.ROOT) : STATUS_NONE;
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private String limitText(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String resolveUserDisplayName(User user, Long userId) {
        if (user == null) {
            return userId == null ? "-" : "用户#" + userId;
        }
        if (StringUtils.hasText(user.getRealName())) {
            return user.getRealName().trim();
        }
        if (StringUtils.hasText(user.getUsername())) {
            return user.getUsername().trim();
        }
        return user.getId() == null ? "-" : "用户#" + user.getId();
    }
}
