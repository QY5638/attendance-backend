package com.quyong.attendance.module.model.trace.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.quyong.attendance.module.model.trace.entity.DecisionTrace;
import com.quyong.attendance.module.model.trace.mapper.DecisionTraceMapper;
import com.quyong.attendance.module.model.trace.service.DecisionTraceService;
import com.quyong.attendance.module.model.trace.vo.DecisionTraceVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DecisionTraceServiceImpl implements DecisionTraceService {

    private final DecisionTraceMapper decisionTraceMapper;

    public DecisionTraceServiceImpl(DecisionTraceMapper decisionTraceMapper) {
        this.decisionTraceMapper = decisionTraceMapper;
    }

    @Override
    public void save(String businessType,
                     Long businessId,
                     String ruleResult,
                     String modelResult,
                     String finalDecision,
                     BigDecimal confidenceScore,
                     String decisionReason) {
        DecisionTrace decisionTrace = new DecisionTrace();
        decisionTrace.setBusinessType(businessType);
        decisionTrace.setBusinessId(businessId);
        decisionTrace.setRuleResult(ruleResult);
        decisionTrace.setModelResult(modelResult);
        decisionTrace.setFinalDecision(finalDecision);
        decisionTrace.setConfidenceScore(confidenceScore);
        decisionTrace.setDecisionReason(decisionReason);
        decisionTraceMapper.insert(decisionTrace);
    }

    @Override
    public List<DecisionTraceVO> list(String businessType, Long businessId) {
        return decisionTraceMapper.selectList(Wrappers.<DecisionTrace>lambdaQuery()
                        .eq(DecisionTrace::getBusinessType, businessType)
                        .eq(DecisionTrace::getBusinessId, businessId)
                        .orderByAsc(DecisionTrace::getId))
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    private DecisionTraceVO toVO(DecisionTrace entity) {
        DecisionTraceVO vo = new DecisionTraceVO();
        vo.setId(entity.getId());
        vo.setBusinessType(entity.getBusinessType());
        vo.setBusinessId(entity.getBusinessId());
        vo.setRuleResult(entity.getRuleResult());
        vo.setModelResult(entity.getModelResult());
        vo.setFinalDecision(entity.getFinalDecision());
        vo.setConfidenceScore(entity.getConfidenceScore());
        vo.setDecisionReason(entity.getDecisionReason());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
