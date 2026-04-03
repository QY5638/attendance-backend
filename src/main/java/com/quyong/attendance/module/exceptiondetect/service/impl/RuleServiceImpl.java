package com.quyong.attendance.module.exceptiondetect.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.exceptiondetect.dto.RuleQueryDTO;
import com.quyong.attendance.module.exceptiondetect.dto.RuleSaveDTO;
import com.quyong.attendance.module.exceptiondetect.dto.RuleStatusDTO;
import com.quyong.attendance.module.exceptiondetect.entity.Rule;
import com.quyong.attendance.module.exceptiondetect.mapper.RuleMapper;
import com.quyong.attendance.module.exceptiondetect.service.RuleService;
import com.quyong.attendance.module.exceptiondetect.support.RuleValidationSupport;
import com.quyong.attendance.module.exceptiondetect.vo.RuleVO;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class RuleServiceImpl implements RuleService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final RuleMapper ruleMapper;
    private final RuleValidationSupport ruleValidationSupport;

    public RuleServiceImpl(RuleMapper ruleMapper, RuleValidationSupport ruleValidationSupport) {
        this.ruleMapper = ruleMapper;
        this.ruleValidationSupport = ruleValidationSupport;
    }

    @Override
    public PageResult<RuleVO> list(RuleQueryDTO queryDTO) {
        RuleQueryDTO validatedDTO = ruleValidationSupport.validateQuery(queryDTO);
        int offset = (validatedDTO.getPageNum().intValue() - 1) * validatedDTO.getPageSize().intValue();
        long total = ruleMapper.countByQuery(validatedDTO.getKeyword(), validatedDTO.getStatus());
        List<Rule> entities = ruleMapper.selectPageByQuery(
                validatedDTO.getKeyword(),
                validatedDTO.getStatus(),
                validatedDTO.getPageSize().intValue(),
                offset
        );
        List<RuleVO> records = new ArrayList<RuleVO>();
        for (Rule entity : entities) {
            records.add(toVO(entity));
        }
        return new PageResult<RuleVO>(Long.valueOf(total), records);
    }

    @Override
    public void add(RuleSaveDTO saveDTO) {
        RuleSaveDTO validatedDTO = ruleValidationSupport.validateSave(saveDTO, false);
        Rule rule = new Rule();
        fillRule(rule, validatedDTO);
        rule.setStatus(1);
        ruleMapper.insert(rule);
    }

    @Override
    public void update(RuleSaveDTO saveDTO) {
        RuleSaveDTO validatedDTO = ruleValidationSupport.validateSave(saveDTO, true);
        Rule rule = ruleMapper.selectById(validatedDTO.getId());
        if (rule == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "规则不存在");
        }
        fillRule(rule, validatedDTO);
        ruleMapper.updateById(rule);
    }

    @Override
    public void updateStatus(RuleStatusDTO statusDTO) {
        RuleStatusDTO validatedDTO = ruleValidationSupport.validateStatus(statusDTO);
        Rule rule = ruleMapper.selectById(validatedDTO.getId());
        if (rule == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "规则不存在");
        }
        rule.setStatus(validatedDTO.getStatus());
        ruleMapper.updateById(rule);
    }

    @Override
    public Rule getEnabledRule() {
        Rule rule = ruleMapper.selectOne(Wrappers.<Rule>lambdaQuery()
                .eq(Rule::getStatus, 1)
                .orderByAsc(Rule::getId)
                .last("LIMIT 1"));
        if (rule == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "未找到启用中的考勤规则");
        }
        return rule;
    }

    private void fillRule(Rule rule, RuleSaveDTO saveDTO) {
        rule.setName(saveDTO.getName());
        rule.setStartTime(ruleValidationSupport.parseTime(saveDTO.getStartTime(), "上班时间"));
        rule.setEndTime(ruleValidationSupport.parseTime(saveDTO.getEndTime(), "下班时间"));
        rule.setLateThreshold(saveDTO.getLateThreshold());
        rule.setEarlyThreshold(saveDTO.getEarlyThreshold());
        rule.setRepeatLimit(saveDTO.getRepeatLimit());
    }

    private RuleVO toVO(Rule entity) {
        RuleVO vo = new RuleVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setStartTime(entity.getStartTime() == null ? null : entity.getStartTime().format(TIME_FORMATTER));
        vo.setEndTime(entity.getEndTime() == null ? null : entity.getEndTime().format(TIME_FORMATTER));
        vo.setLateThreshold(entity.getLateThreshold());
        vo.setEarlyThreshold(entity.getEarlyThreshold());
        vo.setRepeatLimit(entity.getRepeatLimit());
        vo.setStatus(entity.getStatus());
        return vo;
    }
}
