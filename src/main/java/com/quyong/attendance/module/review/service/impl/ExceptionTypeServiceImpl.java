package com.quyong.attendance.module.review.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.review.dto.ExceptionTypeQueryDTO;
import com.quyong.attendance.module.review.dto.ExceptionTypeUpdateDTO;
import com.quyong.attendance.module.review.entity.ExceptionType;
import com.quyong.attendance.module.review.mapper.ExceptionTypeMapper;
import com.quyong.attendance.module.review.service.ExceptionTypeService;
import com.quyong.attendance.module.review.support.ReviewValidationSupport;
import com.quyong.attendance.module.review.vo.ExceptionTypeVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ExceptionTypeServiceImpl implements ExceptionTypeService {

    private final ExceptionTypeMapper exceptionTypeMapper;
    private final ReviewValidationSupport reviewValidationSupport;

    public ExceptionTypeServiceImpl(ExceptionTypeMapper exceptionTypeMapper,
                                    ReviewValidationSupport reviewValidationSupport) {
        this.exceptionTypeMapper = exceptionTypeMapper;
        this.reviewValidationSupport = reviewValidationSupport;
    }

    @Override
    public PageResult<ExceptionTypeVO> list(ExceptionTypeQueryDTO dto) {
        ExceptionTypeQueryDTO validatedDTO = reviewValidationSupport.validateExceptionTypeQuery(dto);
        List<ExceptionType> entities = exceptionTypeMapper.selectList(Wrappers.<ExceptionType>lambdaQuery()
                .eq(validatedDTO.getStatus() != null, ExceptionType::getStatus, validatedDTO.getStatus())
                .orderByDesc(ExceptionType::getUpdateTime)
                .orderByDesc(ExceptionType::getId));
        long total = entities.size();
        int fromIndex = (validatedDTO.getPageNum().intValue() - 1) * validatedDTO.getPageSize().intValue();
        if (fromIndex >= entities.size()) {
            return new PageResult<ExceptionTypeVO>(Long.valueOf(total), Collections.<ExceptionTypeVO>emptyList());
        }
        int toIndex = Math.min(fromIndex + validatedDTO.getPageSize().intValue(), entities.size());
        List<ExceptionTypeVO> records = new ArrayList<ExceptionTypeVO>();
        for (ExceptionType entity : entities.subList(fromIndex, toIndex)) {
            records.add(toVO(entity));
        }
        return new PageResult<ExceptionTypeVO>(Long.valueOf(total), records);
    }

    @Override
    public void update(ExceptionTypeUpdateDTO dto) {
        ExceptionTypeUpdateDTO validatedDTO = reviewValidationSupport.validateExceptionTypeUpdate(dto);
        ExceptionType entity = exceptionTypeMapper.selectOne(Wrappers.<ExceptionType>lambdaQuery()
                .eq(ExceptionType::getCode, validatedDTO.getCode())
                .last("LIMIT 1"));
        if (entity == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "异常类型配置不存在");
        }
        entity.setName(validatedDTO.getName());
        entity.setDescription(validatedDTO.getDescription());
        entity.setStatus(validatedDTO.getStatus());
        exceptionTypeMapper.updateById(entity);
    }

    private ExceptionTypeVO toVO(ExceptionType entity) {
        ExceptionTypeVO vo = new ExceptionTypeVO();
        vo.setCode(entity.getCode());
        vo.setName(entity.getName());
        vo.setDescription(entity.getDescription());
        vo.setStatus(entity.getStatus());
        return vo;
    }
}
