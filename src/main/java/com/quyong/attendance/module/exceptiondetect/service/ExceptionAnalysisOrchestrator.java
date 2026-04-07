package com.quyong.attendance.module.exceptiondetect.service;

import com.quyong.attendance.module.exceptiondetect.dto.ComplexCheckDTO;
import com.quyong.attendance.module.exceptiondetect.dto.RuleCheckDTO;
import com.quyong.attendance.module.exceptiondetect.vo.ExceptionDecisionVO;

public interface ExceptionAnalysisOrchestrator {

    ExceptionDecisionVO ruleCheck(RuleCheckDTO dto);

    ExceptionDecisionVO complexCheck(ComplexCheckDTO dto);
}
