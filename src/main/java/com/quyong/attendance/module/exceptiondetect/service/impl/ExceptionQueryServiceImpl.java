package com.quyong.attendance.module.exceptiondetect.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.exceptiondetect.dto.ExceptionQueryDTO;
import com.quyong.attendance.module.exceptiondetect.entity.AttendanceException;
import com.quyong.attendance.module.exceptiondetect.entity.ExceptionAnalysis;
import com.quyong.attendance.module.exceptiondetect.mapper.AttendanceExceptionMapper;
import com.quyong.attendance.module.exceptiondetect.mapper.ExceptionAnalysisMapper;
import com.quyong.attendance.module.exceptiondetect.service.ExceptionQueryService;
import com.quyong.attendance.module.exceptiondetect.support.ExceptionValidationSupport;
import com.quyong.attendance.module.exceptiondetect.vo.AttendanceExceptionVO;
import com.quyong.attendance.module.exceptiondetect.vo.ExceptionAnalysisBriefVO;
import com.quyong.attendance.module.user.entity.User;
import com.quyong.attendance.module.user.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ExceptionQueryServiceImpl implements ExceptionQueryService {

    private final AttendanceExceptionMapper attendanceExceptionMapper;
    private final ExceptionAnalysisMapper exceptionAnalysisMapper;
    private final ExceptionValidationSupport exceptionValidationSupport;
    private final UserMapper userMapper;

    public ExceptionQueryServiceImpl(AttendanceExceptionMapper attendanceExceptionMapper,
                                     ExceptionAnalysisMapper exceptionAnalysisMapper,
                                     ExceptionValidationSupport exceptionValidationSupport,
                                     UserMapper userMapper) {
        this.attendanceExceptionMapper = attendanceExceptionMapper;
        this.exceptionAnalysisMapper = exceptionAnalysisMapper;
        this.exceptionValidationSupport = exceptionValidationSupport;
        this.userMapper = userMapper;
    }

    @Override
    public PageResult<AttendanceExceptionVO> list(ExceptionQueryDTO queryDTO) {
        ExceptionQueryDTO validatedDTO = exceptionValidationSupport.validateQuery(queryDTO);
        List<AttendanceException> entities = attendanceExceptionMapper.selectList(Wrappers.<AttendanceException>lambdaQuery()
                .eq(validatedDTO.getUserId() != null, AttendanceException::getUserId, validatedDTO.getUserId())
                .eq(validatedDTO.getType() != null, AttendanceException::getType, validatedDTO.getType())
                .eq(validatedDTO.getRiskLevel() != null, AttendanceException::getRiskLevel, validatedDTO.getRiskLevel())
                .eq(validatedDTO.getProcessStatus() != null, AttendanceException::getProcessStatus, validatedDTO.getProcessStatus())
                .orderByDesc(AttendanceException::getCreateTime)
                .orderByDesc(AttendanceException::getId));
        long total = entities.size();
        int fromIndex = (validatedDTO.getPageNum().intValue() - 1) * validatedDTO.getPageSize().intValue();
        if (fromIndex >= entities.size()) {
            return new PageResult<AttendanceExceptionVO>(Long.valueOf(total), Collections.<AttendanceExceptionVO>emptyList());
        }
        int toIndex = Math.min(fromIndex + validatedDTO.getPageSize().intValue(), entities.size());
        List<AttendanceExceptionVO> records = new ArrayList<AttendanceExceptionVO>();
        for (AttendanceException entity : entities.subList(fromIndex, toIndex)) {
            records.add(toVO(entity));
        }
        return new PageResult<AttendanceExceptionVO>(Long.valueOf(total), records);
    }

    @Override
    public AttendanceExceptionVO getById(Long id) {
        AttendanceException entity = attendanceExceptionMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "异常记录不存在");
        }
        return toVO(entity);
    }

    @Override
    public ExceptionAnalysisBriefVO getAnalysisBrief(Long id) {
        ExceptionAnalysis analysis = exceptionAnalysisMapper.selectOne(Wrappers.<ExceptionAnalysis>lambdaQuery()
                .eq(ExceptionAnalysis::getExceptionId, id)
                .orderByDesc(ExceptionAnalysis::getCreateTime)
                .last("LIMIT 1"));
        if (analysis == null) {
            return null;
        }
        ExceptionAnalysisBriefVO vo = new ExceptionAnalysisBriefVO();
        vo.setModelConclusion(analysis.getModelConclusion());
        vo.setReasonSummary(analysis.getReasonSummary());
        vo.setActionSuggestion(analysis.getActionSuggestion());
        vo.setSimilarCaseSummary(analysis.getSimilarCaseSummary());
        vo.setPromptVersion(analysis.getPromptVersion());
        vo.setConfidenceScore(analysis.getConfidenceScore());
        return vo;
    }

    private AttendanceExceptionVO toVO(AttendanceException entity) {
        AttendanceExceptionVO vo = new AttendanceExceptionVO();
        vo.setId(entity.getId());
        vo.setRecordId(entity.getRecordId());
        vo.setUserId(entity.getUserId());
        if (entity.getUserId() != null) {
            User user = userMapper.selectById(entity.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setRealName(user.getRealName());
            }
        }
        vo.setType(entity.getType());
        vo.setRiskLevel(entity.getRiskLevel());
        vo.setSourceType(entity.getSourceType());
        vo.setDescription(entity.getDescription());
        vo.setProcessStatus(entity.getProcessStatus());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
