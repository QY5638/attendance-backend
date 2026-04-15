package com.quyong.attendance.module.warning.service;

import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.module.warning.dto.RiskLevelQueryDTO;
import com.quyong.attendance.module.warning.dto.RiskLevelUpdateDTO;
import com.quyong.attendance.module.warning.dto.WarningQueryDTO;
import com.quyong.attendance.module.warning.dto.WarningReplyDTO;
import com.quyong.attendance.module.warning.dto.WarningRequestExplanationDTO;
import com.quyong.attendance.module.warning.dto.WarningReevaluateDTO;
import com.quyong.attendance.module.warning.vo.RiskLevelConfigVO;
import com.quyong.attendance.module.warning.vo.WarningAdviceVO;
import com.quyong.attendance.module.warning.vo.WarningDashboardVO;
import com.quyong.attendance.module.warning.vo.WarningInteractionVO;
import com.quyong.attendance.module.warning.vo.WarningVO;

import java.util.List;

public interface WarningService {

    PageResult<WarningVO> list(WarningQueryDTO queryDTO);

    WarningDashboardVO dashboard();

    WarningAdviceVO getAdvice(Long id);

    WarningVO reEvaluate(WarningReevaluateDTO dto);

    List<WarningInteractionVO> listInteractions(Long warningId);

    void requestExplanation(Long warningId, WarningRequestExplanationDTO dto);

    void reply(Long warningId, WarningReplyDTO dto);

    void syncWarningByExceptionId(Long exceptionId);

    void markProcessedByExceptionId(Long exceptionId);

    void notifyReviewResult(Long exceptionId, String reviewResult, String reviewComment);

    int runAbsenceCheck();

    int runMissingCheckoutCheck();

    int runReminderCheck();

    PageResult<RiskLevelConfigVO> listRiskLevels(RiskLevelQueryDTO queryDTO);

    void updateRiskLevel(RiskLevelUpdateDTO dto);
}
