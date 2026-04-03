package com.quyong.attendance.module.model.trace.service;

import com.quyong.attendance.module.model.trace.vo.DecisionTraceVO;

import java.math.BigDecimal;
import java.util.List;

public interface DecisionTraceService {

    void save(String businessType,
              Long businessId,
              String ruleResult,
              String modelResult,
              String finalDecision,
              BigDecimal confidenceScore,
              String decisionReason);

    List<DecisionTraceVO> list(String businessType, Long businessId);
}
