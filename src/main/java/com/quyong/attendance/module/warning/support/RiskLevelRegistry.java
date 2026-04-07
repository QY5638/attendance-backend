package com.quyong.attendance.module.warning.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.warning.dto.RiskLevelQueryDTO;
import com.quyong.attendance.module.warning.dto.RiskLevelUpdateDTO;
import com.quyong.attendance.module.warning.entity.RiskLevel;
import com.quyong.attendance.module.warning.mapper.RiskLevelMapper;
import com.quyong.attendance.module.warning.vo.RiskLevelConfigVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.time.LocalDateTime;

@Component
public class RiskLevelRegistry {

    private final RiskLevelMapper riskLevelMapper;

    public RiskLevelRegistry(RiskLevelMapper riskLevelMapper) {
        this.riskLevelMapper = riskLevelMapper;
    }

    public PageResult<RiskLevelConfigVO> list(RiskLevelQueryDTO queryDTO) {
        List<RiskLevel> entities = riskLevelMapper.selectList(Wrappers.<RiskLevel>lambdaQuery()
                .eq(queryDTO.getStatus() != null, RiskLevel::getStatus, queryDTO.getStatus())
                .orderByAsc(RiskLevel::getId));
        long total = entities.size();
        int fromIndex = (queryDTO.getPageNum().intValue() - 1) * queryDTO.getPageSize().intValue();
        if (fromIndex >= entities.size()) {
            return new PageResult<RiskLevelConfigVO>(Long.valueOf(total), Collections.<RiskLevelConfigVO>emptyList());
        }
        int toIndex = Math.min(fromIndex + queryDTO.getPageSize().intValue(), entities.size());
        List<RiskLevelConfigVO> records = new ArrayList<RiskLevelConfigVO>();
        for (RiskLevel entity : entities.subList(fromIndex, toIndex)) {
            records.add(toVO(entity));
        }
        return new PageResult<RiskLevelConfigVO>(Long.valueOf(total), records);
    }

    public void update(RiskLevelUpdateDTO updateDTO) {
        RiskLevel entity = riskLevelMapper.selectOne(Wrappers.<RiskLevel>lambdaQuery()
                .eq(RiskLevel::getCode, updateDTO.getCode())
                .last("LIMIT 1"));
        if (entity == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "风险等级配置不存在");
        }
        entity.setName(updateDTO.getName());
        entity.setDescription(updateDTO.getDescription());
        entity.setStatus(updateDTO.getStatus());
        entity.setUpdateTime(LocalDateTime.now());
        riskLevelMapper.updateById(entity);
    }

    public RiskLevelConfigVO get(String code) {
        RiskLevel entity = riskLevelMapper.selectOne(Wrappers.<RiskLevel>lambdaQuery()
                .eq(RiskLevel::getCode, code)
                .last("LIMIT 1"));
        return entity == null ? null : toVO(entity);
    }

    private RiskLevelConfigVO toVO(RiskLevel source) {
        RiskLevelConfigVO vo = new RiskLevelConfigVO();
        vo.setCode(source.getCode());
        vo.setName(source.getName());
        vo.setDescription(source.getDescription());
        vo.setStatus(source.getStatus());
        return vo;
    }
}
